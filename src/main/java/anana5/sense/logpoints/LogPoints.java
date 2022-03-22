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

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.util.Computation;
import anana5.util.Knot;
import anana5.util.PList;
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
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class LogPoints {

    private static Logger logger = LoggerFactory.getLogger(LogPoints.class);
    private static LogPoints instance = null;
    private static Transform cpf = PackManager.v().getPack("jop").get("jop.cpf");
    public static LogPoints v() {
        if (instance == null) {
            instance = new LogPoints();
        }
        return instance;
    }

    private List<Pattern> tags = new ArrayList<>();

    private CallGraph cg;

    public LogPoints configure(String[] args) {
        return configure(Cmd.parse(args));
    }

    public LogPoints configure(Namespace ns) {
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

    public LogPoints configure() {
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

    public LogPoints prepend(boolean should) {
        Options.v().set_prepend_classpath(should);
        return this;
    }

    public LogPoints classpath(String classpath) {
        Options.v().set_soot_classpath(classpath);
        return this;
    }

    public LogPoints modulepath(String modulepath) {
        Options.v().set_soot_modulepath(modulepath);
        return this;
    }

    public LogPoints classes(List<String> classes) {
        Options.v().classes().clear();
        Options.v().classes().addAll(classes);
        return this;
    }

    public LogPoints include(List<String> inclusions) {
        Options.v().set_include(inclusions);
        return this;
    }

    public LogPoints exclude(List<String> exclusions) {
        Options.v().set_exclude(exclusions);
        return this;
    }

    private LogPoints() {
        // make sure to configure soot
        this.configure();
    }

    public LogPoints(CallGraph cg) {
        this.cg = cg;
    }

    public LogPoints tag(Pattern pattern) {
        tags.add(pattern);
        return this;
    }

    public LogPoints tag(int flags, String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern, flags));
        }
        return this;
    }

    public LogPoints tag(String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern));
        }
        return this;
    }

    public Rain<Box<Stmt>.Ref> graph() {
        var seen = new HashSet<Box<Stmt>.Ref>();
        var done = new HashMap<Box<Stmt>.Ref, Rain<Box<Stmt>.Ref>>();
        // return build();
        return Rain.bind(build().fold(drops -> {
            PList<Promise<Rain<Box<Stmt>.Ref>>> promises = drops.map(drop -> {
                final var v = drop.get();
                if (done.containsKey(v)) {
                    return Promise.just(done.get(v));
                } else if (v.sentinel()) {
                    if (seen.contains(v)) {
                        seen.remove(v);
                        return Promise.just(Rain.of());
                    }
                    seen.add(v);
                    return drop.next().then(rain -> {
                        seen.remove(v);
                        return rain.empty().then(e -> {
                            if (!e) {
                                done.put(v, rain);
                            }
                            return Promise.just(rain);
                        });
                    });
                } else {
                    var rain = Rain.of(Drop.of(drop.get(), Rain.bind(drop.next())));
                    done.put(v, rain);
                    return Promise.just(rain);
                }
            });

            var out = promises.foldr(Rain.<Box<Stmt>.Ref>of(), (promise, rain) -> promise.then(r -> Promise.just(Rain.merge(rain, r))));
            return out.map(rain -> Rain.fix(process(rain.unfix())));
        }));
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

        return build(null, PList.from(Scene.v().getEntryPoints()), new Path());
    }

    private Rain<Box<Stmt>.Ref> build(Stmt invoker, PList<SootMethod> methods, Path path) {
        // build clinit first
        // var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        // var cr = Rain.merge(cs.map(m -> build(invoker, m, path)));
        var mr = Rain.merge(ms.map(m -> build(invoker, m, path)));

        // if (logger.isTraceEnabled()) {
        //     return Rain.bind(cr.resolve().then(resolvedCr -> {
        //         var knot = Knot.tie(a -> connect(resolvedCr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        //         return knot.snd();
        //         // return Promise.just(Rain.merge(mr, connect(resolvedCr, mr)));
        //     }));
        // }

        // var knot = Knot.tie(a -> connect(cr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        // return Rain.bind(knot.snd());

        return mr;
    }

    private Map<SootMethod, Rain<Box<Stmt>.Ref>> memo1 = new HashMap<>();
    private Rain<Box<Stmt>.Ref> build(Stmt invoker, SootMethod method, Path path) {
        var curr = path.push(invoker, method);

        if (memo1.containsKey(method)) {
            logger.trace("{} loaded from cache", format(path, method));
            return memo1.get(method).map(v -> curr.box().copy(v));
        }

        var box = curr.box();

        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return rain;
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return rain;
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return rain;
        }

        logger.trace("{} loading", format(path, method));
        var builder = new CFGFactory(curr, method);
        var rain = builder.build();
        memo1.put(method, rain);
        return rain;
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Map<Stmt, Drop<Box<Stmt>.Ref, Rain<Box<Stmt>.Ref>>> memo1;
        Set<Stmt> memo0;
        ExceptionalUnitGraph cfg;
        Path path;
        CFGFactory(Path path, SootMethod method) {
            var body = method.retrieveActiveBody();
            if (body instanceof JimpleBody) {
                this.path = path;
                this.method = method;
                this.memo0 = new HashSet<>();
                this.memo1 = new HashMap<>();
                LogPoints.cpf.apply(body);
                this.cfg = new ExceptionalUnitGraph(body);
                this.sourceName = method.getDeclaringClass().getName() + "." + method.getName();
            } else {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
        }

        public Rain<Box<Stmt>.Ref> build() {
            var rain = build(PList.from(cfg.getHeads()).map(unit -> (Stmt)unit));
            if (logger.isTraceEnabled()) {
                rain = Rain.bind(rain.resolve());
            }
            return rain;
        }

        private Rain<Box<Stmt>.Ref> build(PList<Stmt> stmts) {
            return Rain.fix(stmts.map(stmt -> build(stmt)));
        }

        private Drop<Box<Stmt>.Ref, Rain<Box<Stmt>.Ref>> build(Stmt stmt) {
            // if there is no cycle, we can load from cache
            if (memo1.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(path, method, stmt));
                return memo1.get(stmt);
            }

            //TODO handle exceptional dests;

            List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
            List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
            PList<Stmt> next = PList.from(sucs).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr()) {
                // skip this statement
                logger.trace("{} skipped with successors {}", format(path, method, stmt), code);
                final var drop = Drop.of(path.box().of(true, stmt), build(next));
                memo1.put(stmt, drop);
                return drop;
            }

            // stmt is an invokation, so get call information
            SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
            SootClass declaringClass = methodRef.getDeclaringClass();
            String methodName = declaringClass.getName() + "." + methodRef.getName();

            // check if stmt needs to be kept
            for (Pattern pattern : LogPoints.this.tags) {
                if (pattern.matcher(methodName).find()) {
                    logger.trace("{} matched with tag {}, continuing with successors {}", format(path, method, stmt), pattern.toString(), code);
                    stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
                    final var drop = Drop.of(path.box().of(stmt), build(next));
                    memo1.put(stmt, drop);
                    return drop;
                }
            }

            // get rain of called methods
            final PList<SootMethod> methods = PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

            logger.trace("{} expanding with successors {}", format(path, method, stmt), code);

            final var succs = Rain.bind(methods.empty().<Rain<Box<Stmt>.Ref>>map(noMethods -> {
                if (noMethods) {
                    logger.trace("{} expands to nothing", format(path, method, stmt));
                    return build(next);
                }
                final var subrain = LogPoints.this.build(stmt, methods, path);
                return connect(subrain, build(next));

            }));

            return Drop.of(path.box().of(true, stmt), succs);
        }
    }

    private static <T> PList<Drop<Box<Stmt>.Ref, T>> process(PList<Drop<Box<Stmt>.Ref, T>> drops) {
        var seen = new HashSet<>();
        return drops.filter(drop -> {
            var box = drop.get();
            if (seen.contains(drop.get())) {
                return Promise.just(false);
            }
            seen.add(box);
            return Promise.just(true);
        });
    }

    private static Rain<Box<Stmt>.Ref> connect(Rain<Box<Stmt>.Ref> rain, Rain<Box<Stmt>.Ref> rets) {
        return rain.fold(drops -> Rain.fix(drops.map(drop -> {
            Stmt s = drop.get().value();
            if (s instanceof ReturnStmt || s instanceof ReturnVoidStmt) {
                return Drop.of(drop.get(), Rain.<Box<Stmt>.Ref>bind(drop.next().empty().map(e -> e ? rets : drop.next())));
            }
            return drop;
        })));
    }

    private static String format(Path path, SootMethod method) {
        return path.length() + "::[" + method.getDeclaringClass().getName() + "." + method.getName() + "]@[" + method.hashCode() + "]";
    }

    private static String format(Path path, SootMethod method, Stmt stmt) {
        if (stmt == null) {
            return format(path, method) + " [null]";
        }
        return format(path, method) + " [" + stmt.toString() + "]@[" + stmt.hashCode() + "]";
    }
}
