package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Rainfall<T> {

    Rain<T, Rainfall<T>> rain;
    
    public static <T> Rainfall<T> from(Supplier<? extends Collection<? extends Droplet<T, Rainfall<T>>>> supplier) {
        return new Rainfall<>(Rain.from(supplier));
    }

    public Rainfall(Rain<T, Rainfall<T>> dnodes) {
        this.rain = dnodes;
    }

    public Rainfall(Rainfall<T> other) {
        this(other.rain);
    }

    public Rainfall() {
        this(new Rain<>());
    }

    public Rain<T, Rainfall<T>> unfix() {
        return rain;
    } 

    public <G extends Rain<T, Rainfall<T>>> G unfix(Class<G> cls) {
        return cls.cast(rain);
    } 

    public Cont<Collection<Droplet<T, Rainfall<T>>.SnowFlake>> collect() {
        return rain.collect();
    }

    public <R> R fold(Function<Rain<T, R>, ? extends R> f) {
        return f.apply(rain.map(droplet -> droplet.map((drop, let) -> let.fold(f))));
    }

    public static <T, S> Rainfall<T> unfold(Function<S, ? extends Rain<T, S>> f, S s) {
        return new Rainfall<>(f.apply(s).map(droplet -> droplet.map((drop, let) -> unfold(f, let))));
    }

    public static <T, S> Rainfall<T> unfold(S s, Function<S, ? extends Rain<T, S>> f) {
        return unfold(f, s);
    }

    public Rainfall<T> filter(Predicate<Droplet<T, Rainfall<T>>.SnowFlake> p) {
        HashMap<Droplet<T, Rainfall<T>>, Puddle<T, Rainfall<T>>> cache = new HashMap<>();
        return fold(rain -> new Rainfall<>(rain.flatmap(droplet -> {
            if (cache.containsKey(droplet)) {
                Puddle<T, Rainfall<T>> puddle = cache.get(droplet);
                if (puddle == null) {
                    return Rain.of();
                }
                return Rain.of(puddle);
            }

            cache.put(droplet, null);

            if (p.test(droplet.freeze())) {
                Puddle<T, Rainfall<T>> out = Puddle.of(droplet);
                cache.put(droplet, out);
                return Rain.of(out);
            } else {
                return droplet.let.rain.then(puddle -> cache.put(droplet, puddle));
            }
        })));
    }

    public <R> Rainfall<R> map(Function<T, R> f) {
        return unfold(this, rainfall -> rainfall.rain.map(droplet -> droplet.map(f)));
    }

    public Cont<Void> traverse(BiConsumer<Droplet<T, Rainfall<T>>.SnowFlake, Collection<Droplet<T, Rainfall<T>>.SnowFlake>> visitor) {
        return traverse(null, visitor, new HashSet<>());
    }

    private Cont<Void> traverse(Droplet<T, Rainfall<T>> parent, BiConsumer<Droplet<T, Rainfall<T>>.SnowFlake, Collection<Droplet<T, Rainfall<T>>.SnowFlake>> visitor, Set<Droplet<T, Rainfall<T>>> visited) {
        return rain.c.bind(
            puddle -> {
                Cont<Void> traversal = Cont.of(() -> null);
                for (Droplet<T, Rainfall<T>> node : puddle.droplets) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        traversal = traversal.bind(v -> node.let.traverse(node, visitor, visited));
                    }
                }
                if (parent != null) {
                    visitor.accept(parent.freeze(), puddle.collect());
                } else {
                    visitor.accept(null, puddle.collect());
                }
                return traversal;
            }
        );
    }

    public static <T> Rainfall<T> merge(Collection<Rainfall<T>> jungles) {
        Cont<Puddle<T, Rainfall<T>>> $ = Cont.of(() -> new Puddle<>());
        for (Rainfall<T> jungle : jungles) {
            $ = $.bind(a -> jungle.rain.c.bind(b -> Cont.of(Puddle.merge(a, b))));
        }
        return new Rainfall<>(new Rain<>($));
    }

    public Rainfall<T> then(Consumer<Puddle<T, Rainfall<T>>> f) {
        return new Rainfall<>(rain.then(f));
    }

    @Override
    public int hashCode() {
        return Objects.hash(rain);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rainfall)) {
            return false;
        }
        Rainfall<?> other = (Rainfall<?>)obj;
        return rain.equals(other.rain);
    }

    public static class Rain<A ,F> {
        Cont<Puddle<A, F>> c;
        private Rain(Cont<? extends Puddle<A, F>> cgraph) {
            this.c = cgraph.map(graph -> (Puddle<A, F>)graph);
        }
        public Rain(Rain<A, F> cgraph) {
            this.c = cgraph.c;
        }
        public Rain(Puddle<A, F> graph) {
            this(Cont.of(() -> graph));
        }
        public Rain(Collection<? extends Droplet<A, F>> nodes) {
            this(new Puddle<>(nodes));
        }
        @SafeVarargs
        public Rain(Droplet<A, F>... droplets) {
            this(Arrays.asList(droplets));
        }
        public Rain() {
            this(new Puddle<>());
        }
        @SafeVarargs
        static <A, F> Rain<A, F> of (Droplet<A, F>... droplets) {
            return new Rain<>(droplets);
        }
        static <A, F> Rain<A, F> of (Puddle<A, F> puddle) {
            return new Rain<>(puddle);
        }
        static <A, F> Rain<A, F> from(Supplier<? extends Collection<? extends Droplet<A, F>>> nodes) {
            return new Rain<>(Cont.of(nodes).map(Puddle::new));
        }
        public Cont<Collection<Droplet<A,F>.SnowFlake>> collect() {
            return c.map(Puddle::collect);
        }
        public <B, G> Rain<B, G> map(Function<Droplet<A, F>, Droplet<B, G>> g) {
            return new Rain<>(c.map(graph -> graph.map(g)));
        }
        public <B, G> Rain<B, G> flatmap(Function<Droplet<A, F>, Rain<B, G>> f) {
            return new Rain<>(c.bind(graph -> {
                Cont<Puddle<B, G>> out = Cont.of(() -> new Puddle<>());
                for (var node : graph.droplets) {
                    out = out.bind(a -> f.apply(node).c.map(b -> {
                        a.addAll(b);
                        return a;
                    }));
                }
                return out;
            }));
        }
        public <T> Cont<T> bind(Function<Droplet<A, F>, Cont<T>> f) {
            return c.bind(graph -> {
                Cont<T> out = Cont.of(() -> null);
                for (var node : graph.droplets) {
                    out = out.bind(g -> {
                        return f.apply(node);
                    });
                }
                return out;
            });
        }
        public <N extends Droplet<A, F>> void add(N n) {
            add(Cont.of(n));
        }
        public <N extends Droplet<A, F>> void add(Supplier<N> n) {
            add(Cont.of(n));
        }
        public <N extends Droplet<A, F>> void add(Cont<N> node) {
            c = c.bind(graph -> node.bind(n -> {
                graph.add(n);
                return Cont.of(graph);
            }));
        }
        public Rain<A, F> filter(BiPredicate<A, F> p) {
            return new Rain<>(c.map(graph -> graph.filter(p)));
        }
        public void addAll(Rain<A, F> droplets) {
            c = c.bind(graph -> droplets.c.bind(d -> {
                graph.addAll(d);
                return Cont.of(graph);
            }));
        }

        public Rain<A, F> then(Consumer<Puddle<A, F>> f) {
            return new Rain<>(c.map(puddle -> {
                f.accept(puddle);
                return puddle;
            }));
        }

        @Override
        public int hashCode() {
            return Objects.hash(c);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Rain)) {
                return false;
            }
            Rain<?, ?> other = (Rain<?, ?>)obj;
            return c.equals(other.c);
        }

    }

    static class Puddle<A, F> implements Iterable<Droplet<A, F>> {
        Collection<Droplet<A, F>> droplets;
        Puddle() {
            this.droplets = new HashSet<>();
        }
        Puddle(Collection<? extends Droplet<A, F>> nodes) {
            this.droplets = new HashSet<>(nodes.size());
            for (Droplet<A ,F> node : nodes) {
                this.droplets.add(node);
            }
        }
        Puddle(Puddle<A, F> other) {
            this.droplets = new HashSet<>(other.droplets);
        }
        @SafeVarargs
        public Puddle(Droplet<A, F>... droplets) {
            this(Arrays.asList(droplets));
        }
        @SafeVarargs
        public static <A, F> Puddle<A, F> of (Droplet<A, F>... droplets) {
            return new Puddle<>(droplets);
        }
        public Collection<Droplet<A,F>.SnowFlake> collect() {
            Collection<Droplet<A,F>.SnowFlake> out = new ArrayList<>(droplets.size());
            for (Droplet<A, F> droplet : droplets) {
                out.add(droplet.freeze());
            }
            return out;
        }
        public <B, G> Puddle<B, G> map(Function<Droplet<A, F>, Droplet<B, G>> g) {
            Collection<Droplet<B, G>> $ = new HashSet<>();
            for (Droplet<A, F> node : droplets) {
                $.add(g.apply(node));
            }
            return new Puddle<>($);
        }
        public <B, G> Puddle<B, G> flatmap(BiFunction<A, F, Puddle<B, G>> g) {
            Collection<Droplet<B, G>> $ = new HashSet<>();
            for (Droplet<A, F> node : droplets) {
                $.addAll(g.apply(node.drop, node.let).droplets);
            }
            return new Puddle<>($);
        }
        public Puddle<A, F> filter(BiPredicate<A, F> p) {
            Collection<Droplet<A, F>> $ = new HashSet<>();
            for (Droplet<A, F> node : droplets) {
                if (p.test(node.drop, node.let)) {
                    $.add(node);
                }
            }
            return new Puddle<>($);
        }
        public static <A, F> Puddle<A, F> merge(Puddle<A, F> a, Puddle<A, F> b) {
            Collection<Droplet<A, F>> out = new HashSet<>(a.droplets.size() + b.droplets.size());
            out.addAll(a.droplets);
            out.addAll(b.droplets);
            return new Puddle<>(out);
        }
        public <N extends Droplet<A, F>> void add(N n) {
            droplets.add(n);
        }
        public void addAll(Puddle<A, F> puddle) {
            droplets.addAll(puddle.droplets);
        }

        @Override
        public java.util.Iterator<Rainfall.Droplet<A,F>> iterator() {
            return droplets.iterator();
        };

        @Override
        public int hashCode() {
            return Objects.hash(droplets);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Puddle)) {
                return false;
            }
            Puddle<?, ?> other = (Puddle<?, ?>)obj;
            return droplets.equals(other.droplets);
        }
    }

    public static class Droplet<A, F> {
        static int count = 0;
        private int seq;
        private A drop;
        private F let;
        public Droplet(A ref, F next) {
            this.seq = count++;
            this.drop = ref;
            this.let = next;
        }
        public Droplet(int seq, A ref, F next) {
            this.seq = seq;
            this.drop = ref;
            this.let = next;
        }
        public Droplet(Droplet<A, F> node) {
            this.seq = node.seq;
            this.drop = node.drop;
            this.let = node.let;
        }
        public <R, S> Droplet<R, S> map(Function<A, R> f, Function<F, S> g) {
            return new Droplet<>(seq, f.apply(drop), g.apply(let));
        }
        public <B> Droplet<B, F> map(Function<A, B> g) {
            return new Droplet<>(seq, g.apply(drop), let);
        }
        public <G> Droplet<A, G> map(BiFunction<A, F, G> g) {
            return new Droplet<>(seq, drop, g.apply(drop, let));
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Droplet)) {
                return false;
            }
            Droplet<?, ?> other = (Droplet<?, ?>)obj;
            return drop.equals(other.drop) && seq == other.seq;
        }
        @Override
        public int hashCode() {
            return Objects.hash(drop, seq);
        }

        public void accept(BiConsumer<A, F> consumer) {
            consumer.accept(drop, let);
        }

        public SnowFlake freeze() {
            return new SnowFlake();
        }

        public class SnowFlake {
            @Override
            public String toString() {
                return drop.toString();
            }

            @Override
            public int hashCode() {
                return Droplet.this.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                return Droplet.this.equals(other);
            }

            public A get() {
                return drop;
            }

            public <B> B get(Function<A, B> f) {
                return f.apply(drop);
            }
        }
    }
}
