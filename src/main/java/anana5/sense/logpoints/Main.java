package anana5.sense.logpoints;

import java.util.Arrays;

import anana5.sense.graph.Rain;
import soot.PackManager;
import soot.Scene;
import soot.Unit;
import soot.jimple.Stmt;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        Options.v().set_exclude(Arrays.asList("jdk.*"));
        Options.v().parse(args);
        
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_fix);
        Options.v().set_app(true);

        if (!Options.v().no_bodies_for_excluded()) {
            Options.v().setPhaseOption("cg.spark", "on");
        }

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("cg").apply();

        Rain<Unit> flow = new ExecutionFlow(
            Scene.v().getCallGraph(),
            Scene.v().getEntryPoints()
        );
        
        flow = flow.filter(unit -> unit.toString().contains("println"));

        try (var printer = new DotPrinter<>(System.out)) {
            flow.traverse((u, vs) -> {
                for (var v : vs) {
                    printer.print(((Stmt)u.get()).getInvokeExpr().getArgs(), ((Stmt)v.get()).getInvokeExpr().getArgs());
                }
            }).run();
        }
    }
}
