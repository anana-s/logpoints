package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
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
import anana5.util.Promise;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class GraphFactory {

    static Logger logger = LoggerFactory.getLogger(ExecutionFlow.class);

    CallGraph cg;

    public GraphFactory(CallGraph cg) {
        this.cg = cg;
    }

    public Rain<Stmt> build(CallGraph cg, Iterable<SootMethod> entrypoints) {
        var builder = new Builder(cg);
        var states = new LList<>(entrypoints).flatmap(entrypoint -> {
            var context = builder.new Context(entrypoint);
            var frame = builder.new Frame(context);
            return context.roots().map(pointer -> builder.new State(frame, pointer));
        });
        return Rain.unfold(states, builder);
    }

    private Rain<Stmt> build(LList<SootMethod> methods, Rain<Stmt> rets) {
        return Rain.merge(methods.map(method -> build(method).fold(droplets -> {
            var promise = droplets.isEmpty().map(isEmpty -> {
                if (isEmpty) {
                    return rets;
                } else {
                    return new Rain<>(droplets);
                }
            });
            return Rain.bind(promise);
        })));
    }

    Map<SootMethod, Rain<Stmt>> visited = new HashMap<>();
    private Rain<Stmt> build(SootMethod method) {
        return visited.computeIfAbsent(method, m -> new CFGFactory(method).build());
    }

    class CFGFactory {
        Map<Stmt, Rain<Stmt>> visited;
        ExceptionalUnitGraph cfg;
        CFGFactory(SootMethod method) {
            this.cfg = new ExceptionalUnitGraph(method.retrieveActiveBody());

        }

        Rain<Stmt> build() {
            return build(new LList<>(cfg.getHeads()).map(unit -> (Stmt)unit));
        }

        private Rain<Stmt> build(LList<Stmt> stmts) {
            var droplets = stmts.map(stmt -> {
                var rets = build(new LList<>(cfg.getSuccsOf(stmt)).map(unit -> (Stmt)unit));

                if (stmt.containsInvokeExpr()) {
                    var methods = new LList<>(cg.edgesOutOf(stmt)).map(edge -> edge.tgt());
                    return new Droplet<>(stmt, GraphFactory.this.build(methods, rets));
                } else {
                    return new Droplet<>(stmt, rets);
                }
            });
            return new Rain<Stmt>(droplets);
        }
    }

    private static class Builder implements Function<LList<Builder.State>, LList<Droplet<Stmt, LList<Builder.State>>>> {
        final private Map<SootMethod, Rain<Stmt>> cache = new HashMap<>();
        final private CallGraph cg;

        Builder(CallGraph cg) {
            this.cg = cg;
        }

        @Override
        public LList<Droplet<Stmt, LList<State>>> apply(LList<State> states) {
            return states.map(state -> state.droplet());
        }

        private class State {
            Frame frame;
            Context.Pointer pointer;

            State(Frame frame, Context.Pointer pointer) {
                this.frame = frame;
                this.pointer = pointer;
            }

            Droplet<Stmt, LList<State>> droplet() {
                return pointer.droplet(frame);
            }
        }

        private class Frame {
            final Frame parent;
            final Context context;
            final LList<Context.Pointer> rets;

            Frame(Context context) {
                this(null, context, new LList<>());
            }

            Frame(Frame parent, Context context, LList<Context.Pointer> rets) {
                this.parent = parent;
                this.context = context;
                this.rets = rets;
            }

            Frame(SootMethod method) {
                this(new Context(method));
            }

            Frame(Frame parent, SootMethod method, LList<Context.Pointer> rets) {
                this.parent = parent;
                this.context = new Context(method);
                this.rets = rets;
            }

            LList<Context.Pointer> nextOf(Context.Pointer pointer) {
                var units = context.cfg.getSuccsOf(pointer.stmt);
                if (units.isEmpty()) {
                    return rets;
                }
                return new LList<>(units).map(unit -> context.new Pointer((Stmt)unit));
            }
        }

        private class Context {
            ExceptionalUnitGraph cfg;

            public Context(SootMethod method) {
                this.cfg = new ExceptionalUnitGraph(method.getActiveBody());
            }

            private LList<Pointer> roots = null;
            public LList<Pointer> roots() {
                if (roots == null) {
                    roots = new LList<>(cfg.getHeads()).map(unit -> new Pointer((Stmt)unit));
                }
                return roots;
            }

            public class Pointer {
                final private Stmt stmt;
    
                private Droplet<Stmt, LList<State>> droplet;
                public Droplet<Stmt, LList<State>> droplet(Frame frame) {
                    if (droplet != null) {
                        return droplet;
                    }

                    LList<Pointer> rets = frame.nextOf(this);
        
                    if (isInvokation()) {
                        droplet = new Droplet<>(stmt, new LList<>(cg.edgesOutOf(stmt)).flatmap(edge -> {
                            // create new frame for invoked method
                            Frame newFrame = new Frame(frame, edge.tgt(), rets);


                            return null;
                        }));
                    } else {
                        droplet = new Droplet<>(stmt, rets.map(pointer -> new State(frame, pointer)));
                    }
    
                    return droplet;
                }
            
                private Pointer(Stmt stmt) {
                    this.stmt = stmt;
                }
            
                private boolean isInvokation() {
                    return this.stmt.containsInvokeExpr();
                }
            }
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
