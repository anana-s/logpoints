package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Box;
import anana5.graph.rainfall.Droplet;
import anana5.graph.rainfall.Rain;
import anana5.util.LList;
import anana5.util.Path;
import anana5.util.Promise;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class GraphFactory {

    static Logger logger = LoggerFactory.getLogger(GraphFactory.class);
    

    CallGraph cg;
    List<Pattern> tags = new ArrayList<>();
    Map<SootMethod, Rain<Stmt>> visited = new HashMap<>();

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
        return build(new LList<>(entrypoints), new Path<>(), new HashSet<>());
    }

    private Rain<Stmt> build(LList<SootMethod> methods, Path<SootMethod> path, Set<SootMethod> CGGuards) {
        // construct subrain for each method
        return Rain.merge(methods.map(method -> {
            if (visited.containsKey(method)) {
                var rain = visited.get(method);
                if (path.contains(method)) {
                    if (CGGuards.contains(method)) {
                        // break empty cycle
                        logger.trace("{} skipped due to closing empty loop", method);
                        return new Rain<>();
                    } else {
                        logger.trace("{} loaded from cache", method);
                        return rain;
                    }
                } else {
                    // map to new boxes
                    logger.trace("{} loaded from cache in new context", method);
                    return rain.map(Function.identity());
                }
            }

            if (method.isPhantom()){
                logger.trace("{} skipped due to being phantom", method);
                return new Rain<>();
            }
            
            if (!method.isConcrete()) {
                logger.trace("{} skipped due to not being concrete", method);
                return new Rain<>();
            } 
            
            if (method.getDeclaringClass().isLibraryClass()) {
                logger.trace("{} skipped due to being library method", method);
                return new Rain<>();
            }

            var builder = new CFGFactory(method);
            logger.trace("{} loaded", method);
            CGGuards.add(method);
            var rain = builder.build(path.push(method), CGGuards);
            visited.put(method, rain);
            return rain;
        }));
    }

    class CFGFactory {
        SootMethod method;
        Map<Stmt, Rain<Stmt>> visited;
        ExceptionalUnitGraph cfg;
        CFGFactory(SootMethod method) {
            this.method = method;
            this.visited = new HashMap<>();
            this.cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());
        }

        Rain<Stmt> build(Path<SootMethod> path, Set<SootMethod> CGGuards) {
            return build(new LList<>(cfg.getHeads()).map(unit -> (Stmt)unit), new HashSet<>(), path, CGGuards);
        }

        private Rain<Stmt> build(LList<Stmt> stmts, Set<Stmt> CFGGuards, Path<SootMethod> path, Set<SootMethod> CGGuards) {
            // build rain for each stmt and merge
            return Rain.merge(stmts.map(stmt -> {
                if (CFGGuards.contains(stmt)) {
                    // break empty cycle
                    logger.trace("{} [{}] is closing an empty loop", method, stmt);
                    return new Rain<Stmt>();
                }
                
                if (visited.containsKey(stmt)) {
                    // return from cache;
                    logger.trace("{} [{}] loaded from cache", method, stmt);
                    return visited.get(stmt);
                }

                //TODO handle exceptional dests;

                var sucs = cfg.getUnexceptionalSuccsOf(stmt);
                var rets = new LList<>(sucs).map(unit -> (Stmt)unit);
    
                if (stmt.containsInvokeExpr()) {
                    var methodRef = stmt.getInvokeExpr().getMethodRef();
                    var declaringClass = methodRef.getDeclaringClass();
                    String methodName = declaringClass.getName() + "." + methodRef.getName();
                    for (Pattern pattern : GraphFactory.this.tags) {
                        if (pattern.matcher(methodName).find()) {
                            // clear guards and make new droplet
                            var drop = new Droplet<>(stmt, build(rets, new HashSet<>(), path, new HashSet<>()));
                            var rain = new Rain<>(drop);
                            visited.put(stmt, rain);
                            logger.trace("{} [{}] reported with {} successors", method, stmt, sucs.size());
                            return rain;
                        }
                    }

                    // get subrain
                    var methods = new LList<>(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());
                    var subrain = GraphFactory.this.build(methods, path, CGGuards);

                    // check if empty, if so, add guard.
                    // connect return values
                    var rain = Rain.bind(subrain.isEmpty().then(isEmpty ->{
                        if (isEmpty) {
                            CFGGuards.add(stmt);
                            return Promise.just(build(rets, CFGGuards, path, CGGuards));
                        } else {
                            // clear guards
                            logger.trace("cg guards cleared");
                            logger.trace("{} cfg guards cleared", method);
                            var r = build(rets, new HashSet<>(), path, new HashSet<>());
                            var s = subrain.fold(droplets -> {
                                return Rain.bind(droplets.isEmpty().map(condition -> {
                                    if (condition) {
                                        return r;
                                    } else {
                                        return new Rain<>(droplets);
                                    }
                                }));
                            });
                            return Promise.just(s);
                        }
                    }));
                    visited.put(stmt, rain);
                    logger.trace("{} [{}] expanded with {} successors", method, stmt, sucs.size());
                    return rain;
                } else {
                    CFGGuards.add(stmt);
                    var rain = build(rets, CFGGuards, path, CGGuards);
                    rain = deduplicate(rain);
                    visited.put(stmt, rain);
                    logger.trace("{} [{}] skipped with {} successors", method, stmt, sucs.size());
                    return rain;
                }
            }));
        }

        Rain<Stmt> deduplicate(Rain<Stmt> rain) {
            Set<Box<Stmt>> seen = new HashSet<>();
            return Rain.bind(rain.unfix().foldr(new LList<Droplet<Stmt, Rain<Stmt>>>(), (drop, let) -> {
                var box = drop.get();
                if (seen.contains(box)) {
                    return let;
                }
                seen.add(box);
                return new LList<>(drop, let);
            }).then(let -> Promise.just(new Rain<Stmt>(let))));
        }
    }
}
