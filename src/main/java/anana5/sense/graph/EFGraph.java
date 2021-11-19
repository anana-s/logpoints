package anana5.sense.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class EFGraph {

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

    public class Vertex {
        public Object ref;
        public Defered<Node<Vertex>> scs;

        public Vertex(Object ref) {
            this.ref = ref;
            this.scs = () -> new Node<>();
        }

        public Vertex(Object ref, Defered<Node<Vertex>> scs) {
            this.ref = ref;
            this.scs = scs;
        }

        public Vertex(SootMethod method) {
            this(method, () -> new Node<>(), new Path<>());
        }

        public Vertex(SootMethod method, Defered<Node<Vertex>> rets, Path<Object, Vertex> path) {
            ref = method;
            if (!mfilter(method)) {
                scs = rets;
            } else {
                scs = () -> {
                    Path<Object, Vertex>  newPath = path.push(method, this);
                    ExceptionalUnitGraph g = new ExceptionalUnitGraph(method.getActiveBody());
                    Map<Unit, Vertex> visited = new WeakHashMap<>();
                    return new Node<>(g.getHeads()).map(u -> visited.computeIfAbsent(u, v -> new Vertex((Stmt)v, g, rets, newPath, visited)));
                };
            }
        }

        private Defered<Node<Vertex>> succs(Stmt stmt, ExceptionalGraph<Unit> g, Defered<Node<Vertex>> rets, Path<Object, Vertex>  path, Map<Unit, Vertex> visited) {
            return () -> {
                List<Unit> units = g.getSuccsOf(stmt);
                if (units.isEmpty()) {
                    return rets.value();
                } else {
                    return new Node<>(units).map(u -> visited.computeIfAbsent(u, v -> new Vertex((Stmt)v, g, rets, path, visited)));
                }
            };
        }

        private Vertex find(SootMethod method, Defered<Node<Vertex>> rets, Path<Object, Vertex> path) {
            Vertex target = path.find(method);
            if (target != null) {
                return target;
            } else {
                return new Vertex(method, rets, path);
            }
        }

        private Vertex(Stmt stmt, ExceptionalGraph<Unit> g, Defered<Node<Vertex>> rets, Path<Object, Vertex>  path, Map<Unit, Vertex> visited) {
            ref = stmt;
            scs = () -> {
                if (stmt.containsInvokeExpr()) {
                        Iterator<Edge> edges = cg.edgesOutOf(stmt);
                        if (!edges.hasNext()) {
                            return new Node<>(find(stmt.getInvokeExpr().getMethod(), succs(stmt, g, rets, path, visited), path));
                        } else {
                            Node<Vertex> node = new Node<>();
                            while (edges.hasNext()) {
                                node.add(find(edges.next().tgt(), succs(stmt, g, rets, path, visited), path));
                            }
                            return node;
                        }
                } else {
                    return succs(stmt, g, rets, path, visited).value();
                }
            };
        }

        public <R> R fold(Supplier<R> base, BiFunction<Object, Defered<Node<R>>, R> f) {
            return fold(base, f, Collections.newSetFromMap(new WeakHashMap<>()), new WeakHashMap<>());
        }

        private <R> R fold(Supplier<R> base, BiFunction<Object, Defered<Node<R>>, R> f, Set<Vertex> seen, Map<Vertex, R> visited) {
            if (visited.containsKey(this)) {
                return visited.get(this);
            }
            if (seen.contains(this)) {
                return base.get();
            }
            seen.add(this);
            return visited.compute(this,(u, r) -> f.apply(u.ref, u.scs.map(n -> n.map(v -> v.fold(base, f, seen, visited)))));
        }

        public Node<Vertex> filter(Predicate<Object> p) {
            return fold(() -> new Node<>(), (ref, dnnv) -> {
                Defered<Node<Vertex>> dnv = dnnv.map(nnv -> nnv.flatmap(Function.identity()));
                if (p.test(ref)) {
                    return new Node<>(new Vertex(ref, dnv));
                } else {
                    return dnv.value();
                }
            });
        }

        public void traverse(Predicate<Vertex> visitor, Set<Vertex> visited) {
            Stack<Vertex> stack = new Stack<>(); 
            stack.add(this);
            while (!stack.isEmpty()) {
                Vertex v = stack.pop();
                if (visited.contains(v)) {
                    continue;
                }
                visited.add(v);
                if (!visitor.test(v)) {
                    continue;
                }
                stack.addAll(v.scs.value());
            }
        }
        @Override
        public String toString() {
            return ref.toString();
        }
    }

    Set<Vertex> nodes = new HashSet<>();
    CallGraph cg;
    Defered<Node<Vertex>> entrypoints;

    static boolean mfilter(SootMethod m) {
        return !m.isJavaLibraryMethod() && !m.isPhantom() && m.isConcrete();
    }

    public EFGraph(CallGraph cg, List<SootMethod> entrypoints) {
        this.cg = cg;
        this.entrypoints = () -> new Node<SootMethod>(entrypoints).map(m -> new Vertex(m));
    }

    public Defered<Void> traverse(Predicate<Vertex> visitor) {
        Set<Vertex> visited = new HashSet<>();
        return entrypoints.map(n -> {
            n.forEach(v -> {
                v.traverse(visitor, visited);
            });
            return null;
        });
    }

    public <R> Defered<EFGraph> transform(BiFunction<Object, Defered<Node<Vertex>>, Vertex> f) {
        return () -> {
            entrypoints = entrypoints.bind(n -> fold(() -> null, f));
            return this;
        };
    }
    
    public <R> Defered<Node<R>> fold(Supplier<R> base, BiFunction<Object, Defered<Node<R>>, R> f) {
        return entrypoints.map(n -> n.map(v -> v.fold(base, f)));
    }

    public Defered<EFGraph> filter(Predicate<Object> p) {
        return () -> {
            entrypoints = entrypoints.map(n -> n.flatmap(v -> v.filter(p)));
            return this;
        };
    }
}
