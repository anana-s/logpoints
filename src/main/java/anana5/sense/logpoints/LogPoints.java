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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.graph.rainfall.RainGraph;
import anana5.sense.logpoints.Box.Ref;
import anana5.util.Computation;
import anana5.util.PList;
import anana5.util.Promise;
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

    private final Map<SootMethod, Rain<Box.Ref>> memo1 = new HashMap<>();
    private RainGraph<Box.Ref> graph;
    private CallGraph cg;

    public final synchronized RainGraph<Box.Ref> graph() {
        if (graph == null) {
            graph = RainGraph.of(Rain.bind(build()));
        }
        return graph;
    }

    protected final Promise<Rain<Box.Ref>> build() {
        LocalDateTime start = LocalDateTime.now();
        logger.debug("started at {} with trace: {}", start, trace);

        if (this.cg == null) {
            Scene.v().loadNecessaryClasses();
            PackManager.v().getPack("cg").apply();
            this.cg = Scene.v().getCallGraph();
        }

        return build(null, PList.from(Scene.v().getEntryPoints()))
        .<Rain<Box.Ref>>then(rain -> {
            return Promise.just(rain.fold(drops -> Rain.fix(drops.filter(drop -> Promise.just(!isReturn(drop.get().get()))))));
        })
        .then(Rain::resolve)
        .effect(rain -> {
            LocalDateTime end = LocalDateTime.now();
            logger.debug("done in {} iterations ({}) at {}", Computation.statistics.iterations(), Duration.between(end, start), end);
            return Promise.<Void>nil();
        });
    }

    private final Promise<Rain<Box.Ref>> build(Stmt invoker, PList<SootMethod> methods) {
        // build clinit first
        var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        var cr = bind(cs.map(m -> build(invoker, m)));
        var mr = bind(ms.map(m -> build(invoker, m)));

        // if (logger.isTraceEnabled()) {
        //     return Rain.bind(cr.resolve().then(resolvedCr -> {
        //         var knot = Knot.tie(a -> connect(resolvedCr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        //         return knot.snd();
        //         // return Promise.just(Rain.merge(mr, connect(resolvedCr, mr)));
        //     }));
        // }

        // var knot = Knot.tie(a -> connect(cr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        // return Rain.bind(knot.snd());

        return mr.then(out -> Promise.just(process(out)));
    }

    private final Promise<Rain<Box.Ref>> build(Stmt invoker, SootMethod method) {
        if (memo1.containsKey(method)) {
            logger.trace("{} loaded from cache", format(method));
            return Promise.just(memo1.get(method));
        }


        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(method));
            var rain = fixture(method);
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(method));
            var rain = fixture(method);
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(method));
            var rain = fixture(method);
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        logger.trace("{} loading", format(method));
        var body = method.retrieveActiveBody();
        var factory = new CFGFactory(body);
        return factory.build().then(rain -> {
            logger.trace("{} substituting", format(method));
            memo1.put(method, rain);
            return factory.substitute(rain);
        }).then(substitued -> {
            logger.trace("{} done", format(method));
            memo1.put(method, substitued);
            return Promise.just(substitued);
        });
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

    private static Promise<Rain<Box.Ref>> bind(PList<Promise<Rain<Box.Ref>>> promises) {
        return promises.foldr(Rain.of(), (rain, promise) -> promise.then(r -> Promise.just(Rain.merge(rain, r))));
    }


    class CFGFactory {
        private SootMethod method;
        private Map<Stmt, Rain<Box.Ref>> memo;
        private ExceptionalUnitGraph cfg;
        private Box box;
        CFGFactory(Body body) {
            if (!(body instanceof JimpleBody)) {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
            this.method = body.getMethod();
            LogPoints.cpf.apply(body);
            this.cfg = new ExceptionalUnitGraph(body);
            this.memo = new HashMap<>();
            this.box = new Box();
        }

        public final Stmt tag(Stmt stmt) {
            String sourceName = method.getDeclaringClass().getName() + "." + method.getName();
            stmt.addTag(new SourceMapTag(sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
            return stmt;
        }

        public final Rain<Box.Ref> fixture() {
            var stmt = tag(new JReturnVoidStmt());
            var ref = box.of(stmt);
            return Rain.of(Drop.of(ref, Rain.of()));
        }

        public final Promise<Rain<Box.Ref>> build() {
            var stmts = PList.from(cfg.getHeads()).map(unit -> (Stmt)unit);
            return build(stmts).then(rain -> {
                rain = filter(rain);
                if (trace) {
                    return rain.resolve();
                }
                return Promise.just(rain);
            });
        }

        private final Promise<Rain<Box.Ref>> build(PList<Stmt> stmts) {
            var promises = stmts.map(stmt -> {
                if (memo.containsKey(stmt)) {
                    return Promise.just(memo.get(stmt));
                }
                //TODO handle exceptional dests;
                var next = build(PList.from(cfg.getUnexceptionalSuccsOf(stmt)).map(unit -> (Stmt)unit));
                var ref = box.of(stmt);
                var rain = Rain.of(Drop.of(ref, Rain.bind(next)));

                memo.put(stmt, rain);

                return Promise.just(rain);
            });
            return promises.foldr(Rain.of(), (acc, promise) -> promise.then(rain -> Promise.just(Rain.merge(acc, rain))));
        }

        private Rain<Box.Ref> filter(Rain<Box.Ref> rain) {
            var seen = new HashSet<Box.Ref>();
            var done = new HashMap<Box.Ref, Rain<Box.Ref>>();
            var promise = rain.<Promise<Rain<Box.Ref>>>fold(drops -> {
                PList<Promise<Rain<Box.Ref>>> promises = drops.map(drop -> {
                    var ref = drop.get();
                    if (done.containsKey(ref)) {
                        logger.trace("{} loaded from cache", format(method, ref.get()));
                        var cached = done.get(ref);
                        return Promise.just(cached);
                    }

                    if (seen.contains(ref)) {
                        logger.trace("{} untied knot", format(method, ref.get()));
                        return Promise.just(Rain.of());
                    }

                    if (keep(ref.get())) {
                        logger.trace("{} kept", format(method, ref.get()));
                        seen.remove(ref);
                        var next = Rain.bind(drop.next());
                        var out = Rain.of(Drop.of(ref, next));
                        done.put(ref, out);
                        return Promise.just(out);
                    }

                    seen.add(ref);

                    logger.trace("{} skipped", format(method, ref.get()));
                    return drop.next().then(out -> {
                        seen.remove(ref);
                        return out.empty().then(e -> {
                            if (!e) {
                                done.put(ref, out);
                            }
                            return Promise.just(out);
                        });
                    });
                });

                return bind(promises).then(out -> Promise.just(process(out)));
            });
            return Rain.bind(promise);
        }

        private final Promise<Rain<Box.Ref>> substitute(Rain<Box.Ref> rain) {
            var seen = new HashSet<Box.Ref>();
            var done = new HashMap<Box.Ref, Rain<Box.Ref>>();
            Promise<Rain<Box.Ref>> promise = rain.fold(drops -> {
                var promises = drops.<Promise<Rain<Ref>>>map(drop -> {
                    var ref = drop.get();

                    if (done.containsKey(ref)) {
                        logger.trace("{} loaded from cache", format(method, ref.get()));
                        return Promise.just(done.get(ref));
                    }

                    if (seen.contains(ref)) {
                        logger.trace("{} untied knot", format(method, ref.get()));
                        return Promise.just(Rain.of());
                    }

                    Stmt stmt = ref.get();
                    var next = Rain.bind(drop.next());

                    if (isReturn(stmt) || match(stmt)) {
                        logger.trace("{} kept", format(method, ref.get()));
                        tag(stmt);
                        var out = Rain.of(Drop.of(ref, next));
                        done.put(ref, out);
                        return Promise.just(out);
                    }

                    seen.add(ref);

                    logger.trace("{} substituting", format(method, ref.get()));
                    return LogPoints.this.build(stmt, PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt())).then(subrain -> {
                        return connect(subrain, next);
                    }).then(subrain -> {
                        return subrain.empty().then(e -> {
                            seen.remove(ref);
                            if (!e) {
                                done.put(ref, subrain);
                            }
                            return Promise.just(subrain);
                        });
                    });
                });
                return bind(promises);
            });
            if (trace) {
                return promise.then(r -> r.resolve());
            }
            return promise;
        }

        private Promise<Rain<Box.Ref>> connect(Rain<Ref> rain, Rain<Ref> rets) {
            var memo = new HashMap<Ref, Rain<Ref>>();
            return rain.fold(drops -> bind(drops.map(drop -> {
                Ref ref = drop.get();
                if (memo.containsKey(ref)) {
                    return Promise.just(memo.get(ref));
                }

                if (isReturn(ref.get())) {
                    memo.put(ref, rets);
                    return Promise.just(rets);
                }
                var out = Rain.of(Drop.of(box.of(ref), Rain.bind(drop.next())));
                memo.put(ref, out);
                return Promise.just(out);
            })));
        }
    }

    private final Rain<Box.Ref> process(Rain<Box.Ref> rain) {
        // filter out similar boxes inside a single layer of rain
        // all return stmt are considered similar (thus using the key `null`)
        var seen = new HashSet<Box.Ref>();
        var drops = rain.unfix().filter(drop -> {
            var ref = drop.get();
            if (seen.contains(ref)) {
                return Promise.just(false);
            }
            seen.add(ref);
            return Promise.just(true);
        });
        return Rain.fix(drops);
    }

    private static boolean isReturn(Stmt stmt) {
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
