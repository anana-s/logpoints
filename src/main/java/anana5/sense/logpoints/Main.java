package anana5.sense.logpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.sense.graph.Computation;
import anana5.sense.graph.Rainfall;
import anana5.sense.graph.Rainfall.Droplet;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import polyglot.ast.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootMethodRef;
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

        Options.v().set_prepend_classpath(ns.getBoolean("prepend"));
        if (ns.getString("classpath") != null) {
            Options.v().set_soot_classpath(ns.getString("classpath"));
        }
        if (ns.getString("modulepath") != null) {
            Options.v().set_soot_modulepath(ns.getString("modulepath"));
        }
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_fix);
        Options.v().set_app(ns.getBoolean("app"));
        Options.v().setPhaseOption("cg.spark", "on");

        List<String> classes = ns.getList("classes");
        Options.v().classes().addAll(classes);
        Options.v().set_main_class(classes.get(classes.size() - 1));

        List<String> exclusions = new ArrayList<>(ns.getList("exclude"));
        exclusions.add("jdk.*");
        Options.v().set_exclude(exclusions);
        Options.v().set_include(ns.getList("include"));

        Scene.v().loadNecessaryClasses();

        LocalDateTime start = LocalDateTime.now();
        logger.info("Started at {}.", start);
        logger.info("Building callgraph.");
        PackManager.v().getPack("cg").apply();

        Rainfall<Stmt> flow = new ExecutionFlow(
            Scene.v().getCallGraph(),
            Scene.v().getEntryPoints()
        );

        List<String> queries = ns.getList("tag");
        
        flow = flow.filter(flake -> {
            Stmt s = flake.get();
            boolean keep = false;

            if (s.containsInvokeExpr()) {
                SootMethodRef method = s.getInvokeExpr().getMethodRef();
                String line = method.getDeclaringClass().getName() + "." + method.getName();
                for (String query : queries) {
                    if (line.contains(query)) {
                        keep = true;
                    }
                }
            }

            if (keep) {
                logger.trace("+ {}", flake);
            } else {
                logger.trace("- {}", flake);
            }

            return keep;
        });

        try (var printer = new DotPrinter(System.out, Main::format)) {
            flow.traverse((s, ts) -> {
                for (var t : ts) {
                    printer.print(s, t);
                }
            }).run();
        }

        LocalDateTime end = LocalDateTime.now();
        logger.info("Done in {} iterations ({}) at {}.", Computation.statistics.iterations(), Duration.between(end, start), end);
    }

    static String format(Droplet<Stmt, ?>.SnowFlake flake) {
        if (flake.get().containsInvokeExpr()) {
            return flake.id() + " " + flake.get().getInvokeExpr().getMethodRef().getDeclaringClass().getName() + "." + flake.get().getInvokeExpr().getMethodRef().getName() + flake.get().getInvokeExpr().getArgs().toString();
        } else {
            return flake.toString();
        }
    }
}
