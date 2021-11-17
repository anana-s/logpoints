package anana5.sense.graph.java;

import java.util.List;
import java.util.Map;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
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

            // ef.map((Object ref, Stream<Stream<Vertex>> vs) -> {
            //     Stream<Vertex> suc = vs.flatMap(Function.identity());
            //     if (ref instanceof InvokeStmt) {
            //         return Stream.of(ef.new Vertex(ref, suc));
            //     }
            //     return suc;
            // });

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
