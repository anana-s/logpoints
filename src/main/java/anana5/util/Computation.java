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
        return new JustValue<>(t);
    }

    static Computation<Void> just(Runnable runnable) {
        return new JustRunnable(runnable);
    }

    static <T> Computation<T> just(Supplier<T> supplier) {
        return new JustSupplier<>(supplier);
    }

    static class JustValue<T> implements Computation<T> {
        final T t;
        JustValue(T t) {
            this.t = t;
        }

        @Override
        public Continuation accept(Callback<T> k) {
            return Continuation.apply(k, t);
        }
    }

    static class JustRunnable implements Computation<Void> {
        final Runnable runnable;
        JustRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Continuation accept(Callback<Void> k) {
            runnable.run();
            return Continuation.apply(k, null);
        }
    }

    static class JustSupplier<T> implements Computation<T> {
        final Supplier<T> r;
        JustSupplier(Supplier<T> r) {
            this.r = r;
        }

        @Override
        public Continuation accept(Callback<T> k) {
            return Continuation.apply(k, r.get());
        }
    }

    default Computation<T> effect(Consumer<? super T> consumer) {
        return map(t -> {
            consumer.accept(t);
            return t;
        });
    }

    default <R> Computation<R> map(Function<? super T, ? extends R> f) {
        return new Mapping<>(f, this);
    }

    static class Mapping<T, R> implements Computation<R> {
        final Function<? super T, ? extends R> f;
        final Computation<? extends T> c;
        Mapping(Function<? super T, ? extends R> f, Computation<? extends T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.accept(c, k.map(f));
        }
    }

    default <R> Computation<R> apply(Computation<? extends Function<? super T, ? extends R>> f) {
        return new Application<>(f, this);
    }

    static class Application<T, R> implements Computation<R> {
        final Computation<? extends Function<? super T, ? extends R>> f;
        final Computation<? extends T> c;
        Application(Computation<? extends Function<? super T, ? extends R>> f, Computation<? extends T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.accept(c, k.apply(f));
        }
    }

    default <S> Computation<S> bind(Function<? super T, ? extends Computation<S>> f) {
        return new Binding<T, S>(f, this);
    }

    static class Binding<T, R> implements Computation<R> {
        Function<? super T, ? extends Computation<R>> f;
        Computation<? extends T> c;
        Binding(Function<? super T, ? extends Computation<R>> f, Computation<? extends T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<R> k) {
            return Continuation.accept(c, k.bind(f));
        }
    }

    interface Callback<S> {
        Continuation accept(S t);

        static <S> Callback<S> pure(Consumer<? super S> f) {
            return new Pure<>(f);
        }

        static class Pure<S> implements Callback<S> {
            final Consumer<? super S> f;
            Pure(Consumer<? super S> f) {
                this.f = f;
            }
            @Override
            public Continuation accept(S t) {
                f.accept(t);
                return null;
            }
        }

        default <T> Callback<T> map(Function<? super T, ? extends S> f) {
            return new Mapping<>(f, this);
        }

        static class Mapping<T, S> implements Callback<T> {
            final Function<? super T, ? extends S> f;
            final Callback<? super S> k;
            Mapping(Function<? super T, ? extends S> f, Callback<? super S> k) {
                this.f = f;
                this.k = k;
            }
            public Continuation accept(T t) {
                return Continuation.apply(k, f.apply(t));
            };
        }

        default <R> Callback<R> apply(Computation<? extends Function<? super R, ? extends S>> f) {
            return new Application<>(f, this);
        }

        static class Application<T, R> implements Callback<T> {
            final Computation<? extends Function<? super T, ? extends R>> f;
            final Callback<? super R> k;
            Application(Computation<? extends Function<? super T, ? extends R>> f, Callback<? super R> k) {
                this.f = f;
                this.k = k;
            }
            @Override
            public Continuation accept(T t) {
                return Continuation.accept(f.map(f$ -> f$.apply(t)), k);
            }
        }

        default <T> Callback<T> bind(Function<? super T, ? extends Computation<S>> f) {
            return new Binding<>(f, this);
        }

        static class Binding<T, S> implements Callback<T> {
            final Function<? super T, ? extends Computation<S>> f;
            final Callback<S> k;
            Binding(Function<? super T, ? extends Computation<S>> f, Callback<S> k) {
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
    
        static <T> Continuation apply(Callback<? super T> k, T t) {
            return new Application<>(k, t);
        }
        static class Application<T> implements Continuation {
            final Callback<? super T> k;
            final T t;
            Application(Callback<? super T> k, T t) {
                this.k = k;
                this.t = t;
            }
            @Override
            public Continuation next() {
                return k.accept(t);
            }
        }
    }

    default void run(Consumer<? super T> f) {
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
