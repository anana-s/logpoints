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
    private static boolean trace = false;
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
        this.trace(ns.getBoolean("trace"));
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

    public LogPoints trace(boolean trace) {
        LogPoints.trace = trace;
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

    public Rain<Box.Ref> graph() {
        return Rain.bind(build());
    }

    private final Promise<Rain<Box.Ref>> build() {
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

        return build(null, PList.from(Scene.v().getEntryPoints()), new Path()).then(rain -> {
            return Promise.just(Rain.fix(rain.unfix().filter(drop -> Promise.just(!isReturn(drop.get().value())))));
        });
    }

    private final Promise<Rain<Box.Ref>> build(Stmt invoker, PList<SootMethod> methods, Path path) {
        // build clinit first
        // var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        // var cr = Rain.merge(cs.map(m -> build(invoker, m, path)));
        var mr = Rain.merge(ms.map(m -> Rain.bind(build(invoker, m, path))));

        // if (logger.isTraceEnabled()) {
        //     return Rain.bind(cr.resolve().then(resolvedCr -> {
        //         var knot = Knot.tie(a -> connect(resolvedCr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        //         return knot.snd();
        //         // return Promise.just(Rain.merge(mr, connect(resolvedCr, mr)));
        //     }));
        // }

        // var knot = Knot.tie(a -> connect(cr, Rain.bind(a)), b -> Rain.merge(mr, Rain.bind(b)));
        // return Rain.bind(knot.snd());

        return Promise.just(process(mr));
    }

    private final Map<SootMethod, Rain<Box.Ref>> memo1 = new HashMap<>();
    private final Set<SootMethod> recur = new HashSet<>();
    private final Promise<Rain<Box.Ref>> build(Stmt invoker, SootMethod method, Path path) {

        var curr = path.push(invoker, method);
        var box = curr.box();

        if (memo1.containsKey(method)) {
            logger.trace("{} loaded from cache", format(path, method));
            return Promise.just(memo1.get(method).map(v -> box.of(v)));
        }

        if (path.contains(invoker, method)) {
            logger.trace("{} untied recursion knot", format(path, method));
            return Promise.just(Rain.of());
        }


        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(path, method));
            var rain = Rain.of(Drop.of(box.of(true, new JReturnVoidStmt()), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        logger.trace("{} loading", format(path, method));
        var builder = new CFGFactory(curr, method);
        return builder.build().then(rain -> {
            logger.trace("{} done loading", format(path, method));

            // trade some performance for better memory usage and readability of the traces
            rain = Rain.bind(rain.resolve());
            memo1.put(method, rain);

            return Promise.just(rain);
        });
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Set<Stmt> memo0;
        Map<Stmt, Rain<Box.Ref>> memo1;
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

        public final Promise<Rain<Box.Ref>> build() {
            return build(PList.from(cfg.getHeads()).map(unit -> (Stmt)unit));
        }

        private final Promise<Rain<Box.Ref>> build(PList<Stmt> stmts) {
            return stmts.map(stmt -> build(stmt)).foldr(Rain.of(), (p, rain) -> p.then(r -> Promise.just(Rain.merge(rain, r))));
        }

        private final Promise<Rain<Box.Ref>> build(Stmt stmt) {
            // if there is no cycle, we can load from cache
            if (memo1.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(path, method, stmt));
                return Promise.just(memo1.get(stmt));
            }

            if (memo0.contains(stmt)) {
                logger.trace("{} untied knot", format(path, method, stmt));
                return Promise.just(Rain.of());
            }

            memo0.add(stmt);

            //TODO handle exceptional dests;

            List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
            List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
            PList<Stmt> next = PList.from(sucs).map(unit -> (Stmt)unit);

            if (isReturn(stmt)) {
                logger.trace("{} returned", format(path, method, stmt), code);
                final var rain = Rain.of(Drop.of(path.box().of(stmt), Rain.of()));
                memo1.put(stmt, rain);
                return Promise.just(rain);
            }

            if (!stmt.containsInvokeExpr()) {
                // skip this statement
                logger.trace("{} skipped with successors {}", format(path, method, stmt), code);
                return build(next).then(rain -> {
                    var processed = process(rain);
                    return rain.empty().then(e -> {
                        memo0.remove(stmt);
                        if (!e) {
                            memo1.put(stmt, processed);
                        }
                        return Promise.just(processed);
                    });
                });
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
                    final var rain = Rain.of(Drop.of(path.box().of(stmt), Rain.bind(build(next))));
                    memo1.put(stmt, rain);
                    return Promise.just(rain);
                }
            }

            // get rain of called methods
            final PList<SootMethod> methods = PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

            logger.trace("{} expanding with successors {}", format(path, method, stmt), code);

            return methods.empty().<Rain<Box.Ref>>then(noMethods -> {
                if (noMethods) {
                    logger.trace("{} expands to nothing", format(path, method, stmt));
                    return build(next);
                }

                return LogPoints.this.build(stmt, methods, path).then(subrain -> {
                    Rain<Box.Ref> a, b;
                    a = Rain.bind(Promise.lazy().then(n -> build(next)));
                    b = Rain.bind(Promise.lazy().then(n -> build(next)));

                    var done = new HashMap<Box.Ref, Rain<Box.Ref>>();
                    var connected = subrain.unfix().map(drop_ -> {
                        Stmt s_ = drop_.get().value();
                        if (isReturn(s_)) {
                            logger.trace("{} returning from [{}]@[{}] to {} [unguarded]", format(path, method, stmt), s_, s_.hashCode(), code);
                            return a;
                        }
                        return Rain.of(Drop.of(drop_.get(), drop_.next().<Rain<Box.Ref>>fold(drops -> Rain.merge(drops.map(drop -> {
                            var box = drop.get();
                            var s = box.value();

                            if (done.containsKey(box)) {
                                return done.get(box);
                            } else if (isReturn(s)) {
                                logger.trace("{} returning from [{}]@[{}] to {} [guarded]", format(path, method, stmt), s, s.hashCode(), code);
                                done.put(box, b);
                                return b;
                            } else {
                                var out = Rain.of(drop);
                                done.put(box, out);
                                return out;
                            }
                        })))));
                    });
                    return Promise.just(Rain.merge(connected));
                });
            }).then(rain -> {
                var processed = process(rain);
                return rain.empty().then(e -> {
                    memo0.remove(stmt);
                    if (!e) {
                        memo1.put(stmt, processed);
                    }
                    return Promise.just(processed);
                });
            });
        }
    }

    private static Rain<Box.Ref> process(Rain<Box.Ref> rain) {
        // filter out similar boxes inside a single layer of rain
        // all return stmt are considered similar (thus using the key `null`)
        var seen = new HashSet<>();
        var drops = rain.unfix().filter(drop -> {
            var stmt = drop.get().value();
            if (isReturn(stmt)) {
                stmt = null;
            }
            if (seen.contains(stmt)) {
                return Promise.just(false);
            }
            seen.add(stmt);
            return Promise.just(true);
        });
        return Rain.fix(drops);
    }

    private static boolean isReturn(Stmt stmt) {
        return stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt;
    }

    private static Rain<Box.Ref> connect(Rain<Box.Ref> rain, Rain<Box.Ref> rets) {
        return rain.fold(drops -> Rain.merge(drops.map(drop -> {
            Stmt s = drop.get().value();
            if (isReturn(s)) {
                return rets;
            }
            return Rain.of(drop);
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

    // private static Rain<Box.Ref> filter(Rain<Box.Ref> rain) {

    //     var seen = new HashSet<Box.Ref>();
    //     var done = new HashMap<Box.Ref, Rain<Box.Ref>>();
    //     return Rain.bind(rain.fold(drops -> {
    //         PList<Promise<Rain<Box.Ref>>> promises = drops.map(drop -> {
    //             final Box.Ref v = drop.get();
    //             if (done.containsKey(v)) {
    //                 logger.trace("filtering {} loaded from cache", v.hashCode());
    //                 return Promise.just(done.get(v));
    //             } else if (v.sentinel()) {
    //                 if (seen.contains(v)) {
    //                     logger.trace("filtering {} untied knot", v.hashCode());
    //                     return Promise.just(Rain.of());
    //                 }
    //                 logger.trace("filtering {} skipped", v.hashCode());
    //                 seen.add(v);
    //                 return drop.next().then(next -> {
    //                     seen.remove(v);
    //                     return next.empty().then(e -> {
    //                         if (!e) {
    //                             done.put(v, next);
    //                         }
    //                         return Promise.just(next);
    //                     });
    //                 });
    //             } else {
    //                 logger.trace("filtering {} kept", v.hashCode());
    //                 Rain<Box.Ref> rain_ = Rain.of(Drop.of(drop.get(), Rain.bind(drop.next())));
    //                 done.put(v, rain_);
    //                 return Promise.just(rain_);
    //             }
    //         });

    //         Promise<Rain<Box.Ref>> out = promises.foldr(Rain.of(), (promise, rain_) -> promise.then(r -> Promise.just(Rain.merge(rain_, r))));
    //         return out.map(rain_ -> Rain.fix(process(rain_.unfix())));
    //     }));
    // }
}
