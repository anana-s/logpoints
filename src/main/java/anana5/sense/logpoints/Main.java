package anana5.sense.logpoints;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class Main {
    

    public static class ICFGTransformer extends SceneTransformer {
        final Logger logger = LoggerFactory.getLogger(ICFGTransformer.class);

        @Override
        protected void internalTransform(String phaseName, Map<String, String> options) {

            CallGraph callgraph = Scene.v().getCallGraph();

            List<SootMethod> entrypoints = Scene.v().getEntryPoints();

            ExecutionFlowJungle flow = new ExecutionFlowJungle(callgraph, entrypoints);

            logger.info("Flow {} built.", flow);

            File dir = new File(Options.v().output_dir());
            dir.mkdirs();

            var counter = new Object() {
                int value = 0;
            };

            try (PrintStream out = new PrintStream(new File(dir, Scene.v().getMainClass().getName() + ".dot"))) {
                try (GraphVizPrinter printer = new GraphVizPrinter(System.out)) {
                    flow.traverse((u, successors) -> {
                        if (counter.value++ > 100) {
                            throw new RuntimeException();
                        }
                        for (Object successor : successors) {
                            printer.print(u, successor);
                        }
                    }).run();
                }
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            }

            // entrypoints.removeIf(m -> !EFGraph.mfilter(m));


            // D<?> defered = ((D<EFGraph>)() -> new EFGraph(cg, entrypoints))
            //     .bind(g -> g.filter(ref -> ref instanceof InvokeStmt && ref.toString().contains("println")))
            //     .bind(g -> g.traverse(u -> {
            //         u.scs.map(n -> {
            //             for (Vertex v : n) {
            //                 printer.print(u, v);
            //             }
            //             return null;
            //         }).compute();
            //         return true;
            //     })
            // );

            // defered.compute();
        }
    }

    public static void main(String[] args) {
        Options.v().set_output_dir("./.soot");
        Options.v().parse(args);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("wjap").add(new Transform("wjap.print_icfg", new ICFGTransformer()));

        PackManager.v().runPacks();
    }
}
