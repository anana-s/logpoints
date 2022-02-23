package anana5.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Computation<T> {

    Continuation accept(Callback<T> k);

    static <T> Computation<T> nil() {
        return new Nil<>();
    }

    static class Nil<T> implements Computation<T> {
        @Override
        public Continuation accept(Callback<T> k) {
            return Continuation.apply(k, null);
        }
    }

    static <T> Computation<T> just(T t) {
        return new Just<>(t);
    }

    static class Just<T> implements Computation<T> {
        final T t;
        Just(T t) {
            this.t = t;
        }

        @Override
        public Continuation accept(Callback<T> k) {
            return Continuation.apply(k, t);
        }
    }

    static <T> Pure<T> pure(Supplier<T> supplier) {
        return new Pure<>(supplier);
    }

    static class Pure<R> implements Computation<R> {
        final Supplier<R> r;
        Pure(Supplier<R> r) {
            this.r = r;
        }

        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.apply(k, r.get());
        }
    }

    default <R> Computation<R> map(Function<T, R> f) {
        return new Mapping<>(f, this);
    }

    static class Mapping<T, R> implements Computation<R> {
        final Function<T, R> f;
        final Computation<T> c;
        Mapping(Function<T, R> f, Computation<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.accept(c, k.map(f));
        }
    }

    default <R> Computation<R> apply(Computation<Function<T, R>> f) {
        return new Application<>(f, this);
    }

    static class Application<T, R> implements Computation<R> {
        final Computation<Function<T, R>> f;
        final Computation<T> c;
        Application(Computation<Function<T, R>> f, Computation<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.accept(c, k.apply(f));
        }
    }

    default <S> Computation<S> bind(Function<T, Computation<S>> f) {
        return new Binding<T, S>(f, this);
    }

    static class Binding<T, R> implements Computation<R> {
        Function<T, Computation<R>> f;
        Computation<T> c;
        Binding(Function<T, Computation<R>> f, Computation<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return new Continuation.Visit<>(c, k.bind(f));
        }
    }

    interface Callback<T> {
        Continuation accept(T t);

        static <T> Callback<T> pure(Consumer<T> f) {
            return new Pure<>(f);
        }

        static class Pure<T> implements Callback<T> {
            final Consumer<T> f;
            Pure(Consumer<T> f) {
                this.f = f;
            }
            @Override
            public Continuation accept(T t) {
                f.accept(t);
                return null;
            }
        }

        default Callback<T> then(Consumer<T> f) {
            return new SideEffect<>(f, this);
        }

        static class SideEffect<T> implements Callback<T> {
            final Consumer<T> f;
            final Callback<T> k;
            SideEffect(Consumer<T> f, Callback<T> k) {
                this.f = f;
                this.k = k;
            }
            @Override
            public Continuation accept(T t) {
                f.accept(t);
                return Continuation.apply(k, t);
            }
        }

        default <S> Callback<S> map(Function<S, T> f) {
            return new Mapping<>(f, this);
        }

        static class Mapping<T, S> implements Callback<T> {
            final Function<T, S> f;
            final Callback<S> k;
            Mapping(Function<T, S> f, Callback<S> k) {
                this.f = f;
                this.k = k;
            }
            public Continuation accept(T t) {
                return Continuation.apply(k, f.apply(t));
            };
        }

        default <R> Callback<R> apply(Computation<Function<R, T>> f) {
            return new Application<>(f, this);
        }

        static class Application<T, R> implements Callback<T> {
            final Computation<Function<T, R>> f;
            final Callback<R> k;
            Application(Computation<Function<T, R>> f, Callback<R> k) {
                this.f = f;
                this.k = k;
            }
            @Override
            public Continuation accept(T t) {
                return Continuation.accept(f.map(f$ -> f$.apply(t)), k);
            }
        }

        default <S> Callback<S> bind(Function<S, Computation<T>> f) {
            return new Binding<>(f, this);
        }

        static class Binding<T, S> implements Callback<T> {
            final Function<T, Computation<S>> f;
            final Callback<S> k;
            Binding(Function<T, Computation<S>> f, Callback<S> k) {
                this.f = f;
                this.k = k;
            }
            @Override
            public Continuation accept(T t) {
                return Continuation.accept(f.apply(t), k);
            }
        }
    }

    interface Continuation {
        Continuation next();

        static <T> Continuation accept(Computation<T> c, Callback<T> k) {
            return new Visit<T>(c, k);
        }
        static class Visit<T> implements Continuation {
            final Computation<T> c;
            final Callback<T> k;
            Visit(Computation<T> c,  Callback<T> k) {
                this.c = c;
                this.k = k;
            }
            @Override
            public Continuation next() {
                return c.accept(k);
            }
        }
    
        static <T> Continuation apply(Callback<T> k, T t) {
            return new Application<>(k, t);
        }
        static class Application<T> implements Continuation {
            final Callback<T> k;
            final T t;
            Application(Callback<T> k, T t) {
                this.k = k;
                this.t = t;
            }
            @Override
            public Continuation next() {
                return k.accept(t);
            }
        }
    }

    default T run() {
        var result = new Object() { T value; };
        run(t -> result.value = t);
        return result.value;
    }

    default void run(Consumer<T> f) {
        Continuation a = this.accept(Callback.pure(f));
        while (a != null) {
            a = a.next();
            statistics.iterations++;
        }
    }

    static Statistics statistics = new Statistics();
    static class Statistics {
        int iterations = 0;
        public int iterations() {
            return statistics.iterations;
        }
    }
}
