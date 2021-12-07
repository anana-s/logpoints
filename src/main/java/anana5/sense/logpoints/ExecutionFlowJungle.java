package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import anana5.sense.graph.Jungle;
import anana5.sense.graph.Path;
import anana5.sense.graph.Branch;
import anana5.sense.graph.Cont;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExecutionFlowJungle extends Jungle<Unit> {

    private static boolean skip(SootMethod m) {
        return m.isJavaLibraryMethod() || m.isPhantom() || !m.isConcrete();
    }

    public ExecutionFlowJungle(CallGraph callgraph, Collection<SootMethod> entrypoints) {
        super(new Builder(callgraph).build(entrypoints));
    }

    static class Builder {
        final CallGraph cg;

        Builder(CallGraph cg) {
            this.cg = cg;
        }
        
        Jungle<Unit> build(Collection<SootMethod> es) {
            return build(es, new Jungle<>(), new Path<>());
        }

        Jungle<Unit> build(Collection<SootMethod> es, Jungle<Unit> returns, Path<Unit, Jungle<Unit>> path) {
            Collection<Jungle<Unit>> jungles = new ArrayList<>(es.size());
            Cont<Collection<Unit>> rets = returns.branches();
            for (SootMethod e : es) {
                if (skip(e)) {
                    continue;
                }
                ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(e.retrieveActiveBody());
                jungles.add(Jungle.unfold(new BranchBuilder(cfg, rets), cfg.getHeads()));
            }

            if (jungles.isEmpty()) {
                return returns;
            }

            return Jungle.merge(jungles).modify((unit, jungle) -> {
                if (!(unit instanceof Stmt) || !((Stmt)unit).containsInvokeExpr()) {
                    return jungle;
                }
                if (path.contains(unit)) {
                    return new Jungle<>();
                }
                Collection<SootMethod> methods = new ArrayList<>();
                cg.edgesOutOf(unit).forEachRemaining(edge -> methods.add(edge.tgt()));
                return path.push(unit, $path -> build(methods, jungle, $path));
            });
        }
        
        class BranchBuilder implements Function<Unit, Branch<Unit>> {
            final Map<Unit, Branch<Unit>> visited;
            final ExceptionalUnitGraph cfg;
            final Cont<Collection<Unit>> returns;
    
            BranchBuilder(ExceptionalUnitGraph controlflowgraph, Cont<Collection<Unit>> returns) {
                this.cfg = controlflowgraph;
                this.returns = returns;
                this.visited = new HashMap<>();
            }
    
            @Override
            public Branch<Unit> apply(Unit unit) {
                return visited.computeIfAbsent(unit, $unit -> new Branch<Unit>($unit, succs(cfg.getSuccsOf($unit))));
            }

            public Cont<Collection<Unit>> succs(Collection<Unit> units) {
                if (units.isEmpty()) {
                    return returns;
                } else {
                    return Cont.of(units);
                }
            }
        }
    }
}
