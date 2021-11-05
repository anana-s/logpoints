package anana5.sense.graph.java;

import java.util.List;
import java.util.Map;

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
            ICFGBuilder builder = new PrintICFGBuilder();
    
            (new DFS<SootMethod>((m, methods) -> {
                if (m.isJavaLibraryMethod() || m.isPhantom() || !m.isConcrete()) {
                    return;
                }
    
                ExceptionalGraph<Unit> g = new ExceptionalUnitGraph(m.retrieveActiveBody());
    
                List<Unit> heads = g.getHeads();
    
                for (Unit u : heads) {
                    builder.add(m, u);
                }
    
                (new DFS<Unit>((u, units) -> {
                    for (Edge e : (Iterable<Edge>)() -> cg.edgesOutOf(u)) {
                        SootMethod n = e.tgt();
                        builder.add(u, n);
                        methods.accept(e.tgt());
                    }

                    for (Unit v : g.getSuccsOf(u)) {
                        builder.add(u, v);
                        units.accept(v);
                    }
                })).on(heads);
            })).on(Scene.v().getEntryPoints());   
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
