package anana5.sense.graph;

import java.util.Map;
import java.util.WeakHashMap;

import java.util.function.Consumer;
import java.util.function.Function;

public interface C<T> {

    App app(Callback<T> k);

    static <T> C<T> of(T t) {
        return new Pure<>(t);
    }

    static class Pure<R> implements C<R> {
        R r;
        Pure(R r) {
            this.r = r;
        }

        @Override
        public App app(Callback<R> k) {
            return App.apply(k, r);
        }
    }

    default <R> C<R> map(Function<T, R> f) {
        return new Mapping<>(f, this);
    }

    static class Mapping<T, R> implements C<R> {
        Function<T, R> f;
        C<T> c;
        Mapping(Function<T, R> f, C<T> c) {
            this.f = f;
            this.c = c;
        }
        @Override
        public App app(Callback<R> k) {
            return new App.Builder<>(c, k.map(f));
        }
    }

    default <S> C<S> bind(Function<T, C<S>> f) {
        return new Binding<T, S>(f, this);
    }

    static class Binding<T, R> implements C<R> {
        Function<T, C<R>> f;
        C<T> c;
        Binding(Function<T, C<R>> f, C<T> c) {
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

        default <S> Callback<S> bind(Function<S, C<T>> f) {
            return new Binding<>(f, this);
        }

        static class Binding<T, S> implements Callback<T> {
            Function<T, C<S>> f;
            Callback<S> k;
            Binding(Function<T, C<S>> f, Callback<S> k) {
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

        static <T> App build(C<T> c, Callback<T> k) {
            return new Builder<T>(c, k);
        }
        static class Builder<T> implements App {
            C<T> c;
            Callback<T> k;
            Builder(C<T> c,  Callback<T> k) {
                this.k = k;
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


    /**
     * run computation
     */
    static Map<C<?>, Object> cache = new WeakHashMap<>();
    
    @SuppressWarnings({ "unchecked" })
    default T compute() {
        return (T)cache.computeIfAbsent(this, C::force);
    }

    default T force() {
        var result = new Object() { T value; };
        App a = this.app(Callback.of(t -> result.value = t));
        while (a != null) {
            a = a.app();
        }
        return result.value;
    }
}
