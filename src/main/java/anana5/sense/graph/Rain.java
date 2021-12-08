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
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Rain<T> {

    Droplets<T, Rain<T>> cgraph;
    
    public static <T> Rain<T> from(Supplier<? extends Collection<? extends Drop<T, Rain<T>>>> supplier) {
        return new Rain<>(Droplets.from(supplier));
    }

    public Rain(Droplets<T, Rain<T>> dnodes) {
        this.cgraph = dnodes;
    }

    public Rain(Rain<T> other) {
        this(other.cgraph);
    }

    public Rain() {
        this(new Droplets<>());
    }

    public Droplets<T, Rain<T>> unfix() {
        return cgraph;
    } 

    public <G extends Droplets<T, Rain<T>>> G unfix(Class<G> cls) {
        return cls.cast(cgraph);
    } 

    public Cont<Collection<T>> collect() {
        return cgraph.collect();
    }

    public <R> R fold(Function<Droplets<T, R>, ? extends R> f) {
        return fold(f, new HashSet<>(), new HashMap<>());
    }

    private <R> R fold(Function<Droplets<T, R>, ? extends R> f, Set<Rain<T>> open, Map<Rain<T>, R> close) {
        if (close.containsKey(this)) {
            return close.get(this);
        }

        if (open.contains(this)) {
            R r = f.apply(new Droplets<>());
            close.put(this, r);
            return r;
        }
        open.add(this);

        R r = f.apply(cgraph.map(jungle -> jungle.fold(f, open, close)));
        close.put(this, r);

        return r;
    }

    public static <T, S> Rain<T> unfold(Function<S, ? extends Droplets<T, S>> f, S s) {
        return new Rain<>(f.apply(s).map($ -> unfold(f, $)));
    }

    public Rain<T> filter(Predicate<T> p) {
        return fold(droplets -> {
            droplets = droplets.flatmap(drop -> {
                if (!p.test(drop.ref)) {
                    return drop.next.cgraph;
                }
                return new Droplets<T, Rain<T>>(Collections.singleton(drop));
            });

            return new Rain<>(droplets);
        });
    }

    public <R> Rain<R> map(Function<T, R> f) {
        return unfold(cgraph -> cgraph.map(f, jungle -> jungle.cgraph), cgraph);
    }

    public Rain<T> modify(BiFunction<T, Rain<T>, Rain<T>> g) {
        return fold(cgraph -> new Rain<>(cgraph.map(g)));
    }

    public Cont<Void> traverse(BiConsumer<Drop<T, Rain<T>>.SnowFlake, Collection<Drop<T, Rain<T>>.SnowFlake>> visitor) {
        Set<Drop<T, Rain<T>>> visited = new HashSet<>();
        return cgraph.c.bind(
            graph -> {
                Cont<Void> traversal = Cont.of(() -> null);
                for (Drop<T, Rain<T>> node : graph.nodes) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        traversal = traversal.bind(v -> node.next.traverse(node, visitor, visited));
                    }
                }
                return traversal;
            }
        );
    }

    private Cont<Void> traverse(Drop<T, Rain<T>> parent, BiConsumer<Drop<T, Rain<T>>.SnowFlake, Collection<Drop<T, Rain<T>>.SnowFlake>> visitor, Set<Drop<T, Rain<T>>> visited) {
        return cgraph.c.bind(
            graph -> {
                Collection<Drop<T, Rain<T>>.SnowFlake> collected = new ArrayList<>(graph.nodes.size());
                Cont<Void> traversal = Cont.of(() -> null);
                for (Drop<T, Rain<T>> node : graph.nodes) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        traversal = traversal.bind(v -> node.next.traverse(node, visitor, visited));
                    }
                    collected.add(node.freeze());
                }
                visitor.accept(parent.freeze(), collected);
                return traversal;
            }
        );
    }

    public static <T> Rain<T> merge(Collection<Rain<T>> jungles) {
        Cont<Puddle<T, Rain<T>>> $ = Cont.of(() -> new Puddle<>());
        for (Rain<T> jungle : jungles) {
            $ = $.bind(a -> jungle.cgraph.c.bind(b -> Cont.of(Puddle.merge(a, b))));
        }
        return new Rain<>(new Droplets<>($));
    }

    public static class Droplets<A ,F> {
        Cont<Puddle<A, F>> c;
        private Droplets(Cont<? extends Puddle<A, F>> cgraph) {
            this.c = cgraph.map(graph -> (Puddle<A, F>)graph);
        }
        public Droplets(Droplets<A, F> cgraph) {
            this.c = cgraph.c;
        }
        public Droplets(Puddle<A, F> graph) {
            this(Cont.of(() -> graph));
        }
        public Droplets(Collection<? extends Drop<A, F>> nodes) {
            this(new Puddle<>(nodes));
        }
        public Droplets() {
            this(new Puddle<>());
        }
        static <A, F> Droplets<A, F> from(Supplier<? extends Collection<? extends Drop<A, F>>> nodes) {
            return new Droplets<>(Cont.of(nodes).map(Puddle::new));
        }
        public Cont<Collection<A>> collect() {
            return c.map(Puddle::collect);
        }
        public <R, S> Droplets<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new Droplets<>(c.map(graph -> graph.map(f, g)));
        }
        public <G> Droplets<A, G> map(Function<F, G> g) {
            return new Droplets<>(c.map(graph -> graph.map(g)));
        }
        public <G> Droplets<A, G> map(BiFunction<A, F, G> g) {
            return new Droplets<>(c.map(graph -> graph.map(g)));
        }
        public <B, G> Droplets<B, G> flatmap(Function<Drop<A, F>, Droplets<B, G>> f) {
            return new Droplets<>(c.bind(graph -> {
                Cont<Puddle<B, G>> out = Cont.of(() -> new Puddle<>());
                for (var node : graph.nodes) {
                    out = out.bind(a -> f.apply(node).c.map(b -> {
                        a.addAll(b);
                        return a;
                    }));
                }
                return out;
            }));
        }
        public <T> Cont<T> bind(Function<Drop<A, F>, Cont<T>> f) {
            return c.bind(graph -> {
                Cont<T> out = Cont.of(() -> null);
                for (var node : graph.nodes) {
                    out = out.bind(g -> {
                        return f.apply(node);
                    });
                }
                return out;
            });
        }
        public <N extends Drop<A, F>> void add(N n) {
            add(Cont.of(n));
        }
        public <N extends Drop<A, F>> void add(Supplier<N> n) {
            add(Cont.of(n));
        }
        public <N extends Drop<A, F>> void add(Cont<N> node) {
            c = c.bind(graph -> node.bind(n -> {
                graph.add(n);
                return Cont.of(graph);
            }));
        }
        public Droplets<A, F> filter(BiPredicate<A, F> p) {
            return new Droplets<>(c.map(graph -> graph.filter(p)));
        }
        public void addAll(Droplets<A, F> droplets) {
            c = c.bind(graph -> droplets.c.bind(d -> {
                graph.addAll(d);
                return Cont.of(graph);
            }));
        }

    }

    static class Puddle<A, F> implements Iterable<Drop<A, F>> {
        Collection<Drop<A, F>> nodes;
        Puddle() {
            this.nodes = new HashSet<>();
        }
        Puddle(Collection<? extends Drop<A, F>> nodes) {
            this.nodes = new HashSet<>(nodes.size());
            for (Drop<A ,F> node : nodes) {
                this.nodes.add(node);
            }
        }
        Puddle(Puddle<A, F> other) {
            this.nodes = new HashSet<>(other.nodes);
        }
        public Collection<A> collect() {
            Collection<A> $ = new ArrayList<>(nodes.size());
            for (Drop<A, F> node : nodes) {
                $.add(node.ref);
            }
            return $;
        }
        public <R, S> Puddle<R, S> map(Function<A, R> f, Function<F, S> g) {
            Collection<Drop<R, S>> $ = new HashSet<>();
            for (Drop<A, F> node : nodes) {
                $.add(node.map(f, g));
            }
            return new Puddle<>($);
        }
        public <G> Puddle<A, G> map(Function<F, G> g) {
            Collection<Drop<A, G>> $ = new HashSet<>();
            for (Drop<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new Puddle<>($);
        }
        public <G> Puddle<A, G> map(BiFunction<A, F, G> g) {
            Collection<Drop<A, G>> $ = new HashSet<>();
            for (Drop<A, F> node : nodes) {
                $.add(node.map(g));
            }
            return new Puddle<>($);
        }
        public <B, G> Puddle<B, G> flatmap(BiFunction<A, F, Puddle<B, G>> g) {
            Collection<Drop<B, G>> $ = new HashSet<>();
            for (Drop<A, F> node : nodes) {
                $.addAll(g.apply(node.ref, node.next).nodes);
            }
            return new Puddle<>($);
        }
        public Puddle<A, F> filter(BiPredicate<A, F> p) {
            Collection<Drop<A, F>> $ = new HashSet<>();
            for (Drop<A, F> node : nodes) {
                if (p.test(node.ref, node.next)) {
                    $.add(node);
                }
            }
            return new Puddle<>($);
        }
        public static <A, F> Puddle<A, F> merge(Puddle<A, F> a, Puddle<A, F> b) {
            Collection<Drop<A, F>> out = new HashSet<>(a.nodes.size() + b.nodes.size());
            out.addAll(a.nodes);
            out.addAll(b.nodes);
            return new Puddle<>(out);
        }
        public <N extends Drop<A, F>> void add(N n) {
            nodes.add(n);
        }
        public <Drops extends Puddle<A, F>> void addAll(Drops droplets) {
            nodes.addAll(droplets.nodes);
        }

        @Override
        public java.util.Iterator<Rain.Drop<A,F>> iterator() {
            return nodes.iterator();
        };
    }

    public static class Drop<A, F> {
        static int count = 0;
        int seq;
        A ref;
        F next;
        public Drop(A ref, F next) {
            this.seq = count++;
            this.ref = ref;
            this.next = next;
        }
        public Drop(int seq, A ref, F next) {
            this.seq = seq;
            this.ref = ref;
            this.next = next;
        }
        public Drop(Drop<A, F> node) {
            this.seq = node.seq;
            this.ref = node.ref;
            this.next = node.next;
        }
        public <R, S> Drop<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new Drop<>(seq, f.apply(ref), g.apply(next));
        }
        public <G> Drop<A, G> map(Function<F, G> g) {
            return new Drop<>(seq, ref, g.apply(next));
        }
        public <G> Drop<A, G> map(BiFunction<A, F, G> g) {
            return new Drop<>(seq, ref, g.apply(ref, next));
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Drop)) {
                return false;
            }
            Drop<?, ?> other = (Drop<?, ?>)obj;
            return ref.equals(other.ref) && seq == other.seq;
        }
        @Override
        public int hashCode() {
            return Objects.hash(ref, seq);
        }

        public void accept(BiConsumer<A, F> consumer) {
            consumer.accept(ref, next);
        }

        public SnowFlake freeze() {
            return new SnowFlake();
        }

        public class SnowFlake {
            @Override
            public String toString() {
                return ref.toString();
            }

            @Override
            public int hashCode() {
                return Drop.this.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                return Drop.this.equals(other);
            }

            public A get() {
                return ref;
            }
        }
    }
}
