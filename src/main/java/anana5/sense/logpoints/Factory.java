package anana5.sense.logpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Vertex;
import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.util.Computation;
import anana5.util.LList;
import anana5.util.Path;
import anana5.util.Promise;
import anana5.util.Tuple;
import net.sourceforge.argparse4j.inf.Namespace;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Factory {

    private static Logger logger = LoggerFactory.getLogger(Factory.class);
    private static Factory instance = null;
    private static Transform cpf = PackManager.v().getPack("jop").get("jop.cpf");
    public static Factory v() {
        if (instance == null) {
            instance = new Factory();
        }
        return instance;
    }
    
    private List<Pattern> tags = new ArrayList<>();
    private Map<SootMethod, Rain<Stmt>> visited = new HashMap<>();

    private CallGraph cg;

    public Factory configure(String[] args) {
        return configure(Cmd.parse(args));
    }

    public Factory configure(Namespace ns) {
        this.prepend(ns.getBoolean("prepend"));
        this.classpath(ns.getString("classpath"));
        this.modulepath(ns.getString("modulepath"));
        this.include(ns.<String>getList("include"));
        this.exclude(ns.<String>getList("exclude"));
        this.classes(ns.<String>getList("classes"));
        for (String tag : ns.<String>getList("tag")) {
            this.tag(tag);
        }

        return this;
    }

    public Factory configure() {
        try {
            Options.v().set_output_dir(Files.createTempDirectory("soot").toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory");
        }

        // disable output
        Options.v().set_output_format(Options.output_format_none);

        // application options
        Options.v().set_app(true);
        Options.v().set_include_all(false);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_fix);
        Options.v().set_throw_analysis(Options.throw_analysis_unit);
        Options.v().set_omit_excepting_unit_edges(true);
        Options.v().set_keep_line_number(true);

        // cg options
        Options.v().setPhaseOption("cg.spark", "enabled:true");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        Options.v().setPhaseOption("cg", "safe-forname:true");
        Options.v().setPhaseOption("cg", "safe-newinstance:true");
        Options.v().setPhaseOption("cg", "jdkver:11");
        Options.v().setPhaseOption("cg", "verbose:false");
        Options.v().setPhaseOption("cg", "all-reachable:false");

        // jimple loader options
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        Options.v().setPhaseOption("jb.ls", "enabled:true");
        Options.v().setPhaseOption("jb.a", "enabled:false");
        Options.v().setPhaseOption("jb.ule", "enabled:false");
        Options.v().setPhaseOption("jb.ulp", "enabled:false");
        Options.v().setPhaseOption("jb.lns", "enabled:true");
        Options.v().setPhaseOption("jb.cp", "enabled:false");
        Options.v().setPhaseOption("jb.dae", "enabled:false");
        Options.v().setPhaseOption("jb.cp-ule", "enabled:false");
        Options.v().setPhaseOption("jb.lp", "enabled:false");
        Options.v().setPhaseOption("jb.ne", "enabled:true");
        Options.v().setPhaseOption("jb.uce", "enabled:true");
        Options.v().setPhaseOption("jb.tt", "enabled:true");
        
        // java loader options
        Options.v().setPhaseOption("jj", "use-original-names:true");
        Options.v().setPhaseOption("jj.ls", "enabled:false");
        Options.v().setPhaseOption("jj.a", "enabled:false");
        Options.v().setPhaseOption("jj.ule", "enabled:false");
        Options.v().setPhaseOption("jj.ulp", "enabled:false");
        Options.v().setPhaseOption("jj.lns", "enabled:true");
        Options.v().setPhaseOption("jj.cp", "enabled:false");
        Options.v().setPhaseOption("jj.dae", "enabled:false");
        Options.v().setPhaseOption("jj.cp-ule", "enabled:false");
        Options.v().setPhaseOption("jj.lp", "enabled:false");
        Options.v().setPhaseOption("jj.ne", "enabled:true");
        Options.v().setPhaseOption("jj.uce", "enabled:true");

        return this;
    }

    public Factory prepend(boolean should) {
        Options.v().set_prepend_classpath(should);
        return this;
    }

    public Factory classpath(String classpath) {
        Options.v().set_soot_classpath(classpath);
        return this;
    }

    public Factory modulepath(String modulepath) {
        Options.v().set_soot_modulepath(modulepath);
        return this;
    }

    public Factory classes(List<String> classes) {
        Options.v().classes().clear();
        Options.v().classes().addAll(classes);
        return this;
    }

    public Factory include(List<String> inclusions) {
        Options.v().set_include(inclusions);
        return this;
    }

    public Factory exclude(List<String> exclusions) {
        Options.v().set_exclude(exclusions);
        return this;
    }

    private Factory() {
        // make sure to configure soot
        this.configure();
    }

    public Factory(CallGraph cg) {
        this.cg = cg;
    }

    public Factory tag(Pattern pattern) {
        tags.add(pattern);
        return this;
    }

    public Factory tag(int flags, String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern, flags));
        }
        return this;
    }

    public Factory tag(String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern));
        }
        return this;
    }

    public Rain<Stmt> graph() {
        return build();
    }

    private Rain<Stmt> build() {
        LocalDateTime start = LocalDateTime.now();
        logger.debug("started at {}", start);
        Runnable exitHook = () -> {
            LocalDateTime end = LocalDateTime.now();
            logger.debug("done in {} iterations ({}) at {}", Computation.statistics.iterations(), Duration.between(end, start), end);
        };
        Runtime.getRuntime().addShutdownHook(new Thread(exitHook, "exit"));

        if (this.cg == null) {
            Scene.v().loadNecessaryClasses();
            PackManager.v().getPack("cg").apply();
            this.cg = Scene.v().getCallGraph();
        }

        return build(LList.from(Scene.v().getEntryPoints()), new Path<>(), new HashSet<>())
            .fold(droplets -> Rain.fix(droplets.filter(droplet -> droplet.value() != null)));
    }

    private Rain<Stmt> build(LList<SootMethod> methods, Path<SootMethod> path, boolean guard) {
        // construct subrain for each method
        return Rain.bind(Rain.merge(methods.map(method -> {
            if (visited.containsKey(method)) {
                var rain = visited.get(method);
                if (path.contains(method)) {
                    if (CGGuards.contains(method)) {
                        // break empty cycle
                        logger.trace("{} {} is closing an empty loop", format(path), method);
                        return Rain.empty();
                    } else {
                        logger.trace("{} {} loaded from cache", format(path), method);
                        return rain;
                    }
                } else {
                    // map to new boxes
                    logger.trace("{} {} loaded from cache in new context", format(path), method);
                    return rain.map(v -> new Box(v.value()));
                }
            }

            if (method.isPhantom()){
                logger.trace("{} {} skipped due to being phantom", format(path), method);
                return Rain.of(Drop.of(new Box(null), Rain.empty()));
            }
            
            if (!method.isConcrete()) {
                logger.trace("{} {} skipped due to not being concrete", format(path), method);
                return Rain.of(Drop.of(new Box(null), Rain.empty()));
            } 
            
            if (method.getDeclaringClass().isLibraryClass()) {
                logger.trace("{} {} skipped due to being library method", format(path), method);
                return Rain.of(Drop.of(new Box(null), Rain.empty()));
            }

            var builder = new CFGFactory(method);
            logger.trace("{} {} loaded", format(path), method);
            CGGuards.add(method);
            var rain = builder.build(path.push(method), CGGuards);
            visited.put(method, rain);
            return rain;
        })).resolve());
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Map<Stmt, Rain<Stmt>> memo;
        ExceptionalUnitGraph cfg;
        CFGFactory(SootMethod method) {
            var body = method.retrieveActiveBody();
            if (body instanceof JimpleBody) {
                this.method = method;
                this.memo = new HashMap<>();
                Factory.cpf.apply(body);
                this.cfg = new ExceptionalUnitGraph(body);
                this.sourceName = method.getDeclaringClass().getName() + "." + method.getName();
            } else {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
        }

        Rain<Stmt> build(Path<SootMethod> path, boolean guard) {
            var subpath = new Path<Stmt>();
            return build(LList.from(cfg.getHeads()).map(unit -> (Stmt)unit), path, subpath);
        }

        private Rain<Stmt> build(LList<Stmt> stmts, Path<SootMethod> path, Path<Tuple<Stmt, Boolean>> subpath) {
            // build rain for each stmt and merge
            return Rain.merge(stmts.map(stmt -> {
                if (memo.containsKey(stmt)) {
                    // return from cache;
                    logger.trace("{} [{}]@{} loaded from cache", format(path, subpath));
                    return visited.get(stmt);
                }

                if (subpath.contains(stmt)) {
                    // if empty cycle remove it
                    logger.trace("{} [{}]@{} is closing an empty loop", format(path, subpath));
                    // cannot memo
                    return Rain.empty();
                }

                // return stmt
                if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                    Rain<Stmt> rain = Rain.of(Drop.of(new Box(null), Rain.empty()));
                    memo.put(Key.of(stmt), rain);
                    logger.trace("{} [{}]@{} returned", format(path, subpath));
                    return rain;
                }

                //TODO handle exceptional dests;

                List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
                List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
                LList<Stmt> rets = LList.from(sucs).map(unit -> (Stmt)unit);
    
                if (stmt.containsInvokeExpr()) {
                    SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
                    SootClass declaringClass = methodRef.getDeclaringClass();
                    String methodName = declaringClass.getName() + "." + methodRef.getName();
                    for (Pattern pattern : Factory.this.tags) {
                        if (pattern.matcher(methodName).find()) {
                            // clear guards and make new droplet
                            stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
                            Box box = new Box(stmt);
                            Drop<Stmt, Rain<Stmt>> drop = Drop.of(box, build(rets, path, subpath.push(stmt), false)); // no need to guard since we did not skip this stmt
                            Rain<Stmt> rain = Rain.of(drop);
                            memo.put(Key.of(stmt), rain);
                            logger.trace("{} reported with successors {} by matching {} with tag {}", format(path, subpath), code, methodName, pattern.toString());
                            return rain;
                        }
                    }

                    // get subrain
                    LList<SootMethod> methods = LList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

                    Rain<Stmt> rain = Rain.bind(methods.isEmpty().map(isEmpty -> {
                        if (isEmpty) {
                            // no method resolved
                            logger.trace("{} resolved to nothing with successors {}", format(path, subpath), code);
                            return build(rets, path, subpath.push(stmt), true);
                        } else {
                            logger.trace("{} expanded with successors {}", format(path, subpath), code);
                            Rain<Stmt> subRain = Factory.this.build(methods, path, guard);
                            Rain<Stmt> unguardedRets = build(rets, path, subpath.push(stmt));
                            Rain<Stmt> guardedRets = build(rets, path, subpath.push(stmt));
                            return Rain.merge(subRain.unfix().map(droplet -> {
                                Stmt s = droplet.value();
                                if (s == null) {
                                    return guardedRets;
                                }
                                droplet = droplet.fmap(r -> r.fold(ds -> Rain.merge(ds.map(d -> {
                                    Stmt ss = d.value();
                                    if (ss == null) {
                                        return unguardedRets;
                                    }
                                    return Rain.of(d);
                                }))));
                                return Rain.of(droplet);
                            }));
                        }
                    }));
                    rain = deduplicate(rain);
                    memo.put(stmt, rain); // need to memo guard since the invocation itself is skipped
                    return rain;
                } else {
                    Rain<Stmt> rain = build(rets, path, subpath.push(stmt), guard);
                    rain = deduplicate(rain);
                    memo.put(stmt, rain);
                    logger.trace("{} skipped with successors {}", format(path, subpath, guard), code);
                    return rain;
                }
            }));
        }

        private Rain<Stmt> deduplicate(Rain<Stmt> rain) {
            Set<Vertex<Stmt>> seen = new HashSet<>();
            return Rain.bind(rain.unfix().foldl(LList.<Drop<Stmt, Rain<Stmt>>>empty(), (drop, let) -> {
                var box = drop.vertex();
                if (seen.contains(box)) {
                    return let;
                }
                seen.add(box);
                return LList.cons(drop, let);
            }).then(let -> Promise.just(Rain.fix(let))));
        }

        Rain<Stmt> context(Rain<Stmt> rain, Rain<Stmt> guardedRets, Rain<Stmt> unguardedRets, boolean shouldGuard, Set<Vertex<?>> seen) {
            return Rain.merge(rain.unfix().map(droplet -> {
                Vertex<Stmt> box = droplet.vertex();
                if (seen.contains(box)) {
                    return Rain.of(droplet);
                }
                seen.add(box);
                Stmt s = box.value();
                if (!(s instanceof ReturnStmt || s instanceof ReturnVoidStmt)) {
                    return Rain.of(droplet.fmap(r -> context(r, guardedRets, unguardedRets, false, seen)));
                }
                if (shouldGuard) {
                    return guardedRets;
                } else {
                    return unguardedRets;
                }
            }));
        }
    }
    

    String format(Path<SootMethod> path, Path<Stmt> subpath, boolean guard) {
        String fpath;
        if (path.length() == 0) {
            fpath = "0::<>";
        }
        var method = path.head().get();
        fpath = path.length() + "::" + method.toString() + "@" + method.hashCode();

        String fsubp;
        if (subpath.length() == 0) {
            fsubp = "0::[]";
        }
        var stmt = subpath.head().get();
        fsubp = subpath.length() + "::" + stmt.toString() + "@" + stmt.hashCode();

        if (guard) {
            return fpath + " " + fsubp + " [guarded]";
        } else {
            return fpath + " " + fsubp;
        }
    }

    boolean cycle(Stmt stmt, Path<Tuple<Stmt, Boolean>> subpath) {
    }

    public static class Key<T> {
        private final T t;
        private Key(T t) {
            this.t = t;
        }
        public static <T> Key<T> of(T t) {
            return new Key<>(t);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Key<?>)) {
                return false;
            }
            Key<?> other = (Key<?>) obj;
            return Objects.equals(this.t, other.t);
        }
    }

    public static class GuardedKey<T> extends Key<T> {
        private final boolean guard;
        private GuardedKey(T t, boolean guard) {
            super(t);
            this.guard = guard;
        }
        public static <T> GuardedKey<T> of(T t, boolean guard) {
            return new GuardedKey<>(t, guard);
        }
        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && (!(obj instanceof GuardedKey<?>) || Objects.equals(this.guard, ((GuardedKey<?>) obj).guard));
        }
    }
}
