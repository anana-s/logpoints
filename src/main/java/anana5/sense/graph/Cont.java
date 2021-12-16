package anana5.sense.graph;

import java.util.Map;
import java.util.WeakHashMap;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Cont<T> {

    App app(Callback<T> k);

    static <T> Cont<T> of(T t) {
        return new Pure<>(() -> t);
    }

    static <T> Cont<T> of(Supplier<T> supplier) {
        return new Pure<>(supplier);
    }

    static class Pure<R> implements Cont<R> {
        Supplier<R> r;
        Pure(Supplier<R> r) {
            this.r = r;
        }

        @Override
        public App app(Callback<R> k) {
            return App.apply(k, r.get());
        }
    }

    default <R> Cont<R> map(Function<T, R> f) {
        return new Mapping<>(f, this);
    }

    static class Mapping<T, R> implements Cont<R> {
        Function<T, R> f;
        Cont<T> c;
        Mapping(Function<T, R> f, Cont<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public App app(Callback<R> k) {
            return new App.Builder<>(c, k.map(f));
        }
    }

    default <S> Cont<S> bind(Function<T, Cont<S>> f) {
        return new Binding<T, S>(f, this);
    }

    static class Binding<T, R> implements Cont<R> {
        Function<T, Cont<R>> f;
        Cont<T> c;
        Binding(Function<T, Cont<R>> f, Cont<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public App app(Callback<R> k) {
            return new App.Builder<>(c, k.bind(f));
        }
    }

    interface Callback<T> {
        <S> App app(T t);

        static <T> Callback<T> of(Consumer<T> f) {
            return new Pure<>(f);
        }

        static class Pure<T> implements Callback<T> {
            Consumer<T> f;
            Pure(Consumer<T> f) {
                this.f = f;
            }
            @Override
            public <S> App app(T t) {
                f.accept(t);
                return null;
            }
        }

        default <S> Callback<S> map(Function<S, T> f) {
            return new Mapping<>(f, this);
        }

        static class Mapping<T, S> implements Callback<T> {
            Function<T, S> f;
            Callback<S> k;
            Mapping(Function<T, S> f, Callback<S> k) {
                this.f = f;
                this.k = k;
            }
            public <U> App app(T t) {
                return App.apply(k, f.apply(t));
            };
        }

        default <S> Callback<S> bind(Function<S, Cont<T>> f) {
            return new Binding<>(f, this);
        }

        static class Binding<T, S> implements Callback<T> {
            Function<T, Cont<S>> f;
            Callback<S> k;
            Binding(Function<T, Cont<S>> f, Callback<S> k) {
                this.f = f;
                this.k = k;
            }
            @Override
            public <U> App app(T t) {
                return App.build(f.apply(t), k);
            }
        }
    }

    interface App {
        App app();

        static <T> App build(Cont<T> c, Callback<T> k) {
            return new Builder<T>(c, k);
        }
        static class Builder<T> implements App {
            Cont<T> c;
            Callback<T> k;
            Builder(Cont<T> c,  Callback<T> k) {
                this.c = c;
                this.k = k;
            }
            @Override
            public App app() {
                return c.app(k);
            }
        }
    
        static <T> App apply(Callback<T> k, T t) {
            return new Apply<>(k, t);
        }
        static class Apply<T> implements App {
            Callback<T> k;
            T t;
            Apply(Callback<T> k, T t) {
                this.k = k;
                this.t = t;
            }
            @Override
            public App app() {
                return k.app(t);
            }
        }
    }

    default T run() {
        var result = new Object() { T value; };
        App a = this.app(Callback.of(t -> result.value = t));
        while (a != null) {
            a = a.app();
        }
        return result.value;
    }
}
