package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Jungle<T> {
    public static class NodeF<A, F> {
        A ref;
        F next;
        NodeF(A ref, F next) {
            this.ref = ref;
            this.next = next;
        }
        <R, S> NodeF<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new NodeF<>(f.apply(ref), g.apply(next));
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

    D<Collection<NodeF<T, Jungle<T>>>> dnodes;

    Jungle() {
        dnodes = D.pure(Collections.emptySet());
    }

    Jungle(D<Collection<NodeF<T, Jungle<T>>>> nodes) {
        this.dnodes = nodes;
    }

    public <R> R cata(Function<D<Collection<NodeF<T, R>>>, R> f) {
        return cata(f, new HashSet<>(), new HashMap<>());
    }

    private <R> R cata(Function<D<Collection<NodeF<T, R>>>, R> f, Set<Jungle<T>> open, Map<Jungle<T>, R> close) {
        if (close.containsKey(this)) {
            return close.get(this);
        }
        if (open.contains(this)) {
            R r = f.apply(D.pure(Collections.emptySet()));
            close.put(this, r);
            return r;
        }
        open.add(this);
        R r = f.apply(dnodes.map(nodes -> {
            Collection<NodeF<T, R>> collection_node_t = new ArrayList<>();
            for (NodeF<T, Jungle<T>> node : nodes) {
                collection_node_t.add(node.map(t -> t, jungle -> jungle.cata(f, open, close)));
            }
            open.remove(this);
            return collection_node_t;
        }));
        close.put(this, r);
        return r;
    }

    public <R> R fold(Function<Collection<R>, R> f) {
        return cata(d_c_n_t -> d_c_n_t.map(c_n_t -> {
            Collection<R> c_r = new ArrayList<>();
            for (NodeF<T, R> node : c_n_t) {
                c_r.add(node.next);
            }
            return f.apply(c_r);
        }).get());
    }

    public static <A, B> Jungle<A> unfold(BiFunction<A, B, D<Collection<NodeF<A, B>>>> f, A a, B b) {
        return new Jungle<>(f.apply(a, b).map(c_n_a_b -> {
            Collection<NodeF<A, Jungle<A>>> $ = new HashSet<>();
            for (NodeF<A, B> node : c_n_a_b) {
                $.add(new NodeF<>(node.ref, unfold(f, node.ref, b)));
            }
            return $;
        }));
    }

    public Jungle<T> filter(Predicate<T> p) {
        return cata(d_c_n_t -> new Jungle<>(d_c_n_t.map(c_n_t -> {
            Collection<NodeF<T, Jungle<T>>> $ = new ArrayList<>();
            for (NodeF<T, Jungle<T>> node : c_n_t) {
                if (p.test(node.ref)) {
                    $.add(node);
                }
            }
            return $;
        })));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Jungle)) {
            return false;
        }
        Jungle<?> other = (Jungle<?>)obj;
        return dnodes.equals(other.dnodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dnodes);
    }
}
