package anana5.sense.graph.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class EFGraph {

    class Path {
        Path prev;
        Object ref;
        Vertex target;
        public Path() {
            this(null, null, null);
        }
        public Path(Path prev, Object ref, Vertex target) {
            this.prev = prev;
            this.ref = ref;
            this.target = target;
        };
        public Path push(Object ref, Vertex target) {
            return new Path(this, ref, target);
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
        public Vertex find(Object ref) {
            if (this.ref != null && this.ref.equals(ref)) {
                return target;
            } else if (prev == null) {
                return null;
            } else {
                return prev.find(ref);
            }
        }
    }

    class Node<O, T> {
        public List<T> successors;
        public O ref;

        public Node() {}
        
        public Node(O ref, List<T> successors) {
            this.ref = ref;
            this.successors = successors;
        }
        public Node(Node<O,T> n) {
            this.ref = n.ref;
            this.successors = n.successors;
        }
        public <R> Node<O,R> map(Function<T, R> f) {
            return new Node<>(ref, successors.stream().map(f).collect(Collectors.toList()));
        }
        public Node<O, T> filter(Predicate<T> p) {
            return new Node<>(ref, successors.stream().filter(p).collect(Collectors.toList()));
        }
        public Node<O, T> filter() {
            return filter(t -> t != null);
        }
        public O ref() {
            return ref;
        }
        public Node<O, T> ref(O ref) {
            this.ref = ref;
            return this;
        }
        @Override
        public String toString() {
            return ref.toString();
        }
    }

    public class Vertex extends Delayed<Node<?, Vertex>> {
        public Vertex() {
            super();
        }

        public Vertex(D<Node<?, Vertex>> n) {
            super(n);
        } 

        public Vertex(SootMethod method) {
            this(method, new ArrayList<>(), new Path());
        }

        public Vertex(SootMethod method, List<Vertex> rets, Path path) {
            set(() -> {
                List<Vertex> successors;
                Path newPath = path.push(method, this);
                if (!mfilter(method)) {
                    successors = rets;
                } else {
                    ExceptionalGraph<Unit> g = new ExceptionalUnitGraph(method.getActiveBody());
                    Map<Unit, Vertex> cache = new HashMap<>();
                    successors = (g.getHeads().stream()
                        .map(s -> cache.computeIfAbsent(s, t -> new Vertex((Stmt)t, g, rets, newPath, cache)))
                        .distinct()
                        .collect(Collectors.toList())
                    );
                }
                return new Node<>(method, successors);
            });
        }

        private Vertex(Stmt stmt, ExceptionalGraph<Unit> g, List<Vertex> rets, Path path, Map<Unit, Vertex> cache) {
            set(() -> {
                List<Vertex> successors;
                if (stmt.containsInvokeExpr()) {
                    successors = new ArrayList<>();
                    List<Unit> sucs = g.getSuccsOf(stmt);
                    List<Vertex> newRets;
                    if (sucs.isEmpty()) {
                        newRets = rets;
                    } else {
                        newRets = (g
                            .getSuccsOf(stmt)
                            .stream()
                            .map(s -> cache.computeIfAbsent(s, t -> new Vertex((Stmt)t, g, rets, path, cache)))
                            .distinct()
                            .collect(Collectors.toList())
                        );
                    }
                    Iterator<Edge> edges = cg.edgesOutOf(stmt);
                    if (!edges.hasNext()) {
                        SootMethod method = stmt.getInvokeExpr().getMethod();
                        Vertex target = path.find(method);
                        if (target != null) {
                            successors.add(target);
                        } else {
                            successors.add(new Vertex(method, newRets, path));
                        }
                    } else while (edges.hasNext()) {
                        SootMethod method = edges.next().tgt();
                        Vertex target = path.find(method);
                        if (target != null) {
                            successors.add(target);
                        } else {
                            successors.add(new Vertex(method, newRets, path));
                        }
                    }
                } else {
                    List<Unit> sucs = g.getSuccsOf(stmt);
                    if (sucs.isEmpty()) {
                        successors = rets;
                    } else {
                        successors = (g
                            .getSuccsOf(stmt)
                            .stream()
                            .map(s -> cache.computeIfAbsent(s, t -> new Vertex((Stmt)t, g, rets, path, cache)))
                            .distinct()
                            .collect(Collectors.toList())
                        );
                    }
                
                }
                return new Node<>(stmt, successors);
            });
        }

        public <R> R fold(Function<Node<?, R>, R> f) {
            return f.apply(get().map(v -> v.fold(f)));
        }

        public Vertex filter(Predicate<Object> p) {
            return fold(node -> {
                if (!p.test(node.ref)) {
                    return new Vertex();
                }
                return new Vertex(() -> node.filter());
            });
        }

        public void traverse(Predicate<Node<?, Vertex>> visitor, Set<Vertex> visited) {
            Stack<Vertex> stack = new Stack<>(); 
            stack.add(this);
            while (!stack.isEmpty()) {
                Vertex v = stack.pop();
                if (visited.contains(v)) {
                    continue;
                }
                visited.add(v);
                if (!visitor.test(v.get())) {
                    continue;
                }
                stack.addAll(v.get().successors);
            }
        }
        @Override
        public String toString() {
            return get().toString();
        }
    }

    Set<Vertex> nodes = new HashSet<>();
    CallGraph cg;
    List<Vertex> entrypoints;

    static boolean mfilter(SootMethod m) {
        return !m.isJavaLibraryMethod() && !m.isPhantom() && m.isConcrete();
    }

    public EFGraph(CallGraph cg, List<SootMethod> entrypoints) {
        this.cg = cg;
        this.entrypoints = (entrypoints.stream()
            .map(m -> new Vertex(m))
            .distinct()
            .collect(Collectors.toList())
        );
    }

    public void traverse(Predicate<Node<?, Vertex>> visitor) {
        Set<Vertex> visited = new HashSet<>();
        for (Vertex v : entrypoints) {
            v.traverse(visitor, visited);
        }
    }
    
    public void map(Function<Node<?, Stream<Vertex>>, Stream<Vertex>> f) {
        entrypoints = smap(vs -> vs.map(v -> v.fold(f)).flatMap(Function.identity()));
    }
    
    public <R> List<R> fold(Function<Node<?, R>, R> f) {
        return smap(ss -> ss.map(v -> v.fold(f)));
    }

    public void filter(Predicate<Object> p) {
        entrypoints = smap(ss -> ss.map(v -> v.filter(p)));
    }

    public <R> List<R> smap(Function<Stream<Vertex>, Stream<R>> f) {
        return f.apply(entrypoints.stream()).collect(Collectors.toList());
    }
}
