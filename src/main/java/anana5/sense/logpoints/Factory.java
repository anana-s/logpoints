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
import java.util.Set;
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
    private Map<SootMethod, Rain<Stmt>> memo = new HashMap<>();

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

        return build(LList.from(Scene.v().getEntryPoints()), new Path<>())
            .fold(droplets -> Rain.fix(droplets.filter(droplet -> droplet.value() != null)));
    }

    private Rain<Stmt> build(LList<SootMethod> methods, Path<Tuple<SootMethod, Boolean>> path) {
        // build clinit first
        final var cs = methods.filter(method ->  method.getName().equals("<clinit>"));
        final var ms = methods.filter(method -> !method.getName().equals("<clinit>"));

        final var cr = Rain.bind(Rain.<Stmt>merge(cs.map(method -> build(method, path))).resolve());
        final var mr = Promise.just(() -> Rain.bind(Rain.<Stmt>merge(ms.map(method -> build(method, path))).resolve()));

        return Rain.<Stmt>bind(cr.empty().map(e -> {
            if (e) {
                return mr.join();
            }

            return cr.<Rain<Stmt>>fold(ds -> Rain.merge(ds.map(d -> {
                var stmt = d.value();
                if (stmt == null) {
                    return mr.join();
                }
                return Rain.of(d);
            })));
        }));
    }

    private Rain<Stmt> build(SootMethod method, Path<Tuple<SootMethod, Boolean>> path) {
        if (memo.containsKey(method)) {
            var rain = memo.get(method);
            var knot = knot(method, path); // fst is recursive, snd is knot
            if (knot.fst()) {
                if (knot.snd()) {
                    // break empty cycle
                    logger.trace("{} is closing an empty loop", format(path, method));
                    return Rain.of();
                } else {
                    logger.trace("{} loaded from cache", format(path, method));
                    return rain;
                }
            } else {
                // map to new boxes
                logger.trace("{} loaded from cache in new context", format(path, method));
                return rain.map(v -> new Box(v.value()));
            }
        }

        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(path, method));
            return Rain.of(Drop.of(new Box(null), Rain.of()));
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(path, method));
            return Rain.of(Drop.of(new Box(null), Rain.of()));
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(path, method));
            return Rain.of(Drop.of(new Box(null), Rain.of()));
        }

        var builder = new CFGFactory(method);
        logger.trace("{} loading", format(path, method));
        var rain = builder.build(path);
        memo.put(method, rain);
        return rain;
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

        public Rain<Stmt> build(Path<Tuple<SootMethod, Boolean>> path) {
            return build(LList.from(cfg.getHeads()).map(unit -> (Stmt)unit), path, new Path<>(), false);
        }

        private Rain<Stmt> build(LList<Stmt> stmts, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            return Rain.merge(stmts.map(stmt -> build(stmt, path, subpath, guard)));
        }

        private Rain<Stmt> build(Stmt stmt, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            if (memo.containsKey(stmt)) {
                if (knot(stmt, subpath)) {
                    // break cycles
                    logger.trace("{} is closing an empty loop", format(path, method, stmt));
                    return Rain.of();
                }
                // return from cache;
                logger.trace("{} loaded from cache", format(path, method, stmt));
                return memo.get(stmt);
            }

            // return stmt
            if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                var rain = Rain.of(Drop.of(new Box(null), Rain.of()));
                memo.put(stmt, rain);
                logger.trace("{} returning", format(path, method, stmt));
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
                        stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
                        logger.trace("{} matched with tag {}, reporting with successors {}", format(path, method, stmt), pattern.toString(), code);
                        var rain = Rain.of(Drop.of(new Box(stmt), build(rets, path, subpath.push(Tuple.of(stmt, true)), true)));
                        memo.put(stmt, rain);
                        return rain;
                    }
                }

                // get subrain
                final var methods = LList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

                var rain = Rain.bind(methods.empty().<Rain<Stmt>>map(e -> {
                    if (e) {
                        // no method resolved
                        logger.trace("{} failed to resolve, skipping with successors {}", format(path, method, stmt), code);
                        return build(rets, path, subpath.push(Tuple.of(stmt, false)), guard);
                    }
                    logger.trace("{} expanding with successors {}", format(path, method, stmt), code);
                    final var r = Factory.this.build(methods, path.push(Tuple.of(this.method, guard)));
                    final var guarded = build(rets, path, subpath.push(Tuple.of(stmt, false)), guard);
                    final var unguarded = build(rets, path, subpath.push(Tuple.of(stmt, true)), true);
                    return Rain.<Stmt>merge(r.unfix().<Rain<Stmt>>map(d -> {
                        var s = d.value();
                        if (s == null) {
                            return guarded;
                        }
                        return r.fold(d$s -> Rain.merge(d$s.map(d$ -> {
                            var s$ = d$.value();
                            if (s$ == null) {
                                return unguarded;
                            }
                            return Rain.of(d$);
                        })));
                    }));
                }));
                rain = deduplicate(rain);
                memo.put(stmt, rain);
                return rain;
            } else {
                var rain = build(rets, path, subpath.push(Tuple.of(stmt, false)), guard);
                logger.trace("{} skipped with successors {}", format(path, method, stmt), code);
                rain = deduplicate(rain);
                memo.put(stmt, rain);
                return rain;
            }
        }

        private Rain<Stmt> deduplicate(Rain<Stmt> rain) {
            Set<Vertex<Stmt>> seen = new HashSet<>();
            return Rain.<Stmt>bind(rain.unfix().foldl(LList.<Drop<Stmt, Rain<Stmt>>>of(), (drop, next) -> {
                var box = drop.vertex();
                if (seen.contains(box)) {
                    return next;
                }
                seen.add(box);
                return LList.cons(drop, next);
            }).then(next -> Promise.just(Rain.<Stmt>fix(next))));
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

    String format(Path<Tuple<SootMethod, Boolean>> path, SootMethod method) {
        return path.length() + "::" + method.toString() + "@[" + method.hashCode() + "]";
    }

    String format(Path<Tuple<SootMethod, Boolean>> path, SootMethod method, Stmt stmt) {
        return format(path, method) + " [" + stmt.toString() + "]@[" + stmt.hashCode() + "]";
    }

    boolean knot(Stmt stmt, Path<Tuple<Stmt, Boolean>> subpath) {
        if (subpath.empty()) {
            return false;
        }

        var t = subpath.head().get();
        return !t.snd() && (stmt.equals(t.fst()) || knot(stmt, subpath.tail().get()));
    }

    Tuple<Boolean, Boolean> knot(SootMethod method, Path<Tuple<SootMethod, Boolean>> path) {
        return knot(method, path, false);
    }
    Tuple<Boolean, Boolean> knot(SootMethod method, Path<Tuple<SootMethod, Boolean>> path, boolean guard) {
        if (path.empty()) {
            return Tuple.of(false, false);
        }

        var t = path.head().get();
        if (t.snd()) {
            return Tuple.of(false, false);
        } else if (method.equals(t.fst())) {
            return Tuple.of(true, t.snd() || guard);
        } else {
            return knot(method, path.tail().get(), t.snd() || guard);
        }
    }
}
