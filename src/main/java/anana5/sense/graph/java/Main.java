package anana5.sense.graph.java;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import anana5.sense.graph.java.EFGraph.Node;
import anana5.sense.graph.java.EFGraph.Vertex;
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
            CallGraph cg = Scene.v().getCallGraph();

            List<SootMethod> entrypoints = Scene.v().getEntryPoints();
            entrypoints.removeIf(m -> !EFGraph.mfilter(m));

            EFGraph ef = new EFGraph(cg, entrypoints);

            ef.map(node -> {
                List<Vertex> sucs = node.successors.stream().map(d -> d.get()).flatMap(List::stream).collect(Collectors.toList());
                if (node.ref instanceof InvokeStmt) {
                    return Collections.singletonList(ef.new Vertex(node.ref, sucs));
                }
                return sucs;
            });

            GraphPrinter printer = new GraphPrinter();
            ef.traverse(u -> {
                for (EFGraph.Vertex v : u.successors) {
                    printer.print(u, v.get());
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
