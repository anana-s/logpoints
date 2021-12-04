package anana5.sense.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class ExecutionFlowJungle extends Jungle<Object> {

    public ExecutionFlowJungle(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        this.cgraph = unfold(new Builder(callgraph), entrypoints).map(method -> (Object)method).cgraph;
    }

    class Builder implements Function<Collection<SootMethod>, CGraphF<SootMethod, Collection<SootMethod>>> {
        Path<Object, CGraphF<Object, Iterator<Object>>> path;
        CallGraph callgraph;

        Builder(CallGraph callgraph) {
            this.callgraph = callgraph;
        }

        @Override
        public CGraphF<SootMethod, Collection<SootMethod>> apply(Collection<SootMethod> methods) {
            Collection<NodeF<SootMethod, Collection<SootMethod>>> nodes = new HashSet<>(methods.size());
            for (SootMethod method : methods) {
                Collection<SootMethod> successors = new HashSet<>();
                for (Edge edge : (Iterable<Edge>)() -> callgraph.edgesOutOf(method)) {
                    successors.add(edge.tgt());
                }
                nodes.add(new NodeF<>(method, successors));
            }
            return new CGraphF<>(nodes);
        }

        class Path<Ref, Tgt> {
            Path<Ref, Tgt> prev;
            Ref ref;
            Tgt tgt;
            public Path() {
                this(null, null, null);
            }
            public Path(Path<Ref, Tgt> prev, Ref ref, Tgt target) {
                this.prev = prev;
                this.ref = ref;
                this.tgt = target;
            };
            public Path<Ref, Tgt> push(Ref ref, Tgt target) {
                return new Path<>(this, ref, target);
            }
            public boolean contains(Object ref) {
                if (this.ref != null && this.ref.equals(ref)) {
                    return true;
                } else if (prev == null) {
                    return false;
                } else {
                    return prev.contains(ref);
                }
            }
            public Tgt find(Ref ref) {
                if (this.ref != null && this.ref.equals(ref)) {
                    return tgt;
                } else if (prev == null) {
                    return null;
                } else {
                    return prev.find(ref);
                }
            }
        }
    }

    
}

