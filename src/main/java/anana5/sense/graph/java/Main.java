package anana5.sense.graph.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.graph.Graph;

import anana5.sense.graph.java.EFGraph.Vertex;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Main {

    public static class ICFGTransformer extends SceneTransformer {
        @Override
        protected void internalTransform(String phaseName, Map<String, String> options) {
            CallGraph cg = Scene.v().getCallGraph();

            List<SootMethod> entrypoints = Scene.v().getEntryPoints();
            entrypoints.removeIf(m -> !EFGraph.mfilter(m));

            EFGraph ef = new EFGraph(cg, entrypoints);

            GraphPrinter printer = new GraphPrinter();
            ef.traverse(u -> {
                for (Vertex v : u.successors()) {
                    printer.print(u, v);
                }
                return true;
            });
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
