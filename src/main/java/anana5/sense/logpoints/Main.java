package anana5.sense.logpoints;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.Computation;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import soot.PackManager;
import soot.Scene;
import soot.jimple.Stmt;
import soot.options.Options;

public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints").build()
            .defaultHelp(true)
            .description("Construct the flow graph of logs");

        parser.addArgument("-a", "--app")
            .action(Arguments.storeTrue());

        parser.addArgument("-p", "--prepend")
            .action(Arguments.storeTrue());

        parser.addArgument("-c", "--classpath");
        
        parser.addArgument("-m", "--modulepath");

        parser.addArgument("-i", "--include")
            .action(Arguments.append());

        parser.addArgument("-x", "--exclude")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("-t", "--tag")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("-o", "--output")
            .nargs("?")
            .type(PrintStream.class)
            .setDefault(System.out);

        parser.addArgument("classes").nargs("+");


        Namespace ns;

        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return;
        }

        try {
            Options.v().set_output_dir(Files.createTempDirectory("soot").toString());
        } catch (IOException e) {
            logger.error("Failed to create temporary directory");
            System.exit(2);
            return;
        }

        // classpath and modulepath
        Options.v().set_prepend_classpath(ns.getBoolean("prepend"));
        if (ns.getString("classpath") != null) {
            Options.v().set_soot_classpath(ns.getString("classpath"));
        }
        if (ns.getString("modulepath") != null) {
            Options.v().set_soot_modulepath(ns.getString("modulepath"));
        }

        // application options
        Options.v().set_app(ns.getBoolean("app"));
        List<String> exclusions = new ArrayList<>(ns.getList("exclude"));
        exclusions.add("jdk.*");
        Options.v().set_exclude(exclusions);
        Options.v().set_include(ns.getList("include"));

        // cg options
        Options.v().setPhaseOption("cg.spark", "enabled:true");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        Options.v().setPhaseOption("cg", "safe-forname:false");
        Options.v().setPhaseOption("cg", "safe-newinstance:false");
        Options.v().setPhaseOption("cg", "jdkver:11");
        Options.v().setPhaseOption("cg", "verbose:false");

        // trim cfgs
        Options.v().set_throw_analysis(Options.throw_analysis_unit);
        Options.v().set_omit_excepting_unit_edges(true);
        Options.v().setPhaseOption("jb.tt", "enabled:true");

        // other
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_fix);

        List<String> classes = ns.getList("classes");
        Options.v().classes().addAll(classes);
        Options.v().set_main_class(classes.get(classes.size() - 1));

        LocalDateTime start = LocalDateTime.now();
        logger.info("Started at {}.", start);
        Runnable exitHook = () -> {
            LocalDateTime end = LocalDateTime.now();
            logger.info("Done in {} iterations ({}) at {}.", Computation.statistics.iterations(), Duration.between(end, start), end);
        };
        Runtime.getRuntime().addShutdownHook(new Thread(exitHook, "exit"));

        // load classes
        Scene.v().loadNecessaryClasses();

        // construct cg
        PackManager.v().getPack("cg").apply();

        // construct graph
        List<String> tags = ns.getList("tag");
        GraphFactory factory = new GraphFactory(Scene.v().getCallGraph());
        for (String tag : tags) {
            factory.tag(tag);
        }
        Graph<Stmt> graph = factory.build(Scene.v().getEntryPoints());

        // traverse graph
        try (var printer = new DotPrinter(ns.get("output"), Main::format)) {
            graph.traverse((src, tgt) -> {
                printer.print(src, tgt);
            });
        }
    }

    static String format(Vertex<Stmt> vertex) {
        if (vertex.value() == null) {
            return "[root]";
        }

        if (vertex.value().containsInvokeExpr()) {
            return "[" + vertex.hashCode() + "] " + vertex.value().getInvokeExpr().getMethodRef().getName() + vertex.value().getInvokeExpr().getArgs().toString();
        } else {
            return vertex.toString();
        }
    }
}
