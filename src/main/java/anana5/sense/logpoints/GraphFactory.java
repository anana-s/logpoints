package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Box;
import anana5.graph.DirectedEdge;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.graph.rainfall.Droplet;
import anana5.graph.rainfall.Rain;
import anana5.util.LList;
import anana5.util.ListF;
import anana5.util.Promise;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class GraphFactory {

    static Logger logger = LoggerFactory.getLogger(GraphFactory.class);

    CallGraph cg;
    List<Pattern> tags = new ArrayList<>();

    public GraphFactory(CallGraph cg) {
        this.cg = cg;
    }

    public GraphFactory tag(Pattern pattern) {
        tags.add(pattern);
        return this;
    }

    public GraphFactory tag(int flags, String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern, flags));
        }
        return this;
    }

    public GraphFactory tag(String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern));
        }
        return this;
    }

    public Rain<Stmt> build(Iterable<SootMethod> entrypoints) {
        return build(new LList<>(entrypoints), new Rain<>());
    }

    private Rain<Stmt> build(LList<SootMethod> methods, Rain<Stmt> rets) {

        methods = methods.filter(m -> {
            if (!m.isPhantom() && m.isConcrete() && !m.isJavaLibraryMethod() && !m.getDeclaringClass().isLibraryClass()) {
                return Promise.just(true);
            } else {
                logger.trace("{} skipped", m);
                return Promise.just(false);
            }
        });

        var rain = Rain.merge(methods.map(m -> Promise.just(build(m))));

        // create new context
        rain = rain.fold(droplets -> {
            return Rain.bind(droplets.isEmpty().map(condition -> {
                if (condition) {
                    return rets;
                } else {
                    return new Rain<>(droplets);
                }
            }));
        });

        return rain;
    }

    Map<SootMethod, Rain<Stmt>> visited = new HashMap<>();
    private Rain<Stmt> build(SootMethod method) {
        return visited.compute(method, (m, rain) -> {
            if (rain != null) {
                logger.trace("{} loaded from cache.", method);
                // map to new boxes
                return rain.map(box -> new Box<>(box.value()));
            }
            var builder = new CFGFactory(method);
            var out = Promise.effect(() -> {
                logger.trace("building {}", method);
            }).then(($) -> Promise.just(builder.build()));
            return Rain.bind(out);
        });
    }

    class CFGFactory {
        SootMethod method;
        Map<Stmt, Droplet<Stmt, Rain<Stmt>>> visited;
        Set<Stmt> skip;
        ExceptionalUnitGraph cfg;
        CFGFactory(SootMethod method) {
            this.method = method;
            this.visited = new HashMap<>();
            this.cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());
        }

        Rain<Stmt> build() {
            return filter(build(new LList<>(cfg.getHeads()).map(unit -> Promise.just((Stmt)unit))), new HashSet<>(), new HashMap<>());
        }

        private Rain<Stmt> filter(Rain<Stmt> rain, Set<Box<Stmt>> guards, Map<Box<Stmt>, Rain<Stmt>> cache) {
            return Rain.merge(rain.unfix().map(drop -> Promise.pure(() -> {
                var box = drop.get();
                if (guards.contains(box)) {
                    // break empty cycle
                    return new Rain<>();
                }

                if (cache.containsKey(box)) {
                    // return from cache;
                    return cache.get(box);
                }

                var stmt = box.value();
                if (stmt.containsInvokeExpr()) {
                    var methodRef = stmt.getInvokeExpr().getMethodRef();
                    var declaringClass = methodRef.getDeclaringClass();
                    String methodName = declaringClass.getName() + "." + methodRef.getName();
                    for (Pattern pattern : GraphFactory.this.tags) {
                        if (pattern.matcher(methodName).find()) {
                            var out = new Rain<>(drop.fmap(let -> filter(let, new HashSet<>(), cache)));
                            cache.put(box, out);
                            return out;
                        }
                    }
                }

                logger.trace("{} [{}] skipped", this.method, stmt);
                guards.add(box);
                return filter(drop.next(), guards, cache);
            })));
        }

        private Rain<Stmt> build(LList<Stmt> stmts) {
            // build rain for each stmt and merge
            return new Rain<>(stmts.map(stmt -> Promise.pure(() -> {
                return visited.compute(stmt, ($, drop) -> {
                    if (drop != null) {
                        logger.trace("{} [{}] loaded from cache", this.method, stmt);
                        return drop;
                    }

                    //TODO handle exceptional dests;
    
                    var sucs = cfg.getUnexceptionalSuccsOf(stmt);
                    var rets = build(new LList<>(sucs).map(unit -> Promise.just((Stmt)unit)));
        
                    if (stmt.containsInvokeExpr()) {
                        var methods = new LList<>(cg.edgesOutOf(stmt)).map(edge -> Promise.just(edge.tgt()));
                        logger.trace("{} [{}] expanded with {} successors", this.method, stmt, sucs.size());
                        rets = GraphFactory.this.build(methods, rets);
                    }
                    return new Droplet<>(new Box<>(stmt), rets);
                });
            })));
        }
    }
}
