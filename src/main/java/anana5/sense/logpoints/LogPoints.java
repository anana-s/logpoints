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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.graph.rainfall.RainGraph;
import anana5.sense.logpoints.Box.Ref;
import anana5.util.Computation;
import anana5.util.PList;
import anana5.util.Promise;
import anana5.util.Tuple;
import soot.Body;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class LogPoints {

    /**
     * config
     */

    private static Logger logger = LoggerFactory.getLogger(LogPoints.class);
    private static LogPoints instance = null;
    private static Transform cpf = PackManager.v().getPack("jop").get("jop.cpf");
    public static LogPoints v() {
        if (instance == null) {
            instance = new LogPoints();
        }
        return instance;
    }
    private boolean trace = false;
    private List<Pattern> tags = new ArrayList<>();

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

    public LogPoints trace(boolean trace) {
        this.trace = trace || logger.isTraceEnabled();
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

    /**
     * RainGraph builder
     */

    private final Map<SootMethod, Rain<Box.Ref>> done = new HashMap<>();
    private SerialRefRainGraph graph;
    private CallGraph cg;

    public final synchronized SerialRefRainGraph graph() {
        if (graph == null) {
            graph = new SerialRefRainGraph(build());
        }
        return graph;
    }

    // static Promise<Rain<Ref>> merge(PList<Promise<Rain<Ref>>> promises) {
    //     var out = Rain.<Ref>bind(promises.foldl(Promise.just(Rain.of()), (promise, acc) -> {
    //         return promise.then(rain -> {
    //             return acc.then(accRain -> {
    //                 return Promise.just(Rain.merge(rain, accRain));
    //             });
    //         });
    //     }));
    //     return Promise.just(out);
    // }

    protected final Rain<Box.Ref> build() {
        LocalDateTime start = LocalDateTime.now();
        logger.debug("started at {} with trace: {}", start, trace);

        if (this.cg == null) {
            Scene.v().loadNecessaryClasses();
            PackManager.v().getPack("cg").apply();
            this.cg = Scene.v().getCallGraph();
        }

        var rain = build(null, PList.from(Scene.v().getEntryPoints()), PList.of());
        rain = Rain.fix(rain.unfix().filter(drop -> !isReturn(drop.get().get())));
        return rain;
    }

    // private final Rain<Box.Ref> postprocess(Rain<Box.Ref> rain, Map<Ref, Rain<Ref>> memo) {
    //     return Rain.merge(rain.unfix().<Rain<Ref>>map(drop -> {
    //         var ref = drop.get();
    //         if (isReturn(ref.get())) {
    //             return Rain.of();
    //         }
    //         if (memo.containsKey(ref)) {
    //             return memo.get(ref);
    //         }
    //         var out = Rain.<Ref>of(Drop.of(ref, Rain.bind(Promise.lazy(() -> postprocess(drop.next(), memo)))));
    //         memo.put(ref, out);
    //         return out;
    //     }));
    // }

    private final Rain<Box.Ref> build(Stmt invoker, PList<SootMethod> methods, PList<SootMethod> strand) {
        // build clinit first
        // var cs = methods.filter(method -> method.getName().equals("<clinit>"));
        var ms = methods.filter(method -> !method.getName().equals("<clinit>"));

        // var cr = merge(cs.map(m -> build(invoker, m, strand)));
        var mr = Rain.merge(ms.map(m -> build(invoker, m, strand)));

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

    private final Rain<Box.Ref> build(Stmt invoker, SootMethod method, PList<SootMethod> strand) {
        if (done.containsKey(method)) {
            logger.trace("{} loaded from cache", format(method));
            return done.get(method);
        }

        if (strand.contains(method)) {
            logger.trace("{} untied recursion knot", format(method));
            return Rain.of();
        }


        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(method));
            var rain = fixture(method);
            done.put(method, rain);
            return rain;
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(method));
            var rain = fixture(method);
            done.put(method, rain);
            return rain;
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(method));
            var rain = fixture(method);
            done.put(method, rain);
            return rain;
        }

        logger.trace("{} loading", format(method));
        var body = method.retrieveActiveBody();
        var factory = new CFGFactory(body, strand);
        var rain = factory.build();
        rain = process(rain);
        done.put(method, rain);
        return rain;
    }

    private static final Rain<Box.Ref> fixture(SootMethod method) {
        Stmt stmt = new JReturnVoidStmt();
        stmt.addTag(new SourceMapTag(String.format("%s.%s", method.getDeclaringClass().getName(), method.getName()), 0, 0));
        var box = new Box();
        return Rain.of(Drop.of(box.of(stmt), Rain.of()));
    }

    public final boolean keep(Stmt stmt) {
        return isReturn(stmt) || stmt.containsInvokeExpr();
    }

    public final boolean match(Stmt stmt) {
        if (!stmt.containsInvokeExpr()) {
            return false;
        }
        SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
        SootClass declaringClass = methodRef.getDeclaringClass();
        String target = declaringClass.getName() + "." + methodRef.getName();
        for (Pattern pattern : LogPoints.this.tags) {
            if (pattern.matcher(target).find()) {
                return true;
            }
        }
        return false;
    }

    class CFGFactory {
        private final SootMethod method;
        private final ExceptionalUnitGraph cfg;
        private final Box box;
        private final HashMap<Stmt, Rain<Ref>> memo;
        private final PList<SootMethod> strand;
        CFGFactory(Body body, PList<SootMethod> strand) {
            if (!(body instanceof JimpleBody)) {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
            this.method = body.getMethod();
            LogPoints.cpf.apply(body);
            this.cfg = new ExceptionalUnitGraph(body);
            this.box = new Box();
            this.memo = new HashMap<>();
            this.strand = strand;
        }

        public final Stmt tag(Stmt stmt) {
            String sourceName = method.getDeclaringClass().getName() + "." + method.getName();
            stmt.addTag(new SourceMapTag(sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
            return stmt;
        }

        public final Rain<Box.Ref> build() {
            var stmts = PList.from(cfg.getHeads()).map(unit -> (Stmt)unit);
            return build(stmts, PList.of());
        }

        private final Rain<Box.Ref> build(PList<Stmt> stmts, PList<Stmt> strand) {
            return Rain.merge(stmts.map(stmt -> build(stmt, strand)));
        }

        private final Rain<Box.Ref> build(Stmt stmt, PList<Stmt> strand) {
            if (memo.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(method, stmt));
                return memo.get(stmt);
            }

            if (strand.contains(stmt)) {
                logger.trace("{} untied knot", format(method, stmt));
                return Rain.of();
            }

            if (isReturn(stmt)) {
                logger.trace("{} returned", format(method, stmt));
                tag(stmt);
                var rain = Rain.of(Drop.of(box.of(stmt), Rain.of()));
                memo.put(stmt, rain);
                return rain;
            }

            //TODO handle exceptional dests;
            var next = PList.from(cfg.getUnexceptionalSuccsOf(stmt)).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr()) {
                logger.trace("{} skipped", format(method, stmt));
                var rain = build(next, strand.push(stmt));
                rain = process(rain);
                if (!rain.empty()) {
                    memo.put(stmt, rain);
                }
                return rain;
            }

            if (match(stmt)) {
                logger.trace("{} matched", format(method, stmt));
                tag(stmt);
                var nextRain = Rain.bind(Promise.lazy(() -> build(next, PList.of())));
                var rain = Rain.of(Drop.of(box.of(stmt), nextRain));
                memo.put(stmt, rain);
                return rain;
            }

            logger.trace("{} substituing", format(method, stmt));
            var subrain = LogPoints.this.build(stmt, PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt()), this.strand);
            var retsSkipped = Rain.bind(Promise.lazy(() -> build(next, strand.push(stmt))));
            var rets = Rain.bind(Promise.lazy(() -> build(next, PList.of())));

            var rain = Rain.merge(subrain.unfix().map(drop -> {
                var ref = drop.get();
                if (isReturn(ref.get())) {
                    return retsSkipped;
                }

                return Rain.of(Drop.of(box.of(ref), connect(drop.next(), rets)));
            }));
            rain = process(rain);
            if (!rain.empty()) {
                memo.put(stmt, rain);
            }
            return rain;
        }

        private Rain<Box.Ref> connect(Rain<Ref> rain, Rain<Ref> rets) {
            var memo = new HashMap<Ref, Rain<Ref>>();
            return rain.<Rain<Ref>>fold(drops -> Rain.merge(drops.map(drop -> {
                Ref ref = drop.get();
                if (memo.containsKey(ref)) {
                    return memo.get(ref);
                }

                if (isReturn(ref.get())) {
                    memo.put(ref, rets);
                    return rets;
                }
                var out = Rain.of(Drop.of(box.of(ref),  drop.next()));
                memo.put(ref, out);
                return out;
            })));
        }
    }

    static Rain<Box.Ref> process(Rain<Box.Ref> rain) {
        // filter out similar boxes inside a single layer of rain
        // all return stmt are considered similar (thus using the key `null`)
        var seen = new HashSet<Box.Ref>();
        var drops = rain.unfix().filter(drop -> {
            var ref = drop.get();
            if (seen.contains(ref)) {
                return false;
            }
            seen.add(ref);
            return true;
        });
        return Rain.fix(drops);
    }

    static boolean isReturn(Stmt stmt) {
        return stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt;
    }

    private static String format(SootMethod method) {
        return "[" + method.getDeclaringClass().getName() + "." + method.getName() + "]@[" + method.hashCode() + "]";
    }

    private static String format(SootMethod method, Stmt stmt) {
        if (stmt == null) {
            return format(method) + " [null]";
        }
        return format(method) + " [" + stmt.toString() + "]@[" + stmt.hashCode() + "]";
    }
}
