package anana5.sense.graph;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Rainfall<T> {

    final Rain<T, Rainfall<T>> rain;
    
    @SafeVarargs
    public Rainfall(Supplier<Droplet<T, Rainfall<T>>>... suppliers) {
        this.rain = new Rain<>(suppliers);
    }

    public Rainfall(Rain<T, Rainfall<T>> rain) {
        this.rain = rain;
    }

    public Rainfall(Rainfall<T> other) {
        this.rain = other.rain;
    }

    @SafeVarargs
    public static <T> Rainfall<T> of(Supplier<Droplet<T, Rainfall<T>>>... suppliers) {
        return new Rainfall<>(suppliers);
    }

    public static <T> Rainfall<T> of(Rain<T, Rainfall<T>> rain) {
        return new Rainfall<>(rain);
    }

    public Rain<T, Rainfall<T>> unfix() {
        return rain;
    } 

    public <G extends Rain<T, Rainfall<T>>> G unfix(Class<G> cls) {
        return cls.cast(rain);
    } 

    public Promise<Collection<Droplet<T, Rainfall<T>>.SnowFlake>> collect() {
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
        Set<Droplet<T, Rainfall<T>>> seen = new HashSet<>();
        return fold(rain -> new Rainfall<>(rain.flatmap(droplet -> {
            if (seen.contains(droplet)) {
                return new Rain<>();
            }

            if (p.test(droplet.freeze())) {
                Rain<T, Rainfall<T>> out = new Rain<>(() -> droplet);
                return out;
            } else {
                seen.add(droplet);
                droplet.let.rain.promise = droplet.let.rain.promise.then($ -> seen.remove(droplet));
                return droplet.let.rain;
            }
        })));
    }

    public <R> Rainfall<R> map(Function<T, R> f) {
        return unfold(this, rainfall -> rainfall.rain.map(droplet -> droplet.map(f)));
    }

    public Promise<Void> traverse(BiConsumer<Droplet<T, Rainfall<T>>.SnowFlake, Collection<Droplet<T, Rainfall<T>>.SnowFlake>> visitor) {
        return traverse(null, visitor, new HashSet<>());
    }

    private Promise<Void> traverse(Droplet<T, Rainfall<T>> parent, BiConsumer<Droplet<T, Rainfall<T>>.SnowFlake, Collection<Droplet<T, Rainfall<T>>.SnowFlake>> visitor, Set<Droplet<T, Rainfall<T>>> visited) {
        return rain.promise.bind(puddle -> {
            Promise<Void> traversal = new Promise<>(() -> null);
            for (Droplet<T, Rainfall<T>> node : puddle) {
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
        });
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Rainfall<T> merge(Rainfall<T>... rainfalls) {
        ArrayList<Rain<T, Rainfall<T>>> rains = new ArrayList<>(); 
        for (Rainfall<T> rainfall : rainfalls) {
            rains.add(rainfall.rain);
        }
        return Rainfall.of(Rain.merge(rains.toArray(new Rain[rains.size()])));
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
        static int count = 0;
        final int seq = 0;
        Promise<Puddle<A, F>> promise;
        public Rain(Promise<Puddle<A, F>> promise) {
            this(count++, promise);
        }
        @SafeVarargs
        public Rain(Supplier<Droplet<A, F>>... suppliers) {
            this(count++, suppliers);
        }
        public Rain(int seq, Promise<Puddle<A, F>> promise) {
            this.promise = promise;
        }
        public Rain(int seq, Rain<A, F> rain) {
            this.promise = rain.promise;
        }
        @SafeVarargs
        public Rain(int seq, Supplier<Droplet<A, F>>... suppliers) {
            this.promise = new Promise<>(() -> new Puddle<>());
            for (Supplier<Droplet<A, F>> supplier : suppliers) {
                this.promise = promise.map(puddle -> {
                    puddle.add(supplier.get());
                    return puddle;
                });
            }
        }
        public Promise<Collection<Droplet<A,F>.SnowFlake>> collect() {
            return promise.map(Puddle::collect);
        }
        public <B, G> Rain<B, G> map(Function<Droplet<A, F>, Droplet<B, G>> g) {
            return new Rain<>(seq, promise.map(graph -> graph.map(g)));
        }
        public <B, G> Rain<B, G> flatmap(Function<Droplet<A, F>, Rain<B, G>> f) {
            return new Rain<>(seq, promise.bind(puddle -> {
                Promise<Puddle<B, G>> out = new Promise<>(() -> new Puddle<>());
                for (var node : puddle) {
                    out = out.bind(a -> f.apply(node).promise.map(b -> {
                        a.addAll(b);
                        return a;
                    }));
                }
                return out;
            }));
        }

        public Rain<A, F> filter(BiPredicate<A, F> p) {
            return new Rain<>(seq, promise.map(graph -> graph.filter(p)));
        }

        @SafeVarargs
        public static <A, F> Rain<A, F> merge(Rain<A, F>... rains) {
            Promise<Puddle<A, F>> c = new Promise<>(() -> new Puddle<>());
            for (Rain<A ,F> rain : rains) {
                c = c.bind(puddle -> rain.promise.bind(puddle$ -> {
                    puddle.addAll(puddle$);
                    return new Promise<>(() -> puddle);
                }));
            }
            return new Rain<>(c);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seq);
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
            return seq == other.seq;
        }

    }

    public static class Puddle<A, F> extends HashSet<Droplet<A, F>> {
        public Puddle() {
            super();
        }
        public Puddle(Collection<? extends Droplet<A, F>> nodes) {
            super(nodes);
        }
        public Puddle(int size) {
            super(size);
        }
        @SafeVarargs
        Puddle(Droplet<A, F>... droplets) {
            this(Arrays.asList(droplets));
        }
        @SafeVarargs
        public static <A, F> Puddle<A, F> of(Droplet<A, F>... droplets) {
            return new Puddle<>(droplets);
        }
        public Collection<Droplet<A,F>.SnowFlake> collect() {
            Collection<Droplet<A,F>.SnowFlake> out = new ArrayList<>(size());
            for (Droplet<A, F> droplet : this) {
                out.add(droplet.freeze());
            }
            return out;
        }
        public <B, G> Puddle<B, G> map(Function<Droplet<A, F>, Droplet<B, G>> g) {
            Puddle<B, G> out = new Puddle<>(size());
            for (Droplet<A, F> node : this) {
                out.add(g.apply(node));
            }
            return out;
        }
        public <B, G> Puddle<B, G> flatmap(Function<Droplet<A, F>, Puddle<B, G>> g) {
            Puddle<B, G> out = new Puddle<>();
            for (Droplet<A, F> node : this) {
                out.addAll(g.apply(node));
            }
            return out;
        }
        public Puddle<A, F> filter(BiPredicate<A, F> p) {
            Puddle<A, F> out = new Puddle<>();
            for (Droplet<A, F> node : this) {
                if (p.test(node.drop, node.let)) {
                    out.add(node);
                }
            }
            return out;
        }
        @SafeVarargs
        public static <A, F> Puddle<A, F> merge(Collection<Droplet<A, F>>... puddles) {
            int size = 0;
            for (Collection<Droplet<A, F>> puddle : puddles) {
                size += puddle.size();
            }
            Puddle<A, F> out = new Puddle<>(size);
            for (Collection<Droplet<A, F>> puddle : puddles) {
                out.addAll(puddle);
            }
            return out;
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
            return seq == other.seq;
        }
        @Override
        public int hashCode() {
            return Objects.hash(seq);
        }

        public void accept(BiConsumer<A, F> consumer) {
            consumer.accept(drop, let);
        }

        public SnowFlake freeze() {
            return new SnowFlake();
        }

        public String toString() {
            return freeze().toString();
        }

        public class SnowFlake {
            @Override
            public String toString() {
                return id() + " " + drop.toString();
            }

            @Override
            public int hashCode() {
                return Objects.hash(unfreeze());
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof Droplet.SnowFlake)) {
                    return false;
                }
                Droplet<?, ?>.SnowFlake other = (Droplet<?, ?>.SnowFlake)obj;
                return unfreeze().equals(other.unfreeze());
            }

            public A get() {
                return drop;
            }

            public <B> B get(Function<A, B> f) {
                return f.apply(drop);
            }

            public String id() {
                return Integer.toHexString(seq);
            }

            private Droplet<A, F> unfreeze() {
                return Droplet.this;
            }
        }
    }
}
