package anana5.sense.logpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.util.PList;
import anana5.util.Promise;
// import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import soot.Body;
import soot.EntryPoints;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.SootResolver.SootClassNotFoundException;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.tagkit.SourceFileTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Grapher {

    /**
     * config
     */

    private static Logger logger = LoggerFactory.getLogger(Grapher.class);
    private static Grapher instance = null;
    private static Transform cpf = PackManager.v().getPack("jop").get("jop.cpf");
    public static Grapher v() {
        if (instance == null) {
            instance = new Grapher();
        }
        return instance;
    }
    private List<Pattern> tags = new ArrayList<>();
    private List<SootMethod> targets = new ArrayList<>();
    private boolean clinit = true;

    public Grapher configure() {
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
        Options.v().setPhaseOption("cg", "safe-forname:false"); //TODO: set to true
        Options.v().setPhaseOption("cg", "safe-newinstance:false"); //TODO: set to true
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

    public Grapher prepend(boolean should) {
        Options.v().set_prepend_classpath(should);
        return this;
    }

    public Grapher classpath(String classpath) {
        Options.v().set_soot_classpath(classpath);
        return this;
    }

    public Grapher modulepath(String modulepath) {
        Options.v().set_soot_modulepath(modulepath);
        return this;
    }

    public Grapher clinit(boolean should) {
        this.clinit = should;
        return this;
    }

    public class EntrypointNotFoundException extends Exception {
        private final String string;
        public EntrypointNotFoundException(String string) {
            super();
            this.string = string;
        }
        public EntrypointNotFoundException(String string, Throwable cause) {
            super(cause);
            this.string = string;
        }
        public String name() {
            return string;
        }
    }

    public List<SootMethod> entrypoints() {
        List<SootMethod> out = new ArrayList<>(targets);
        if (clinit) {
            out.addAll(EntryPoints.v().clinits().stream().filter(m -> !skip(m)).collect(Collectors.toList()));
        }
        return out;
    }

    public Grapher entrypoint(String cls) throws EntrypointNotFoundException {

        List<String> path = Arrays.asList(cls.split("\\."));
        SootClass sootClass;
        try {
            String className = String.join(".", path.subList(0, path.size() - 1));
            Options.v().classes().add(className);
            sootClass = Scene.v().loadClass(className, SootClass.BODIES);
        } catch (SootClassNotFoundException e) {
            throw new EntrypointNotFoundException(cls);
        }
        sootClass.setApplicationClass();
        // SootMethod m = c.getMethodByName(path.get(path.size() - 1));
        String methodName = path.get(path.size() - 1);
        List<SootMethod> sootMethods;
        if (methodName.equals("*")) {
            sootMethods = sootClass.getMethods();
        } else {
            sootMethods = sootClass.getMethods().stream().filter(n -> n.getName().equals(methodName)).collect(Collectors.toList());
        }
        if (sootMethods.size() == 0) {
            throw new EntrypointNotFoundException(cls);
        }
        targets.addAll(sootMethods);
        return this;
    }

    public Grapher include(List<String> inclusions) {
        Options.v().set_include(inclusions);
        return this;
    }

    public Grapher exclude(List<String> exclusions) {
        Options.v().set_exclude(exclusions);
        return this;
    }

    private Grapher() {
        // make sure to configure soot
        this.configure();
    }

    public Grapher tag(Pattern pattern) {
        tags.add(pattern);
        return this;
    }

    public Grapher tag(int flags, String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern, flags));
        }
        return this;
    }

    public Grapher tag(String... patterns) {
        for (String pattern : patterns) {
            tag(Pattern.compile(pattern));
        }
        return this;
    }

    /**
     * RainGraph builder
     */

    private final Map<SootMethod, Rain<GrapherVertex>> done = new Object2ReferenceOpenHashMap<>();
    private SerializedGraph graph;
    private CallGraph cg;

    public synchronized SerializedGraph get() {
        if (graph == null) {
            graph = new SerializedGraph(build());
        }
        return graph;
    }

    public synchronized void reset() {
        done.clear();
        this.graph = null;
    }

    protected synchronized final Rain<GrapherVertex> build() {
        if (this.cg == null) {
            Scene.v().setEntryPoints(targets);
            Scene.v().loadNecessaryClasses();
            PackManager.v().getPack("cg").apply();
            this.cg = Scene.v().getCallGraph();
        }

        return build(null, PList.from(entrypoints()), new ObjectOpenHashSet<>());
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
        for (Pattern pattern : Grapher.this.tags) {
            if (pattern.matcher(target).find()) {
                return true;
            }
        }
        return false;
    }

    private final Rain<GrapherVertex> build(Stmt invoker, PList<SootMethod> methods, ObjectSet<SootMethod> strand) {
        final var mr = Rain.merge(methods.map(m -> build(invoker, m, strand)));
        return deduplicate(mr);
    }

    private final Rain<GrapherVertex> build(Stmt invoker, SootMethod method, ObjectSet<SootMethod> strand) {
        if (done.containsKey(method)) {
            if (strand.contains(method)) {
                logger.trace("{} added sentinel", format(method));
                return Rain.of(Drop.of(GrapherVertex.of(), done.get(method)));
            } else {
                logger.trace("{} loaded from cache", format(method));
                return done.get(method);
            }
        }

        logger.trace("{} loading", format(method));
        var body = method.retrieveActiveBody();
        var factory = new CFGFactory(body);

        strand = new ObjectOpenHashSet<>(strand);
        strand.add(method);

        var rain = factory.build(strand);
        done.put(method, rain);
        return rain;
    }

    class CFGFactory {
        private final SootMethod method;
        private final ExceptionalUnitGraph cfg;
        private final Map<Stmt, Rain<GrapherVertex>> memo;
        // private final Map<Tuple<Stmt, Ref>, Rain<Ref>> connector;
        private final String methodName;
        private final String sourceFile;
        // private final HashMap<Object, StringBuilder> stringBuilders;
        CFGFactory(Body body) {
            if (!(body instanceof JimpleBody)) {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
            Grapher.cpf.apply(body);
            this.method = body.getMethod();
            this.cfg = new ExceptionalUnitGraph(body);
            this.memo = new Object2ReferenceOpenHashMap<>();
            // this.connector = new Object2ReferenceOpenHashMap<>();
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

        private final Stmt tag(Stmt stmt) {
            stmt.addTag(new SourceMapTag(methodName, sourceFile, stmt.getJavaSourceStartLineNumber()));
            return stmt;
        }

        public final Rain<GrapherVertex> build(ObjectSet<SootMethod> cgstrand) {
            final var stmts = PList.from(cfg.getHeads()).map(unit -> (Stmt)unit);
            final var rain = build(null, stmts, new ObjectOpenHashSet<>(), cgstrand);
            return rain;
        }

        private final Rain<GrapherVertex> build(Stmt pred, PList<Stmt> stmts, ObjectSet<Stmt> cfgstrand, ObjectSet<SootMethod> cgstrand) {
            final var drops = stmts.flatmap(stmt -> build(pred, stmt, cfgstrand, cgstrand).unfix());
            return deduplicate(Rain.fix(drops));
        }

        private final Rain<GrapherVertex> build(Stmt pred, Stmt stmt, ObjectSet<Stmt> cfgstrand, ObjectSet<SootMethod> cgstrand) {
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
                tag(stmt);
                var rain = Rain.of(Drop.of(GrapherVertex.ret(), Rain.of()));
                memo.put(stmt, rain);
                return rain;
            }

            final var next = PList.from(cfg.getSuccsOf(stmt)).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr() || stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName().equals("java.lang.Object")) {
                logger.trace("{} skipped", format(method, stmt));
                cfgstrand.add(stmt);
                var rain = build(stmt, next, cfgstrand, cgstrand);

                return Rain.bind(rain.unfix().resolve().then(nil -> {
                    return rain.unfix().empty().map(empty -> {
                        if (!empty) {
                            memo.put(stmt, rain);
                        }
                        return rain;
                    });
                }));
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
                var nextRain = build(stmt, next, new ObjectOpenHashSet<>(), cgstrand);
                var rain = Rain.of(Drop.of(GrapherVertex.of(stmt), nextRain));
                memo.put(stmt, rain);
                return rain;
            }

            final var methods = PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt()).filter(m -> Promise.<Boolean>just(!(skip(m) || m.getName().equals("<clinit>"))));
            final var count = methods.count().join();

            return Rain.bind(methods.empty().then(methodsEmpty -> {
                if (methodsEmpty) {
                    logger.trace("{} skipping invokation that resolves to nothing", format(method, stmt));
                    cfgstrand.add(stmt);
                    return Promise.just(build(stmt, next, cfgstrand, cgstrand));
                }

                logger.trace("{} substituing with methods {}", format(method, stmt), methods.collect(Collectors.toList()).join());


                if (count >= 100) {
                    logger.warn("{} resolves to {} methods", format(method, stmt), count);
                }

                final var subrain = Grapher.this.build(stmt, methods, cgstrand);


                cfgstrand.add(stmt);
                final var skip = build(stmt, next, cfgstrand, cgstrand);
                final var rets = build(stmt, next, new ObjectOpenHashSet<>(), cgstrand);

                return Promise.lazy(() -> Rain.merge(subrain.unfix().map(drop -> {
                    var ref = drop.get();

                    if (ref.returns()) {
                        return skip;
                    }

                    return Rain.of(Drop.of(ref, connect(drop.next(), rets)));
                })));
            }).then(rain -> {
                return rain.unfix().resolve().then(plist -> {
                    return rain.unfix().empty().map(empty -> {
                        if (!empty) {
                            memo.put(stmt, rain);
                        }
                        return rain;
                    });
                });
            }));
        }
    }

    private static Rain<GrapherVertex> connect(Rain<GrapherVertex> rain, Rain<GrapherVertex> rets) {
        return rain.fold(drops -> Rain.merge(drops.map(drop -> {
            if (drop.get().returns()) {
                return rets;
            }
            return Rain.of(drop);
        })));
    }

    // private static Rain<Vertex> copy(Rain<Vertex> rain) {
    //     Map<Vertex, Drop<Vertex, Rain<Vertex>>> memo = new Object2ObjectOpenHashMap<>();
    //     return rain.fold(drops -> Rain.fix(drops.map(drop -> memo.computeIfAbsent(drop.get(), box -> Drop.of(box.copy(), drop.next())))));
    // }

    // private static Rain<Ref> copy(Box box, Rain<Ref> rain) {
    //     final var memo = new HashMap<Ref, Ref>();
    //     return rain.map(ref -> {
    //         return memo.computeIfAbsent(ref, r -> {
    //             return box.of(r);
    //         });
    //     });
    // }

    /**
     * merge rains
     *
     * @param rain
     * @return
     */
    private static Rain<GrapherVertex> deduplicate(Rain<GrapherVertex> rain) {
        final var drops = rain.unfix();
        return Rain.bind(drops.count().map(count -> {
            if (count == 1) {
                return rain;
            }

            if (count >= 10000) {
                logger.warn("{} drops to deduplicate", count);
            }

            return Rain.fix(deduplicate(count, drops).map(drop -> Drop.of(drop.get(), deduplicate(drop.next()))));
        }));
    }

    /**
     * deduplicates plist of drops with the same stmt
     */

    private static PList<Drop<GrapherVertex, Rain<GrapherVertex>>> deduplicate(long count, PList<Drop<GrapherVertex, Rain<GrapherVertex>>> drops) {
        final var reduced = new Object2ReferenceOpenHashMap<Stmt, List<Rain<GrapherVertex>>>();
        return PList.bind(drops.foldr(Promise.nil(), (promise, drop) -> {
            final var ref = drop.get();
            final var list = reduced.computeIfAbsent(ref.get(), stmt -> new ArrayList<>());
            list.add(drop.next());
            return promise;
        }).map(nil -> {
            if (reduced.size() == count) {
                return drops;
            }
            return PList.from(reduced.entrySet()).map(entry -> {
                final GrapherVertex ref = GrapherVertex.of(entry.getKey());
                final var list = entry.getValue();
                if (list.size() == 1) {
                    return Drop.of(ref, list.get(0));
                }
                return Drop.of(ref, Rain.merge(PList.from(list)));
            });
        }));
    }

    // private static Rain<Ref> deduplicate(Box box, Rain<Ref> rain, Map<HashSet<Ref>, Drop<Ref, Rain<Ref>>> memo) {
    //     final var reduced = new HashMap<Stmt, List<Drop<Ref, Rain<Ref>>>>();
    //     return Rain.bind(rain.unfix().foldr(Promise.nil(), (promise, drop) -> {
    //         final var ref = drop.get();
    //         final var ds = reduced.computeIfAbsent(ref.get(), stmt -> new ArrayList<>());
    //         ds.add(drop);
    //         return promise;
    //     }).map(nothing -> {
    //         return Rain.fix(PList.from(reduced.values()).map(drops -> {
    //             final var drop = drops.get(0);
    //             final var ref = drop.get();
    //             if (drops.size() == 1) {
    //                 final var key = new HashSet<Ref>();
    //                 key.add(ref);
    //                 return memo.computeIfAbsent(key, k -> Drop.of(box.of(ref), drop.next()));
    //             }
    //             final var key = drops.stream().map(Drop::get).collect(Collectors.toCollection(HashSet::new));
    //             return memo.computeIfAbsent(key, k -> Drop.of(box.of(ref), deduplicate(box, Rain.merge(PList.from(drops.stream().map(Drop::next).iterator())), memo)));
    //         }));
    //     }));
    // }

    static boolean isReturn(Stmt stmt) {
        return stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt || stmt instanceof ThrowStmt;
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

    // private final Rain<Box> postprocess(Rain<Box> rain, Map<Ref, Rain<Ref>> memo) {
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
