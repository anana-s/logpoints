package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    Jungle(CGraphF<T, Jungle<T>> dnodes) {
        this.cgraph = dnodes;
    }

    public Jungle(Jungle<T> other) {
        this(other.cgraph);
    }

    public Jungle() {
        this(new CGraphF<>());
    }

    <S> Jungle(Function<S, ? extends CGraphF<T, S>> f, S s) {
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

    public static <S, T> Jungle<T> unfold(Function<S, ? extends CGraphF<T, S>> f, S base) {
        return new Jungle<>(f, base);
    }

    public static <T, S> Jungle<S> unfold(Function<T, ? extends NodeF<S, Cont<Collection<T>>>> f, Collection<T> base) {
        return new Jungle<>(b -> {
            Collection<NodeF<S, Cont<Collection<T>>>> collection = new HashSet<>();
            for (T t : b) {
                collection.add(f.apply(t));
            }
            return new CGraphF<>(collection).bind(Function.identity());
        }, base);
    }

    public Jungle<T> filter(BiPredicate<T, Jungle<T>> p) {
        return fold(cgraph -> new Jungle<>(cgraph.filter(p)));
    }

    public <R> Jungle<R> map(Function<T, R> f) {
        return new Jungle<>(cgraph -> cgraph.map(f, jungle -> jungle.cgraph), cgraph);
    }

    public Cont<Collection<T>> successors() {
        return cgraph.c.map(graph -> {
            Collection<T> successors = new ArrayList<>(graph.nodes.size());
            for (NodeF<T, Jungle<T>> node : graph.nodes) {
                successors.add(node.ref);
            }
            return successors;
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

    static class CGraphF<A ,F> {
        Cont<? extends GraphF<A, F>> c;
        CGraphF(Cont<? extends GraphF<A, F>> cgraph) {
            this.c = cgraph;
        }
        CGraphF(GraphF<A, F> graph) {
            this(Cont.of(() -> graph));
        }
        CGraphF(CGraphF<A, F> cgraph) {
            this.c = cgraph.c;
        }
        CGraphF() {
            this(new GraphF<>());
        }
        CGraphF(Collection<? extends NodeF<A, F>> nodes) {
            this(new GraphF<>(nodes));
        }
        public CGraphF(Supplier<? extends GraphF<A, F>> graph) {
            this(Cont.of(graph));
        }
        static <A, F> CGraphF<A, F> collect(Supplier<? extends Collection<? extends NodeF<A, F>>> nodes) {
            return new CGraphF<>(Cont.of(nodes).map(GraphF::new));
        }
        <R, S> CGraphF<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new CGraphF<>(c.map(graph -> graph.map(f, g)));
        }
        <G> CGraphF<A, G> map(Function<F, G> g) {
            return new CGraphF<>(c.map(graph -> graph.map(g)));
        }
        <G> CGraphF<A, G> map(BiFunction<A, F, G> g) {
            return new CGraphF<>(c.map(graph -> graph.map(g)));
        }
        <G> CGraphF<A, G> bind(Function<F, Cont<G>> f) {
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

    static class GraphF<A, F> {
        Collection<NodeF<A, F>> nodes;
        GraphF() {
            this.nodes = new HashSet<>();
        }
        GraphF(Collection<? extends NodeF<A, F>> nodes) {
            this.nodes = new HashSet<>(nodes.size());
            for (NodeF<A ,F> node : nodes) {
                this.nodes.add(node);
            }
        }
        GraphF(GraphF<A, F> other) {
            this.nodes = new HashSet<>(other.nodes);
        }
        <R, S> GraphF<R, S> map(Function<A, R> f, Function<F, S> g) {
            Collection<NodeF<R, S>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(f, g));
            }
            return new GraphF<>($);
        }
        <G> GraphF<A, G> map(Function<F, G> g) {
            Collection<NodeF<A, G>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new GraphF<>($);
        }
        <G> GraphF<A, G> map(BiFunction<A, F, G> g) {
            Collection<NodeF<A, G>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new GraphF<>($);
        }
        GraphF<A, F> filter(BiPredicate<A, F> p) {
            Collection<NodeF<A, F>> $ = new HashSet<>();
            for (NodeF<A, F> node : nodes) {
                if (p.test(node.ref, node.next)) {
                    $.add(node);
                }
            }
            return new GraphF<>($);
        }
        GraphF<A, F> concat(GraphF<A, F> other) {
            Collection<NodeF<A, F>> out = new HashSet<>(nodes);
            out.addAll(other.nodes);
            return new GraphF<>(out);
        }
    }

    static class NodeF<A, F> {
        static int count = 0;
        int seq;
        A ref;
        F next;
        public NodeF(A ref, F next) {
            this.seq = count++;
            this.ref = ref;
            this.next = next;
        }
        NodeF(int seq, A ref, F next) {
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
