package anana5.sense.graph.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
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

    class Node<T> {
        Stream<T> successors;
        Object ref;
        
        public Node(Object ref, Stream<T> successors) {
            this.ref = ref;
            this.successors = successors;
        }
        public <R> Node<R> map(Function<T, R> f) {
            return new Node<R>(ref, successors.map(f));
        }
        public Collection<T> successors() {
            return successors.collect(Collectors.toList());
        }
        public Object get() {
            return ref;
        }
    }

    class Vertex {

        private Stream<Vertex> successors;
        public Object ref;

        public Vertex(SootMethod method) {
            this(method, Stream.empty(), new Path());
        }

        private Vertex(Object ref, Stream<Vertex> successors) {
            this.ref = ref;
            this.successors = successors;
        }

        public Vertex(Vertex v) {
            this(v.ref, v.successors);
        }

        private Vertex(SootMethod method, Stream<Vertex> rets, Path path) {
            Path newPath = path.push(method, this);
            ref = method;
            if (!mfilter(method)) {
                successors = rets;
            } else {
                ExceptionalGraph<Unit> g = new ExceptionalUnitGraph(method.getActiveBody());
                successors = (g.getHeads().stream()
                    .map(s -> new Vertex((Stmt)s, g, rets, newPath, new HashMap<>()))
                    .distinct()
                );
            }
        }

        private Vertex(Stmt stmt, ExceptionalGraph<Unit> g, Stream<Vertex> rets, Path path, Map<Unit, Vertex> cache) {
            ref = stmt;
            if (stmt.containsInvokeExpr()) {
                SootMethod method = stmt.getInvokeExpr().getMethod();
                Vertex target = path.find(method);
                if (target != null) {
                    successors = Stream.of(target);
                } else {
                    successors = Stream.of(new Vertex(
                        method,
                        (g
                            .getSuccsOf(stmt)
                            .stream()
                            .map(u -> new Vertex((Stmt)u, g, rets, path, cache))
                            .distinct()
                        ),
                        path
                    ));
                }
            } else if (g.getSuccsOf(stmt).isEmpty()) {
                successors = rets;
            } else {
                successors = (g
                    .getSuccsOf(stmt)
                    .stream()
                    .map(s -> cache.computeIfAbsent(s, t -> new Vertex((Stmt)t, g, rets, path, cache)))
                    .distinct()
                );
            }
        }

        public Object get() {
            return ref;
        }

        public String toString() {
            return get().toString();
        }

        public List<Vertex> successors() {
            List<Vertex> out = successors.collect(Collectors.toList());
            successors = out.stream();
            return out;
        }

        public <R> R fold(Function<Stream<R>, R> f) {
            return f.apply(successors.map(s -> s.fold(f)));
        }

        public Vertex map(Function<Vertex, Vertex> f) {
            return f.apply(new Vertex(ref, successors.map(s -> s.map(f))));
        }

        public Vertex filter(Predicate<Vertex> p) {
            return new Vertex(ref, successors.filter(p).map(s -> s.filter(p)));
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
                stack.addAll(v.successors());
            }
        }
    }

    Set<Vertex> nodes = new HashSet<>();
    CallGraph cg;
    Stream<Vertex> entrypoints;

    static boolean mfilter(SootMethod m) {
        return !m.isJavaLibraryMethod() && !m.isPhantom() && m.isConcrete();
    }

    public EFGraph(CallGraph cg, List<SootMethod> entrypoints) {
        this.cg = cg;
        this.entrypoints = (entrypoints.stream()
            .map(m -> new Vertex(m))
            .distinct()
        );
    }

    public void traverse(Predicate<Vertex> visitor) {
        Set<Vertex> visited = new HashSet<>();
        entrypoints.forEach(v -> {
            v.traverse(visitor, visited);
        });
    }

    public void map(Function<Vertex, Vertex> f) {
        entrypoints.map(m -> m.map(f));
    }
    
    public <R> void fold(Function<Stream<R>, R> f) {
        entrypoints.map(m -> m.fold(f));
    }

    public void filter(Predicate<Vertex> p) {
        entrypoints.map(m -> m.filter(p));
    }
}
