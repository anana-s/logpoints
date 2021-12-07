package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class Jungle<T> {

    CGraphF<T, Jungle<T>> cgraph;

    public Jungle(CGraphF<T, Jungle<T>> dnodes) {
        this.cgraph = dnodes;
    }

    public Jungle(Jungle<T> other) {
        this(other.cgraph);
    }

    public Jungle() {
        this(new CGraphF<>());
    }
    
    public Jungle(Supplier<? extends Collection<? extends NodeF<T, Jungle<T>>>> supplier) {
        this(new CGraphF<>(supplier));
    }

    public <S> Jungle(Function<S, ? extends CGraphF<T, S>> f, S s) {
        this.cgraph = f.apply(s).map(s$ -> new Jungle<>(f, s$));
    }

    public <R> R fold(Function<CGraphF<T, R>, ? extends R> f) {
        return fold(f, new HashSet<>(), new HashMap<>());
    }

    private <R> R fold(Function<CGraphF<T, R>, ? extends R> f, Set<Jungle<T>> open, Map<Jungle<T>, R> close) {
        if (close.containsKey(this)) {
            return close.get(this);
        }

        if (open.contains(this)) {
            R r = f.apply(new CGraphF<>());
            close.put(this, r);
            return r;
        }
        open.add(this);

        R r = f.apply(cgraph.map(jungle -> jungle.fold(f, open, close)));
        close.put(this, r);

        return r;
    }

    public static <T, S> Jungle<T> unfold(Function<S, ? extends CGraphF<T, S>> f, S s) {
        return new Jungle<>(f, s);
    }

    public static <T, S> Jungle<S> unfold(Function<T, ? extends NodeF<S, Cont<Collection<T>>>> f, Collection<T> base) {
        return new Jungle<>(b -> {
            return new CGraphF<>(() -> {
                Collection<NodeF<S, Cont<Collection<T>>>> collection = new HashSet<>();
                for (T t : b) {
                    collection.add(f.apply(t));
                }
                return collection;
            }).bind(Function.identity());
        }, base);
    }

    public CGraphF<T, Jungle<T>> unfix() {
        return cgraph;
    }

    public Jungle<T> filter(BiPredicate<T, Jungle<T>> p) {
        return fold(cgraph -> new Jungle<>(cgraph.filter(p)));
    }

    public <R> Jungle<R> map(Function<T, R> f) {
        return new Jungle<>(cgraph -> cgraph.map(f, jungle -> jungle.cgraph), cgraph);
    }

    public Jungle<T> modify(BiFunction<T, Jungle<T>, Jungle<T>> g) {
        return fold(cgraph -> new Jungle<>(cgraph.map(g)));
    }

    public Cont<Collection<T>> branches() {
        return cgraph.c.map(graph -> {
            Collection<T> branches = new ArrayList<>(graph.nodes.size());
            for (NodeF<T, Jungle<T>> node : graph.nodes) {
                branches.add(node.ref);
            }
            return branches;
        });
    }

    public Cont<Void> traverse(BiConsumer<T, Collection<T>> visitor) {
        Set<NodeF<T, Jungle<T>>> visited = new HashSet<>();
        return cgraph.c.bind(
            graph -> {
                Cont<Void> traversal = Cont.of(() -> null);
                for (NodeF<T, Jungle<T>> node : graph.nodes) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        traversal = traversal.bind(v -> node.next.traverse(node.ref, visitor, visited));
                    }
                }
                return traversal;
            }
        );
    }

    private Cont<Void> traverse(T parent, BiConsumer<T, Collection<T>> visitor, Set<NodeF<T, Jungle<T>>> visited) {
        return cgraph.c.bind(
            graph -> {
                Collection<T> collected = new ArrayList<>(graph.nodes.size());
                Cont<Void> traversal = Cont.of(() -> null);
                for (NodeF<T, Jungle<T>> node : graph.nodes) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        traversal = traversal.bind(v -> node.next.traverse(node.ref, visitor, visited));
                    }
                    collected.add(node.ref);
                }
                visitor.accept(parent, collected);
                return traversal;
            }
        );
    }

    public static <T> Jungle<T> merge(Collection<Jungle<T>> jungles) {
        Cont<GraphF<T, Jungle<T>>> $ = Cont.of(() -> new GraphF<>());
        for (Jungle<T> jungle : jungles) {
            $ = $.bind(a -> jungle.cgraph.c.bind(b -> Cont.of(GraphF.merge(a, b))));
        }
        return new Jungle<>(new CGraphF<>($));
    }

    public static class CGraphF<A ,F> {
        Cont<GraphF<A, F>> c;
        public CGraphF(Cont<? extends GraphF<A, F>> cgraph) {
            this.c = cgraph.map(graph -> (GraphF<A, F>)graph);
        }
        public CGraphF(GraphF<A, F> graph) {
            this(Cont.of(() -> graph));
        }
        public CGraphF(CGraphF<A, F> cgraph) {
            this.c = cgraph.c;
        }
        public CGraphF() {
            this(new GraphF<>());
        }
        public CGraphF(Supplier<? extends Collection<? extends NodeF<A, F>>> nodes) {
            this(Cont.of(nodes).map(GraphF::new));
        }
        static <A, F> CGraphF<A, F> collect(Supplier<? extends Collection<? extends NodeF<A, F>>> nodes) {
            return new CGraphF<>(Cont.of(nodes).map(GraphF::new));
        }
        public <R, S> CGraphF<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new CGraphF<>(c.map(graph -> graph.map(f, g)));
        }
        public <G> CGraphF<A, G> map(Function<F, G> g) {
            return new CGraphF<>(c.map(graph -> graph.map(g)));
        }
        public <G> CGraphF<A, G> map(BiFunction<A, F, G> g) {
            return new CGraphF<>(c.map(graph -> graph.map(g)));
        }
        public <B, G> CGraphF<B, G> flatmap(BiFunction<A, F, CGraphF<B, G>> f) {
            return new CGraphF<>(c.bind(graph -> {
                Cont<GraphF<B, G>> out = Cont.of(() -> new GraphF<>());
                for (var node : graph.nodes) {
                    out = out.bind(g -> {
                        return f.apply(node.ref, node.next).c;
                    });
                }
                return out;
            }));
        }
        public <G> CGraphF<A, G> bind(Function<F, Cont<G>> f) {
            return new CGraphF<A, G>(c.bind(graph -> {
                Cont<GraphF<A, G>> out = Cont.of(() -> new GraphF<>());
                for (var node : graph.nodes) {
                    out = out.bind(g -> {
                        return f.apply(node.next).map(next -> {
                            g.nodes.add(new NodeF<>(node.seq, node.ref, next));
                            return g;
                        });
                    });
                }
                return out;
            }));
        }
        CGraphF<A, F> filter(BiPredicate<A, F> p) {
            return new CGraphF<>(c.map(graph -> graph.filter(p)));
        }
        void test() {
            
        }
    }

    public static class GraphF<A, F> {
        Collection<NodeF<A, F>> nodes;
        public GraphF() {
            this.nodes = new HashSet<>();
        }
        public GraphF(Collection<? extends NodeF<A, F>> nodes) {
            this.nodes = new HashSet<>(nodes.size());
            for (NodeF<A ,F> node : nodes) {
                this.nodes.add(node);
            }
        }
        public GraphF(GraphF<A, F> other) {
            this.nodes = new HashSet<>(other.nodes);
        }
        public <R, S> GraphF<R, S> map(Function<A, R> f, Function<F, S> g) {
            Collection<NodeF<R, S>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(f, g));
            }
            return new GraphF<>($);
        }
        public <G> GraphF<A, G> map(Function<F, G> g) {
            Collection<NodeF<A, G>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new GraphF<>($);
        }
        public <G> GraphF<A, G> map(BiFunction<A, F, G> g) {
            Collection<NodeF<A, G>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new GraphF<>($);
        }
        public <B, G> GraphF<B, G> flatmap(BiFunction<A, F, GraphF<B, G>> g) {
            Collection<NodeF<B, G>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.addAll(g.apply(node.ref, node.next).nodes);
            }
            return new GraphF<>($);
        }
        public GraphF<A, F> filter(BiPredicate<A, F> p) {
            Collection<NodeF<A, F>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                if (p.test(node.ref, node.next)) {
                    $.add(node);
                }
            }
            return new GraphF<>($);
        }
        public static <A, F> GraphF<A, F> merge(GraphF<A, F> a, GraphF<A, F> b) {
            Collection<NodeF<A, F>> out = new HashSet<>(a.nodes.size() + b.nodes.size());
            out.addAll(a.nodes);
            out.addAll(b.nodes);
            return new GraphF<>(out);
        }
    }

    public static class NodeF<A, F> {
        static int count = 0;
        int seq;
        A ref;
        F next;
        public NodeF(A ref, F next) {
            this.seq = count++;
            this.ref = ref;
            this.next = next;
        }
        public NodeF(int seq, A ref, F next) {
            this.seq = seq;
            this.ref = ref;
            this.next = next;
        }
        public NodeF(NodeF<A, F> node) {
            this.seq = node.seq;
            this.ref = node.ref;
            this.next = node.next;
        }
        public <R, S> NodeF<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new NodeF<>(seq, f.apply(ref), g.apply(next));
        }
        public <G> NodeF<A, G> map(Function<F, G> g) {
            return new NodeF<>(seq, ref, g.apply(next));
        }
        public <G> NodeF<A, G> map(BiFunction<A, F, G> g) {
            return new NodeF<>(seq, ref, g.apply(ref, next));
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NodeF)) {
                return false;
            }
            NodeF<?, ?> other = (NodeF<?, ?>)obj;
            return ref.equals(other.ref) && seq == other.seq;
        }
        @Override
        public int hashCode() {
            return Objects.hash(ref, seq);
        }
    }
}
