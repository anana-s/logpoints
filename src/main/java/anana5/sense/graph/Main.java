package anana5.sense.graph;

import java.util.List;
import java.util.Map;

import anana5.sense.graph.EFGraph.Vertex;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.InvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class Main {

    public static class ICFGTransformer extends SceneTransformer {
        @Override
        protected void internalTransform(String phaseName, Map<String, String> options) {

            CallGraph callgraph = Scene.v().getCallGraph();

            List<SootMethod> entrypoints = Scene.v().getEntryPoints();

            Jungle<Object> flow = new ExecutionFlowJungle(callgraph, entrypoints);
            // entrypoints.removeIf(m -> !EFGraph.mfilter(m));

            // GraphPrinter printer = new GraphVizPrinter();

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
