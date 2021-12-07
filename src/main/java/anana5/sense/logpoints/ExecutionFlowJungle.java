package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import anana5.sense.graph.Jungle;
import anana5.sense.graph.Branch;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class ExecutionFlowJungle extends Jungle<Object> {

    public ExecutionFlowJungle(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        super(from(callgraph, entrypoints));
    }

    static Jungle<Object> from(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        return Jungle.unfold(new Builder(callgraph), entrypoints);
    }

    static class Builder implements Function<SootMethod, Branch<Object, SootMethod>> {
        Map<Object, Branch<Object, SootMethod>> seen;
        CallGraph callgraph;

        Builder(CallGraph callgraph) {
            this.callgraph = callgraph;
            this.seen = new HashMap<>();
        }

        @Override
        public Branch<Object, SootMethod> apply(SootMethod method) {
            if (skip(method)) {
                return new Branch<>(method);
            }

            return seen.computeIfAbsent(method, key -> new Branch<>(method, () -> successors(method)));
        }

        public Collection<SootMethod> successors(SootMethod method) {
            Collection<SootMethod> successors = new ArrayList<>();
            for (Edge edge : (Iterable<Edge>)() -> callgraph.edgesOutOf(method)) {
                successors.add(edge.tgt());
            }
            return successors;
        }

        private static boolean skip(SootMethod m) {
            return m.isJavaLibraryMethod() || m.isPhantom() || !m.isConcrete();
        }
    }

    
}

