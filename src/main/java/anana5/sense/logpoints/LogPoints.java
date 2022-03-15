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
    private Map<SootMethod, Promise<Rain<Box<Stmt>.Ref>>> memo = new HashMap<>();

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
        return Rain.bind(build());
    }

    private Promise<Rain<Box<Stmt>.Ref>> build() {
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

        return build(LList.from(Scene.v().getEntryPoints()), new Path<>()).fmap(rain -> {
            return rain.filter(v -> Promise.just(!v.sentinel()));
        });
    }

    private Promise<Rain<Box<Stmt>.Ref>> build(LList<SootMethod> methods, Path<Tuple<SootMethod, Boolean>> path) {
        // build clinit first
        var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        return cs.map(method -> build(method, path)).foldr(Rain.<Box<Stmt>.Ref>of(), (p, acc) -> p.fmap(rain -> Rain.merge(rain, acc))).then(cr -> {
            return ms.map(method -> build(method, path)).foldr(Rain.<Box<Stmt>.Ref>of(), (p, acc) -> p.fmap(rain -> Rain.merge(rain, acc))).then(mr -> {
                return cr.empty().fmap(e -> {
                    if (e) {
                        return mr;
                    }

                    return cr.fold(drops -> Rain.merge(drops.map(drop -> {
                        if (drop.get().sentinel()) {
                            return mr;
                        }
                        return Rain.of(drop);
                    })));
                });
            });
        });
    }

    private final Box<Stmt> box = new Box<>();

    private Promise<Rain<Box<Stmt>.Ref>> build(SootMethod method, Path<Tuple<SootMethod, Boolean>> path) {
            if (memo.containsKey(method)) {
                // map to new boxes
                logger.trace("{} loaded from cache", format(path, method));
                return memo.get(method);
            }


            if (memo.containsKey(method)) {
                // map to new boxes
                logger.trace("{} loaded from cache", format(path, method));
                return memo.get(method);
            }

            if (method.isPhantom()){
                logger.trace("{} skipped due to being phantom", format(path, method));
                return Promise.just(Rain.of(Drop.of(box.sentinel(), Rain.of())));
            }

            if (!method.isConcrete()) {
                logger.trace("{} skipped due to not being concrete", format(path, method));
                return Promise.just(Rain.of(Drop.of(box.sentinel(), Rain.of())));
            }

            if (method.getDeclaringClass().isLibraryClass()) {
                logger.trace("{} skipped due to being library method", format(path, method));
                return Promise.just(Rain.of(Drop.of(box.sentinel(), Rain.of())));
            }

            logger.trace("{} loading", format(path, method));
            var builder = new CFGFactory(method);
            var promise = builder.build(path);
            memo.put(method, promise);
            return promise;
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Map<Stmt, Promise<Rain<Box<Stmt>.Ref>>> memo0;
        ExceptionalUnitGraph cfg;
        Box<Stmt> box;
        CFGFactory(SootMethod method) {
            var body = method.retrieveActiveBody();
            if (body instanceof JimpleBody) {
                this.method = method;
                this.memo0 = new HashMap<>();
                LogPoints.cpf.apply(body);
                this.cfg = new ExceptionalUnitGraph(body);
                this.sourceName = method.getDeclaringClass().getName() + "." + method.getName();
                this.box = new Box<>();
            } else {
                throw new RuntimeException("unsupported body type " + body.getClass());
            }
        }

        public Promise<Rain<Box<Stmt>.Ref>> build(Path<Tuple<SootMethod, Boolean>> path) {
            return build(LList.from(cfg.getHeads()).map(unit -> (Stmt)unit), path, new Path<>(), false);
        }

        private Promise<Rain<Box<Stmt>.Ref>> build(LList<Stmt> stmts, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            return stmts.map(stmt -> build(stmt, path, subpath, guard).fmap(this::process)).foldr(Rain.of(), (p, acc) -> p.fmap(rain -> Rain.merge(rain, acc)));
        }

        private Promise<Rain<Box<Stmt>.Ref>> build(Stmt stmt, Path<Tuple<SootMethod, Boolean>> path, Path<Tuple<Stmt, Boolean>> subpath, boolean guard) {
            // if there is no cycle, we can load from cache
            boolean knot = knot(stmt, subpath);
            if (!knot && memo0.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(path, method, stmt));
                return memo0.get(stmt);
            }

            // if stmt is a return statement
            if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                logger.trace("{} returning", format(path, method, stmt));
                var promise = Promise.just(Rain.of(Drop.of(box.sentinel(stmt), Rain.of())));
                memo0.put(stmt, promise);
                return promise;
                // stop building
            }

            //TODO handle exceptional dests;

            List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
            List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
            LList<Stmt> next = LList.from(sucs).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr()) {
                // skip this statement
                if (knot) {
                    logger.trace("{} undid knot", format(path, method, stmt), code);
                    return Promise.just(Rain.of());
                }
                logger.trace("{} skipped with successors {}", format(path, method, stmt), code);
                var promise = build(next, path, subpath.push(Tuple.of(stmt, false)), guard);
                return promise.then(rain -> rain.empty().fmap(e -> {
                    if (!e) {
                        memo0.put(stmt, promise);
                    }
                    return rain;
                }));
            }

            // stmt is an invokation, so get call information
            SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
            SootClass declaringClass = methodRef.getDeclaringClass();
            String methodName = declaringClass.getName() + "." + methodRef.getName();

            // check if stmt needs to be kept
            for (Pattern pattern : LogPoints.this.tags) {
                if (pattern.matcher(methodName).find()) {
                    if (knot) {
                        // break knot
                        logger.trace("{} undid knot", format(path, method, stmt), code);
                        return Promise.just(Rain.of(Drop.of(box.of(stmt), Rain.of())));
                    } else {
                        logger.trace("{} matched with tag {}, continuing with successors {}", format(path, method, stmt), pattern.toString(), code);
                        stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
                        var succs = build(next, path, subpath.push(Tuple.of(stmt, true)), true);
                        var promise = succs.fmap(s -> Rain.of(Drop.of(box.of(stmt), s)));
                        memo0.put(stmt, promise);
                        return promise;
                    }
                }
            }

            // get rain of called methods
            final LList<SootMethod> methods = LList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

            // stmt is not kept, so expand the invokation
            if (guard) {
                logger.trace("{} expanding [guarded] with successors {}", format(path, method, stmt), code);
            } else {
                logger.trace("{} expanding [unguarded] with successors {}", format(path, method, stmt), code);
            }

            if (knot) {
                // break knot
                return LogPoints.this.build(methods, path.push(Tuple.of(method, guard))).fmap(subrain -> {
                    logger.trace("{} undid knot", format(path, method, stmt), code);
                    return subrain.filter(v -> Promise.just(!v.sentinel())).map(v -> box.copy(v));
                });
            }
            var promise = methods.empty().<Rain<Box<Stmt>.Ref>>then(noMethods -> {
                if (noMethods) {
                    logger.trace("{} expands to nothing", format(path, method, stmt));
                    return build(next, path, subpath.push(Tuple.of(stmt, false)), guard);
                }
                return LogPoints.this.build(methods, path.push(Tuple.of(method, guard))).fmap(subrain -> {
                    subrain = subrain.map(v -> box.copy(v));
                    Rain<Box<Stmt>.Ref> unguarded = Rain.bind(build(next, path, subpath.push(Tuple.of(stmt, false)), guard));
                    Rain<Box<Stmt>.Ref> guarded = Rain.bind(build(next, path, subpath.push(Tuple.of(stmt, true)), true));
                    return Rain.merge(subrain.unfix().map(drop -> {
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
                });
            });
            return promise.then(rain -> rain.empty().fmap(e -> {
                if (!e) {
                    memo0.put(stmt, promise);
                }
                return rain;
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

    <T> boolean knot(T t, Path<Tuple<T, Boolean>> path) {
        if (path.empty()) {
            return false;
        }

        var tuple = path.head().get();

        if (t.equals(tuple.fst())) {
            return true;
        } else if (tuple.snd()) {
            return false;
        } else {
            return knot(t, path.tail().get());
        }
    }
}
