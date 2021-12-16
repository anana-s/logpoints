package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import anana5.sense.graph.Rainfall;
import anana5.sense.graph.Rainfall.Droplet;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import soot.PackManager;
import soot.Scene;
import soot.SootMethodRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints").build()
            .defaultHelp(true)
            .description("Construct the flow graph of logs");

        parser.addArgument("-a", "--app")
            .action(Arguments.storeTrue());

        parser.addArgument("-p", "--prepend")
            .action(Arguments.storeTrue());

        parser.addArgument("-c", "--classpath");

        parser.addArgument("-x", "--exclude")
            .setDefault(Collections.emptyList())
            .action(Arguments.append());

        parser.addArgument("-t", "--tag")
            .setDefault(Lists.newArrayList("println"))
            .action(Arguments.append());

        parser.addArgument("classes").nargs("*");

        Namespace ns = null;

        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Options.v().set_prepend_classpath(ns.getBoolean("prepend"));
        Options.v().set_soot_classpath(ns.getString("classpath"));
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_fix);
        Options.v().set_app(ns.getBoolean("app"));
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().classes().addAll(ns.getList("classes"));

        List<String> exclusions = new ArrayList<>(ns.getList("exclude"));
        exclusions.add("jdk.*");
        Options.v().set_exclude(exclusions);

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("cg").apply();

        Rainfall<Stmt> flow = new ExecutionFlow(
            Scene.v().getCallGraph(),
            Scene.v().getEntryPoints()
        );

        List<String> queries = ns.getList("tag");
        
        flow = flow.filter(flake -> {
            Stmt s = flake.get();

            if (!s.containsInvokeExpr()) {
                return false;
            }

            SootMethodRef method = s.getInvokeExpr().getMethodRef();

            boolean keep = false;
            String line = method.getDeclaringClass().getName() + "." + method.getName();
            for (String query : queries) {
                if (line.contains(query)) {
                    keep = true;
                }
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
    }

    static String format(Droplet<Stmt, ?>.SnowFlake flake) {
        Stmt s = flake.get();
        return Integer.toHexString(s.hashCode()) + s.getInvokeExpr().getArgs().toString();
    }
}
