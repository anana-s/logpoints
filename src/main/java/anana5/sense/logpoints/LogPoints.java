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
        return Rain.bind(build().map(rain -> {
            return rain.fold(drops -> Rain.fix(process(drops).filter(v -> Promise.just(!v.get().sentinel()))));
        }));
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

        return build(PList.from(Scene.v().getEntryPoints()), new Path());
    }

    private Promise<Rain<Box<Stmt>.Ref>> build(PList<SootMethod> methods, Path path) {
        // build clinit first
        var cs = methods.filter(method -> Promise.just(method.getName().equals("<clinit>")));
        var ms = methods.filter(method -> Promise.just(!method.getName().equals("<clinit>")));

        return cs.map(method -> build(method, path)).foldr(Rain.<Box<Stmt>.Ref>of(), (p, acc) -> p.map(rain -> Rain.merge(rain, acc))).then(cr -> {
            return ms.map(method -> build(method, path)).foldr(Rain.<Box<Stmt>.Ref>of(), (p, acc) -> p.map(rain -> Rain.merge(rain, acc))).then(mr -> {
                return cr.empty().map(e -> {
                    if (e) {
                        return mr;
                    }

                    return Rain.merge(mr, cr.fold(drops -> Rain.merge(drops.map(drop -> {
                        if (drop.get().sentinel()) {
                            return mr;
                        }
                        return Rain.of(drop);
                    }))));
                });
            });
        });
    }

    private Set<SootMethod> memo0 = new HashSet<>();
    private Map<SootMethod, Rain<Box<Stmt>.Ref>> memo1 = new HashMap<>();

    private Promise<Rain<Box<Stmt>.Ref>> build(SootMethod method, Path path) {
        path = path.push(method);

        if (memo1.containsKey(method)) {
            logger.trace("{} loaded from cache", format(path));
            return Promise.just(memo1.get(method));
        }

        if (memo0.contains(method)) {
            logger.trace("{} untied recursion knot", format(path));
            return Promise.just(Rain.of());
        }

        memo0.add(method);

        var box = path.box();

        if (method.isPhantom()){
            logger.trace("{} skipped due to being phantom", format(path));
            var rain = Rain.of(Drop.of(box.sentinel(), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (!method.isConcrete()) {
            logger.trace("{} skipped due to not being concrete", format(path));
            var rain = Rain.of(Drop.of(box.sentinel(), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        if (method.getDeclaringClass().isLibraryClass()) {
            logger.trace("{} skipped due to being library method", format(path));
            var rain = Rain.of(Drop.of(box.sentinel(), Rain.of()));
            memo1.put(method, rain);
            return Promise.just(rain);
        }

        logger.trace("{} loading", format(path));
        var builder = new CFGFactory(method);
        return builder.build(path).effect(rain -> {
            memo1.put(method, rain);
        });
    }

    class CFGFactory {
        String sourceName;
        SootMethod method;
        Map<Stmt, Rain<Box<Stmt>.Ref>> memo1;
        Set<Stmt> memo0;
        ExceptionalUnitGraph cfg;
        CFGFactory(SootMethod method) {
            var body = method.retrieveActiveBody();
            if (body instanceof JimpleBody) {
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

        public Promise<Rain<Box<Stmt>.Ref>> build(Path path) {
            return build(PList.from(cfg.getHeads()).map(unit -> (Stmt)unit), path).then(rain -> {
                if (logger.isTraceEnabled()) {
                    return Promise.just(rain);
                    // return rain.resolve();
                } else {
                    return Promise.just(rain);
                }
            });
        }

        private Promise<Rain<Box<Stmt>.Ref>> build(PList<Stmt> stmts, Path path) {
            return stmts.map(stmt -> build(stmt, path)).foldr(Rain.<Box<Stmt>.Ref>of(), (p, acc) -> p.map(rain -> Rain.merge(rain, acc)));
        }

        private Promise<Rain<Box<Stmt>.Ref>> build(Stmt stmt, Path path) {
            // if there is no cycle, we can load from cache
            if (memo1.containsKey(stmt)) {
                logger.trace("{} loaded from cache", format(path, stmt));
                return Promise.just(memo1.get(stmt));
            }

            if (memo0.contains(stmt)) {
                logger.trace("{} undid knot", format(path, stmt));
                return Promise.just(Rain.of());
            }

            memo0.add(stmt);

            // if stmt is a return statement
            if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                var box = path.box();
                logger.trace("{} returning", format(path, stmt));
                var rain = Rain.of(Drop.of(box.sentinel(stmt), Rain.of()));
                memo1.put(stmt, rain);
                return Promise.just(rain);
                // stop building
            }

            //TODO handle exceptional dests;

            List<Unit> sucs = cfg.getUnexceptionalSuccsOf(stmt);
            List<Integer> code = sucs.stream().map(Object::hashCode).collect(Collectors.toList());
            PList<Stmt> next = PList.from(sucs).map(unit -> (Stmt)unit);

            if (!stmt.containsInvokeExpr()) {
                // skip this statement
                logger.trace("{} skipped with successors {}", format(path, stmt), code);
                return build(next, path).then(rain -> {
                    return rain.empty().map(e -> {
                        if (e) {
                            memo0.remove(stmt);
                        } else {
                            memo1.put(stmt, rain);
                        }
                        return rain;
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
                    var box = path.box();
                    logger.trace("{} matched with tag {}, continuing with successors {}", format(path, stmt), pattern.toString(), code);
                    stmt.addTag(new SourceMapTag(this.sourceName, stmt.getJavaSourceStartLineNumber(), stmt.getJavaSourceStartColumnNumber()));
                    var succs = Rain.bind(build(next, path));
                    var rain = Rain.of(Drop.of(box.of(stmt), succs));
                    memo1.put(stmt, rain);
                    return Promise.just(rain);
                }
            }

            // get rain of called methods
            final PList<SootMethod> methods = PList.from(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());

            logger.trace("{} expanding with successors {}", format(path, stmt), code);

            var box = path.box();

            // if (knot) {
            //     // break knot
            //     return LogPoints.this.build(methods, path.push(Tuple.of(method, guard))).fmap(subrain -> {
            //         logger.trace("{} undid knot", format(path, method, stmt), code);
            //         return subrain.filter(v -> Promise.just(!v.sentinel())).map(v -> subbox.copy(v));
            //     });
            // }
            return methods.empty().<Rain<Box<Stmt>.Ref>>then(noMethods -> {
                if (noMethods) {
                    logger.trace("{} expands to nothing", format(path, stmt));
                    return build(next, path);
                }
                return LogPoints.this.build(methods, path).map(subrain -> {
                    subrain = subrain.map(v -> box.copy(v, stmt));
                    return subrain.fold(drops -> Rain.merge(drops.map(drop -> {
                        if (drop.get().sentinel()) {
                            logger.trace("{} returned to {}", format(path, stmt), code);
                            return Rain.bind(build(next, path));
                        }
                        return Rain.of(drop);
                    })));
                });
            }).then(rain -> {
                return rain.empty().map(e -> {
                    if (e) {
                        memo0.remove(stmt);
                    } else {
                        memo1.put(stmt, rain);
                    }
                    return rain;
                });
            });
        }
    }

    private PList<Drop<Box<Stmt>.Ref, Rain<Box<Stmt>.Ref>>> process(PList<Drop<Box<Stmt>.Ref, Rain<Box<Stmt>.Ref>>> drops) {
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

    private String format(Path path) {
        var method = path.head();
        return (path.length() - 1) + "::" + method.toString() + "@[" + method.hashCode() + "]";
    }

    private String format(Path path, Stmt stmt) {
        return format(path) + " [" + stmt.toString() + "]@[" + stmt.hashCode() + "]";
    }
}
