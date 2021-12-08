package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import anana5.sense.graph.Rain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExecutionFlow extends Rain<Unit> {

    private static boolean skip(SootMethod m) {
        return m.isPhantom() 
        || !m.isConcrete()
        || m.isJavaLibraryMethod()
        || m.getDeclaringClass().isLibraryClass();
    }

    public ExecutionFlow(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        super(build(callgraph, entrypoints));
    }

    static Rain<Unit> build(CallGraph cg, Collection<SootMethod> es) {
        Builder builder = new Builder(cg);
        return unfold(builder, builder.roots(es));
    }

    static class Builder implements Function<Builder.Reader, Droplets<Unit, Builder.Reader>> {
        CallGraph cg;

        Builder(CallGraph cg) {
            this.cg = cg;
        }

        @Override
        public Droplets<Unit, Reader> apply(Reader reader) {
            return reader.succs();
        }

        Reader roots(Collection<SootMethod> methods) {
            MultiReader readers = new MultiReader();
            for (SootMethod method : methods) {
                if (!skip(method)) {
                    readers.add(new Context(null, method).root());
                }
            }
            return readers;
        }

        interface Reader {
            Droplets<Unit, Reader> succs();
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
            @Override
            public Droplets<Unit, Reader> succs() {
                Droplets<Unit, Reader> droplets = new Droplets<>();
                for (Reader reader : this) {
                    droplets.addAll(reader.succs());
                }
                return droplets;
            }
        }

        class Context {
            Map<Unit, Collection<Context>> seen;
            Map<Unit, Drop<Unit, Reader>> visited;
            Unit invoker;
            ExceptionalUnitGraph cfg;
            Reader ret;
            Context prv;
            Reader root;
    
            Context(Unit invoker, SootMethod method) {
                this(new HashMap<>(), invoker, method, null, null);
                this.ret = new UnitReader();
            }
    
            Context(Map<Unit, Collection<Context>> seen, Unit invoker, SootMethod method, Reader ret, Context prv) {
                this.seen = seen;
                this.visited = new HashMap<>();
                this.invoker = invoker;
                this.cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());
                this.ret = ret;
                this.prv = prv;
                this.root = null;
            }
    
            public Collection<Context> push(Unit unit, Reader ret) {
                return seen.computeIfAbsent(unit, $ -> {
                    Collection<Context> contexts = new ArrayList<>();
                    cg.edgesOutOf(unit).forEachRemaining(edge -> {
                        SootMethod method = edge.tgt();
                        if (skip(method)) {
                            return;
                        }
                        contexts.add(new Context(seen, unit, method, ret, this));
                    });
                    return contexts;
                });
            }

            public Context pop() {
                seen.remove(invoker);
                return prv;
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

                public Reader of(Unit unit) {
                    List<Unit> units = cfg.getSuccsOf(unit);
                    if (units.isEmpty()) {
                        pop();
                        return ret;
                    }

                    Reader ret = new UnitReader(units);

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
                public Droplets<Unit, Reader> succs() {
                    Droplets<Unit, Reader> droplets = new Droplets<>();
                    for (Unit unit : units) {
                        droplets.add(visited.computeIfAbsent(unit, $ -> {
                            return new Drop<>(unit, of(unit));
                        }));
                    }
                    return droplets;
                }
            }
        }
    }
}
