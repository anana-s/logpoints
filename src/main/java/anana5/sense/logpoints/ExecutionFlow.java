package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Rain;
import anana5.util.Promise;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExecutionFlow extends Rain<Stmt> {

    static Logger logger = LoggerFactory.getLogger(ExecutionFlow.class);

    private static boolean skip(SootMethod m) {
        return m.isPhantom() 
        || !m.isConcrete()
        || m.isJavaLibraryMethod()
        || m.getDeclaringClass().isLibraryClass();
    }

    public ExecutionFlow(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        super(build(callgraph, entrypoints));
    }

    private static Rain<Stmt> build(CallGraph cg, Collection<SootMethod> es) {
        Builder builder = new Builder(cg);
        return unfold(builder, builder.roots(es)).map(Stmt.class::cast);
    }

    static class Builder implements Function<Builder.Reader, Rain<Unit, Builder.Reader>> {
        CallGraph cg;

        Builder(CallGraph cg) {
            this.cg = cg;
        }

        @Override
        public Rain<Unit, Reader> apply(Reader reader) {
            return reader.succs();
        }

        Reader roots(Collection<SootMethod> methods) {
            MultiReader readers = new MultiReader(methods.size());
            for (SootMethod method : methods) {
                if (!skip(method)) {
                    roots.add(method);
                    logger.debug("Building entry context {}.", method);
                    readers.add(new Context(null, method).root());
                }
            }
            return readers;
        }

        interface Reader {
            Rain<Unit, Reader> succs();
        }

        class MultiReader extends ArrayList<Reader> implements Reader {
            MultiReader() {
                super();
            } 
            MultiReader(int size) {
                super(size);
            } 
            MultiReader(Collection<? extends Reader> readers) {
                super(readers);
            } 
            @SuppressWarnings("unchecked")
            @Override
            public Rain<Unit, Reader> succs() {
                Collection<Rain<Unit, Reader>> rains = new ArrayList<>(size());
                for (Reader reader : this) {
                    rains.add(reader.succs());
                }
                return Rain.merge(rains.toArray(new Rain[rains.size()]));
            }
        }

        Set<SootMethod> roots = new HashSet<>();

        class Context {
            Map<Unit, Collection<Context>> seen;
            Map<Unit, Droplet<Unit, Reader>> visited;
            Unit invoker;
            ExceptionalUnitGraph cfg;
            UnitReader ret;
            Reader root;
    
            Context(Unit invoker, SootMethod method) {
                this(new HashMap<>(), invoker, method, null, null);
                this.ret = new UnitReader();
            }
    
            Context(Map<Unit, Collection<Context>> seen, Unit invoker, SootMethod method, UnitReader ret, Context prv) {
                this.seen = seen;
                this.visited = new HashMap<>();
                this.invoker = invoker;
                this.cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());
                this.ret = ret;
                this.root = null;
            }
    
            public Collection<Context> push(Unit unit, UnitReader ret) {
                return seen.computeIfAbsent(unit, $ -> {
                    Collection<Context> contexts = new ArrayList<>();
                    for (Edge edge : (Iterable<Edge>)() -> cg.edgesOutOf(unit)) {
                        SootMethod method = edge.tgt();
                        if (skip(method)) {
                            continue;
                        }
                        if (edge.isClinit()) {
                            if (roots.contains(method)) {
                                continue;
                            }
                            roots.add(method);
                            contexts.add(new Context(unit, method));
                        } else {
                            contexts.add(new Context(seen, unit, method, ret, this));
                        }
                        logger.debug("Building context of {} =>> {}.", edge.src(), unit);
                    }
                    // cg.edgesOutOf(unit).forEachRemaining(edge -> {
                    //     SootMethod method = edge.tgt();
                    //     if (skip(method)) {
                    //         return;
                    //     }
                    //     contexts.add(new Context(seen, unit, method, ret, this));
                    // });
                    return contexts;
                });
            }

            public Context pop() {
                seen.remove(invoker);
                return ret.ctx();
            }
    
            public Reader root() {
                if (root != null) {
                    return root;
                }
                return root = new UnitReader(cfg.getHeads());
            }
    
            class UnitReader implements Reader {
                Collection<Unit> units;
                UnitReader() {
                    this.units = new ArrayList<>();
                }

                UnitReader(Collection<Unit> unit) {
                    this.units = unit;
                }

                public Context ctx() {
                    return Context.this;
                }

                public Reader of(Unit unit) {
                    List<Unit> units = cfg.getSuccsOf(unit);
                    if (units.isEmpty()) {
                        pop();
                        return ret;
                    }

                    UnitReader ret = new UnitReader(units);

                    if (unit instanceof Stmt && ((Stmt)unit).containsInvokeExpr()) {
                        MultiReader readers = new MultiReader();
                        for (Context ctx : push(unit, ret)) {
                            readers.add(ctx.root());
                        }
                        if (readers.isEmpty()) {
                            return ret;
                        }
                        return readers;
                    }

                    return ret;
                }

                @Override
                public Rain<Unit, Reader> succs() {
                    Puddle<Unit, Reader> puddle = new Puddle<>();
                    for (Unit unit : units) {
                        puddle.add(visited.computeIfAbsent(unit, $ -> {
                            return new Droplet<>(unit, of(unit));
                        }));
                    }
                    return new Rain<>(new Promise<Puddle<Unit, Reader>>(() -> puddle));
                }
            }
        }
    }
}
