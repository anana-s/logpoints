package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class Jungle<T> {

    CGraphF<T, Jungle<T>> cgraph;

    Jungle(CGraphF<T, Jungle<T>> dnodes) {
        this.cgraph = dnodes;
    }

    Jungle(Jungle<T> other) {
        this(other.cgraph);
    }

    Jungle() {
        this(new CGraphF<>());
    }

    public <S> Jungle(Function<S, CGraphF<T, S>> f, S s) {
        this.cgraph = f.apply(s).map(s$ -> new Jungle<>(f, s$));
    }

    public <R> R fold(Function<CGraphF<T, R>, R> f) {
        return fold(f, new HashSet<>(), new HashMap<>());
    }

    private <R> R fold(Function<CGraphF<T, R>, R> f, Set<Jungle<T>> open, Map<Jungle<T>, R> close) {
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

    public static <S, T> Jungle<S> unfold(Function<T, CGraphF<S, T>> f, T t) {
        return new Jungle<>(f, t);
    }

    public Jungle<T> filter(BiPredicate<T, Jungle<T>> p) {
        return fold(cgraph -> new Jungle<>(cgraph.filter(p)));
    }

    public <R> Jungle<R> map(Function<T, R> f) {
        return unfold(cgraph -> cgraph.map(f, jungle -> jungle.cgraph), cgraph);
    }

    public C<Collection<T>> successors() {
        return cgraph.c.map(graph -> {
            Collection<T> successors = new ArrayList<>(graph.nodes.size());
            for (NodeF<T, Jungle<T>> node : graph.nodes) {
                successors.add(node.ref);
            }
            return successors;
        });
    }

    public C<Void> traverse(T parent, BiConsumer<T, Collection<T>> visitor) {
        return cgraph.c.bind(
            graph -> C.of(graph.nodes).bind(
                nodes -> {
                    Collection<T> collected = new ArrayList<>(nodes.size());
                    C<Void> traversal = C.of(null);
                    for (NodeF<T, Jungle<T>> node : nodes) {
                        collected.add(node.ref);
                        traversal = traversal.bind(v -> node.next.traverse(node.ref, visitor));
                    }
                    visitor.accept(parent, collected);
                    return traversal;
                }
            )
        );
    }

    static class CGraphF<A ,F> {
        C<GraphF<A, F>> c;
        CGraphF(C<GraphF<A, F>> cgraph) {
            this.c = cgraph;
        }
        CGraphF(GraphF<A, F> graph) {
            this(C.of(graph));
        }
        CGraphF() {
            this(new GraphF<>());
        }
        CGraphF(Collection<NodeF<A, F>> nodes) {
            this(new GraphF<>(nodes));
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
        CGraphF<A, F> filter(BiPredicate<A, F> p) {
            return new CGraphF<>(c.map(graph -> graph.filter(p)));
        }
        void test() {
            
        }
    }

    static class GraphF<A, F> {
        Collection<NodeF<A, F>> nodes;
        GraphF() {
            this.nodes = Collections.emptySet();
        }
        GraphF(Collection<NodeF<A, F>> nodes) {
            this.nodes = nodes;
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
    }

    static class NodeF<A, F> {
        A ref;
        F next;
        NodeF(A ref, F next) {
            this.ref = ref;
            this.next = next;
        }
        <R, S> NodeF<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new NodeF<>(f.apply(ref), g.apply(next));
        }
        <G> NodeF<A, G> map(Function<F, G> g) {
            return new NodeF<>(ref, g.apply(next));
        }
        <G> NodeF<A, G> map(BiFunction<A, F, G> g) {
            return new NodeF<>(ref, g.apply(ref, next));
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
            return ref.equals(other.ref) && next.equals(other.next);
        }
        @Override
        public int hashCode() {
            return Objects.hash(ref, next);
        }
    }
}
