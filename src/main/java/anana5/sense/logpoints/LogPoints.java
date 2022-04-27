package anana5.sense.logpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.sense.logpoints.Box.Ref;
import anana5.util.ListF;
import anana5.util.PList;
import anana5.util.Promise;
import anana5.util.Tuple;
import soot.Body;
import soot.EntryPoints;
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
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.tagkit.SourceFileTag;
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
    private List<SootMethod> targets = new ArrayList<>();
    private boolean clinit = true;

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
        Options.v().setPhaseOption("cg", "safe-forname:false");
        Options.v().setPhaseOption("cg", "safe-newinstance:false");
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

    public LogPoints clinit(boolean should) {
        this.clinit = should;
        return this;
    }

    public class EntrypointNotFoundException extends Exception {
        private final String string;
        public EntrypointNotFoundException(String string, Throwable cause) {
            super(cause);
            this.string = string;
        }
        public String name() {
            return string;
        }
    }

    public PList<SootMethod> entrypoints() {
        PList<SootMethod> out;
        if (clinit) {
            out = PList.from(targets).concat(PList.from(EntryPoints.v().clinits()));
        } else {
            out = PList.from(targets);
        }
        return out.filter(m -> Promise.just(!skip(m)));
    }

    public LogPoints entrypoint(String cls) throws EntrypointNotFoundException {
        try {
            List<String> path = Arrays.asList(cls.split("\\."));
            SootClass c = Scene.v().forceResolve(String.join(".", path.subList(0, path.size() - 1)), SootClass.BODIES);
            c.setApplicationClass();
            // SootMethod m = c.getMethodByName(path.get(path.size() - 1));
            SootMethod m = c.getMethods().stream().filter(n -> n.getName().equals(path.get(path.size() - 1))).findFirst().get();
            targets.add(m);
        } catch (RuntimeException e) {
            throw new EntrypointNotFoundException(cls, e);
        }
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

    private synchronized void setGraph(SerialRefRainGraph graph) {
        this.graph = graph;
    }

    public synchronized SerialRefRainGraph graph() {
        if (graph == null) {
            setGraph(new SerialRefRainGraph(build()));
        }
        return graph;
    }

    public void reset() {
        done.clear();
        setGraph(null);
    }

    protected synchronized final Rain<Box.Ref> build() {
        if (this.cg == null) {
            Scene.v().setEntryPoints(targets);
            Scene.v().loadNecessaryClasses();
            PackManager.v().getPack("cg").apply();
            this.cg = Scene.v().getCallGraph();
        }

        return deduplicate(new Box(null), build(null, entrypoints(), new HashSet<>()));
    }

    // // methods to be skipped
    // static List<String> names = Arrays.asList(
    //     "hashCode",
    //     "equals",
    //     "toString",
    //     "clone",
    //     "getClass"
    // );
    static boolean skip(SootMethod method) {
        return !method.isConcrete()
            || method.getDeclaringClass().isLibraryClass()
            // || names.contains(method.getName())
            ;
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

    private final Rain<Box.Ref> build(Stmt invoker, PList<SootMethod> methods, Set<SootMethod> strand) {
        var mr = Rain.merge(methods.map(m -> build(invoker, m, strand)));
        // return deduplicate(mr); // can not deduplicate because of loops
        return mr;
    }

    private final Rain<Box.Ref> build(Stmt invoker, SootMethod method, Set<SootMethod> strand) {
        if (done.containsKey(method)) {
            if (strand.contains(method)) {
                logger.trace("{} added sentinel", format(method));
                return Rain.of(Drop.of(Box.sentinel(invoker, true), done.get(method)));
            } else {
                logger.trace("{} loaded from cache", format(method));
                return done.get(method);
            }
        }

        logger.trace("{} loading", format(method));
        var body = method.retrieveActiveBody();
        var factory = new CFGFactory(body);

        strand = new HashSet<>(strand);
        strand.add(method);

        var rain = factory.build(strand);
        done.put(method, rain);
        return rain;
    }

    class CFGFactory {
        private final SootMethod method;
        private final ExceptionalUnitGraph cfg;
        private final Box box;
        private final HashMap<Stmt, Rain<Ref>> memo;
        private final String methodName;
        private final String sourceFile;
        // private final HashMap<Object, StringBuilder> stringBuilders;
        CFGFactory(Body body) {
            if (!(body instanceof JimpleBody)) {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
            LogPoints.cpf.apply(body);
            this.method = body.getMethod();
            this.cfg = new ExceptionalUnitGraph(body);
            this.box = new Box(this.method);
            this.memo = new HashMap<>();
            // this.stringBuilders = new HashMap<>();

            this.methodName = method.getDeclaringClass().getName() + "." + method.getName();
            SourceFileTag tag = (SourceFileTag)method.getDeclaringClass().getTag("SourceFileTag");
            if (tag == null) {
                logger.warn("{} has no source map", format(method));
                this.sourceFile = "";
            } else {
                this.sourceFile = tag.getSourceFile();
            }
        }

        public final Stmt tag(Stmt stmt) {
            stmt.addTag(new SourceMapTag(methodName, sourceFile, stmt.getJavaSourceStartLineNumber()));
            return stmt;
        }

        public final Rain<Box.Ref> build(Set<SootMethod> cgstrand) {
            var stmts = PList.from(cfg.getHeads()).map(unit -> (Stmt)unit);
            return build(stmts, new HashSet<>(), cgstrand);
        }

        private final Rain<Box.Ref> build(PList<Stmt> stmts, Set<Stmt> cfgstrand, Set<SootMethod> cgstrand) {
            return Rain.fix(PList.bind(deduplicate(stmts.flatmap(stmt -> build(stmt, cfgstrand, cgstrand).unfix())).resolve()));
        }

        private final Rain<Box.Ref> build(Stmt stmt, Set<Stmt> cfgstrand, Set<SootMethod> cgstrand) {
            if (memo.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(method, stmt));
                return memo.get(stmt);
            }

            if (cfgstrand.contains(stmt)) {
                logger.trace("{} untied knot", format(method, stmt));
                return Rain.of();
            }

            if (isReturn(stmt)) {
                logger.trace("{} returned", format(method, stmt));
                var rain = Rain.of(Drop.of(Box.returns(), Rain.of()));
                memo.put(stmt, rain);
                return rain;
            }

            //TODO handle exceptional dests;
            var next = PList.from(cfg.getSuccsOf(stmt)).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr() || stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName().equals("java.lang.Object")) {
                logger.trace("{} skipped", format(method, stmt));
                cfgstrand.add(stmt);
                var promise = Promise.lazy(() -> build(next, cfgstrand, cgstrand)).then(rain -> {
                    return rain.empty().map(rainEmpty -> {
                        if (!rainEmpty) {
                            cfgstrand.remove(stmt);
                            memo.put(stmt, rain);
                        }
                        return rain;
                    });
                });
                return Rain.bind(promise);
            }

            //TODO
            // // aggregate string builders
            // var ie = stmt.getInvokeExpr();
            // var mr = ie.getMethodRef();
            // if (mr.getDeclaringClass().getName().equals("java.lang.StringBuilder")) {
            //     if (
            //         mr.getName().equals("<init>")
            //     ) {
            //         assert stmt instanceof JAssignStmt;
            //         var as = (JAssignStmt)stmt;
            //         var variable = as.leftBox.getValue();
            //         Class<?> asdf = JimpleLocal.class;
            //         if (ie.getArgCount() == 0) {
            //             stringBuilders.put(variable, new StringBuilder());
            //         }
            //     } else if (
            //         mr.getName().equals("append")
            //     ) {
            //     }
            // }

            if (match(stmt)) {
                logger.trace("{} matched", format(method, stmt));
                tag(stmt);
                var nextRain = Rain.bind(Promise.lazy(() -> build(next, new HashSet<>(), new HashSet<>())));
                var rain = Rain.of(Drop.of(box.of(stmt), nextRain));
                memo.put(stmt, rain);
                return rain;
            }

            var methods = PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt()).filter(m -> Promise.<Boolean>just(!(skip(m) || m.getName().equals("<clinit>"))));

            return Rain.<Ref>bind(methods.empty().then(methodsEmpty -> {
                if (methodsEmpty) {
                    logger.trace("{} skipping invokation that resolves to nothing", format(method, stmt));
                    cfgstrand.add(stmt);
                    return Promise.lazy(() -> build(next, cfgstrand, cgstrand)).then(rain -> {
                        return rain.empty().map(rainEmpty -> {
                            if (!rainEmpty) {
                                cfgstrand.remove(stmt);
                                memo.put(stmt, rain);
                            }
                            return rain;
                        });
                    });
                }

                logger.trace("{} substituing with methods {}", format(method, stmt), methods.collect(Collectors.toList()).join());
                var subrain = LogPoints.this.build(stmt, methods, cgstrand);
                subrain = copy(box, subrain);
                subrain = deduplicate(box, subrain);

                cfgstrand.add(stmt);
                var retsSkipped = Rain.bind(Promise.lazy(() -> build(next, cfgstrand, cgstrand)));

                var rets = Rain.bind(Promise.lazy(() -> build(next, new HashSet<>(), new HashSet<>())));

                var rain = Rain.<Ref>merge(subrain.unfix().map(drop -> {
                    var ref = drop.get();

                    if (ref.returns()) {
                        return retsSkipped;
                    }

                    return Rain.of(Drop.of(ref, connect(drop.next(), rets)));
                }));
                return rain.empty().map(rainEmpty -> {
                    if (!rainEmpty) {
                        cfgstrand.remove(stmt);
                        this.memo.put(stmt, rain);
                    }
                    return rain;
                });
            }));
        }
    }

    private static Rain<Ref> copy(Box box, Rain<Ref> rain) {
        final var memo = new HashMap<Ref, Ref>();
        return rain.map(ref -> {
            return memo.computeIfAbsent(ref, r -> {
                return box.of(r);
            });
        });
    }

    /**
     * merge rains
     *
     * @param rain
     * @return
     */
    private static Rain<Ref> deduplicate(Box box, Rain<Ref> rain) {
        final var memo = new LinkedHashMap<Stmt, Tuple<Ref, List<Rain<Ref>>>>();
        return Rain.bind(rain.unfix().foldr(Promise.nil(), (p, drop) -> {
            var ref = drop.get();
            memo.compute(ref.get(), (stmt, t) -> {
                if (t == null) {
                    final List<Rain<Ref>> rains = new ArrayList<>();
                    rains.add(drop.next());
                    return Tuple.of(ref, rains);
                }

                t.snd().add(drop.next());
                return t;
            });
            return p;
        }).map(nothing -> {
            return Rain.fix(PList.from(memo.values()).map(t -> {
                var rains = t.snd();
                if (rains.size() <= 1) {
                    var next = deduplicate(box, rains.get(0));
                    return Drop.of(t.fst(), next);
                } else {
                    var next = deduplicate(box, Rain.merge(PList.from(rains)));
                    return Drop.of(box.of(t.fst()), next);
                }
            }));
        }));
    }

    /**
     * deduplicates plist of drops with the same stmt
     */

    private static PList<Drop<Ref, Rain<Ref>>> deduplicate(PList<Drop<Ref, Rain<Ref>>> drops) {
        final var memo = new LinkedHashSet<Ref>();
        return PList.unfold(drops, ds -> {
            return ds.unfix().then(listF -> listF.match(() -> Promise.just(listF), (d, n) -> {
                var ref = d.get();
                if (memo.contains(ref)) {
                    return n.unfix();
                }
                memo.add(ref);
                return Promise.just(ListF.cons(d, n));
            }));
        });
    }

    private static Rain<Ref> connect(Rain<Ref> rain, Rain<Ref> rets) {
        return rain.fold(drops -> Rain.merge(drops.map(drop -> {
            if (drop.get().returns()) {
                return rets;
            }
            return Rain.of(drop);
        })));
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

    // private Rain<Ref> resolve(Rain<Ref> rain) {
    //     var seen = new HashSet<Ref>();
    //     var promise = rain.<Promise<Void>>fold(drops -> drops.foldl(Promise.<Void>nil(), (drop, acc) -> {
    //         var ref = drop.get();
    //         if (ref.recursive() || seen.contains(ref)) {
    //             return acc;
    //         }
    //         seen.add(ref);
    //         return drop.next().then(n -> acc);
    //     }));

    //     return Rain.bind(promise.map(n -> rain));
    // }

    // protected static Promise<Rain<Ref>> resolve(Rain<Ref> rain) {
    //     return resolve(rain, new HashSet<>()).map(v -> rain);
    // }

    // protected static Promise<Void> resolve(Rain<Ref> rain, Set<Ref> memo) {
    //     return rain.unfix().<Promise<Void>>map(drop -> {
    //         if (drop.get().sentinel() || memo.contains(drop.get())) {
    //             return Promise.nil();
    //         }

    //         return resolve(drop.next(), memo);
    //     }).foldl(Promise.nil(), (promise, resolved) -> {
    //         return promise.then(u -> {
    //             return resolved.then(v -> {
    //                 return Promise.nil();
    //             });
    //         });
    //     });
    // }

    // static Promise<Rain<Ref>> merge(PList<Promise<Rain<Ref>>> promises) {
    //     return promises.foldl(Promise.just(PList.<Drop<Ref, Rain<Ref>>>of()), (promise, acc) -> {
    //         return promise.then(u -> {
    //             return acc.then(v -> {
    //                 return Promise.just(u.unfix().concat(v));
    //             });
    //         });
    //     }).map(Rain::fix);
    // }

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
}
