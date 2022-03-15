package anana5.sense.logpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private Map<SootMethod, Rain<Box<Stmt>.Ref>> memo = new HashMap<>();

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

    public Rain<Box<Stmt>.Ref> graph() {
        return build();
    }

    private Rain<Box<Stmt>.Ref> build() {
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
            .fold(drops -> Rain.fix(drops.filter(drop -> Promise.just(!drop.get().sentinel()))));
    }

    private Rain<Box<Stmt>.Ref> build(LList<SootMethod> methods, Path<Tuple<SootMethod, Boolean>> path) {
        // build clinit first
        var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        Rain<Box<Stmt>.Ref> cr;
        Rain<Box<Stmt>.Ref> mr;

        if (logger.isTraceEnabled()) {
            cr = Rain.merge(cs.map(method -> build(method, path))).resolve();
            mr = Rain.merge(ms.map(method -> build(method, path))).resolve();
        } else {
            cr = Rain.merge(cs.map(method -> build(method, path)));
            mr = Rain.merge(ms.map(method -> build(method, path)));
        }

        return Rain.bind(cr.empty().fmap(e -> {
            if (e) {
                return mr;
            }

            return cr.fold(drops -> Rain.merge(drops.map(drop -> {
                if (drop.get().sentinel()) {
                    return mr;
                }
                return Rain.of(drop);
            })));
        }));
    }

    private final Box<Stmt> box = new Box<>();

    private Rain<Box<Stmt>.Ref> build(SootMethod method, Path<Tuple<SootMethod, Boolean>> path) {
        var knot = knot(method, path); // fst is recursive, snd is knot
        if (knot.fst()) {
            if (knot.snd()) {
                // break empty cycle
                logger.trace("{} is closing an empty loop", format(path, method));
                return Rain.of();
            } else {
                logger.trace("{} loaded from cache", format(path, method));
                return memo.get(method);
            }
        }

        if (memo.containsKey(method)) {
            // map to new boxes
            logger.trace("{} loaded from cache in new context", format(path, method));
            Box<Stmt> box = new Box<>();
            return memo.get(method).map(v -> box.copy(v));
        }

        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(path, method));
            return Rain.of(Drop.of(box.sentinel(), Rain.of()));
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(path, method));
            return Rain.of(Drop.of(box.sentinel(), Rain.of()));
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(path, method));
            return Rain.of(Drop.of(box.sentinel(), Rain.of()));
        }

        var builder = new CFGFactory(method);
        logger.trace("{} loading", format(path, method));
        var rain = builder.build(path);
        return Rain.bind(rain.empty().fmap(e -> {
            if (!e) {
                memo.put(method, rain);
            }
            return rain;
        }));
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Map<Stmt, Rain<Box<Stmt>.Ref>> memo;
        ExceptionalUnitGraph cfg;
        Box<Stmt> box;
        CFGFactory(SootMethod method) {
            var body = method.retrieveActiveBody();
            if (body instanceof JimpleBody) {
                this.method = method;
                this.memo = new HashMap<>();
                Factory.cpf.apply(body);
                this.cfg = new ExceptionalUnitGraph(body);
                this.sourceName = method.getDeclaringClass().getName() + "." + method.getName();
                this.box = new Box<>();
            } else {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
        }

        public Rain<Box<Stmt>.Ref> build(Path<Tuple<SootMethod, Boolean>> path) {
            return build(LList.from(cfg.getHeads()).map(unit -> (Stmt)unit), path, new Path<>(), false);
        }

        private Rain<Box<Stmt>.Ref> build(LList<Stmt> stmts, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            return Rain.merge(stmts.map(stmt -> build(stmt, path, subpath, guard)));
        }

        private Rain<Box<Stmt>.Ref> build(Stmt stmt, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            // if we have already seen this stmt
            if (memo.containsKey(stmt)) {
                logger.trace("{} loading from cache", format(path, method, stmt));
                return memo.get(stmt);
            }

            // if there is an empty cycle
            var knot = knot(stmt, subpath);
            if (knot.fst() && knot.snd()) {
                logger.trace("{} is closing an empty loop", format(path, method, stmt));
                return Rain.of();
            }

            // if stmt is a return statement
            if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                logger.trace("{} returning", format(path, method, stmt));
                // stop building
                var rain =  Rain.of(Drop.of(box.sentinel(stmt), Rain.of()));
                memo.put(stmt, rain);
                return rain;
            }

            //TODO handle exceptional dests;

            List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
            List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
            LList<Stmt> next = LList.from(sucs).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr()) {
                // skip this statement
                logger.trace("{} skipped with successors {}", format(path, method, stmt), code);
                var rain = process(build(next, path, subpath.push(Tuple.of(stmt, false)), guard));
                return Rain.bind(rain.empty().fmap(e -> {
                    if (!e) {
                        memo.put(stmt, rain);
                    }
                    return rain;
                }));
            }

            // stmt is an invokation, so get call information
            SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
            SootClass declaringClass = methodRef.getDeclaringClass();
            String methodName = declaringClass.getName() + "." + methodRef.getName();

            // check if stmt needs to be kept
            for (Pattern pattern : Factory.this.tags) {
                if (pattern.matcher(methodName).find()) {
                    logger.trace("{} matched with tag {}, continuing with successors {}", format(path, method, stmt), pattern.toString(), code);

                    // tag the stmt with source information
                    stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));

                    // keep this stmt and build drop
                    var succs = process(build(next, path, subpath.push(Tuple.of(stmt, true)), true));
                    var rain = Rain.of(Drop.of(box.of(stmt), succs));
                    memo.put(stmt, rain);
                    return rain;
                }
            }

            // stmt is not kept, so expand the invokation
            if (guard) {
                logger.trace("{} expanding [guarded] with successors {}", format(path, method, stmt), code);
            } else {
                logger.trace("{} expanding [unguarded] with successors {}", format(path, method, stmt), code);
            }

            // get rain of called methods
            final var methods = LList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

            return Rain.bind(methods.empty().then(noMethods -> {
                if (noMethods) {
                    logger.trace("{} expands to nothing", format(path, method, stmt));
                    var rain = process(build(next, path, subpath.push(Tuple.of(stmt, false)), guard));
                    return rain.empty().fmap(e -> {
                        if (!e) {
                            memo.put(stmt, rain);
                        }
                        return rain;
                    });
                }

                Rain<Box<Stmt>.Ref> subrain = Factory.this.build(methods, path.push(Tuple.of(method, guard)));
                var unguarded = Rain.bind(Promise.just(() -> process(build(next, path, subpath.push(Tuple.of(stmt, false)), guard))));
                var guarded = Rain.bind(Promise.just(() -> process(build(next, path, subpath.push(Tuple.of(stmt, true)), true))));
                var rain = Rain.merge(subrain.unfix().map(drop -> {
                    if (drop.get().sentinel()) {
                        logger.trace("{} returned [unguarded] to {}", format(path, method, stmt), code);
                        return unguarded;
                    }

                    return drop.next().fold(drops$ -> Rain.merge(drops$.map(drop$ -> {
                        if (drop$.get().sentinel()) {
                            logger.trace("{} returned [guarded] to {}", format(path, method, stmt), code);
                            return guarded;
                        }
                        return Rain.of(drop);
                    })));
                }));
                rain.empty().fmap(e -> {
                    if (!e) {
                        memo.put(stmt, rain);
                    }
                    return rain;
                });
                return Promise.just(rain);
            }));
        }

        private Rain<Box<Stmt>.Ref> process(Rain<Box<Stmt>.Ref> rain) {
            var seen = new HashSet<>();
            var r = rain.unfix().filter(drop -> {
                var box = drop.get();
                if (seen.contains(drop.get())) {
                    return Promise.just(false);
                }
                seen.add(box);
                return Promise.just(true);
            });

            return Rain.fix(r);
        }
    }

    String format(Path<Tuple<SootMethod, Boolean>> path, SootMethod method) {
        return path.length() + "::" + method.toString() + "@[" + method.hashCode() + "]";
    }

    String format(Path<Tuple<SootMethod, Boolean>> path, SootMethod method, Stmt stmt) {
        return format(path, method) + " [" + stmt.toString() + "]@[" + stmt.hashCode() + "]";
    }

    <T> Tuple<Boolean, Boolean> knot(T t, Path<Tuple<T, Boolean>> path) {
        return knot(t, path, false);
    }
    <T> Tuple<Boolean, Boolean> knot(T t, Path<Tuple<T, Boolean>> path, boolean guard) {
        if (path.empty()) {
            return Tuple.of(false, !guard);
        }

        var tuple = path.head().get();
        if (t.equals(tuple.fst())) {
            return Tuple.of(true, !(guard || tuple.snd()));
        } else {
            return knot(t, path.tail().get(), guard || tuple.snd());
        }
    }
}
