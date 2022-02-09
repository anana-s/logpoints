package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Droplet;
import anana5.graph.rainfall.Rain;
import anana5.util.LList;
import anana5.util.ListF;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class GraphFactory {

    Filter filter;

    static Logger logger = LoggerFactory.getLogger(ExecutionFlow.class);

    public GraphFactory filter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public GraphFactory() {
        this.filter = (a) -> true;
    }

    public Graph<Stmt> build(CallGraph cg, Iterable<SootMethod> entrypoints) {
        List<Frame> out = new ArrayList<>();
        for (SootMethod entrypoint : entrypoints) {
            out.add(new Frame(entrypoint));
        }
        return Rain.unfold(new LList<>(out), new Builder(cg));
    }

    private static class Builder implements Function<LList<Frame>, LList<Droplet<Stmt, LList<Frame>>>> {
        Map<SootMethod, Frame> cache = new HashMap<>();
        CallGraph cg;

        Builder(CallGraph cg) {
            this.cg = cg;
        }

        @Override
        public LList<Droplet<Stmt, LList<Frame>>> apply(LList<Frame> pointers) {
            var out = pointers.map(frame -> {
                return frame.roots().map(pointer -> {
                    var next = new LList<Edge>(cg.edgesOutOf(pointer.stmt));
                    var droplet = new Droplet<Stmt, LList<Frame>>(pointer.stmt, next.map(edge -> frame.push(edge.tgt(), Collections.emptyList())));
                    return droplet;
                });
            });
            
            var flat = out.flatmap(Function.identity());

            return flat;
        }

        private static boolean shouldSkip(SootMethod m) {
            return !m.isPhantom() 
            && m.isConcrete()
            && !m.isJavaLibraryMethod()
            && !m.getDeclaringClass().isLibraryClass();
        }
    }

    public interface Filter extends Predicate<Drop<Stmt>> {}
}
