package anana5.sense.graph;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.base.Supplier;

@FunctionalInterface
public interface D<T> extends Supplier<T> {
    T compute();

    Map<D<?>, Object> cache = new WeakHashMap<>();

    default T get() {
        @SuppressWarnings("unchecked")
        T cached = (T)cache.computeIfAbsent(this, D::compute);
        return cached;
    }

    static <R> D<R> pure(R t) {
        return new Pure<>(t);
    }

    static <K, R> D<R> pure(K k, Function<K, R> f) {
        return new KeyedPure<>(k, f);
    }

    default <R> D<R> map(Function<T, R> f) {
        return new Mapping<>(this, f);
    }

    default <K, R> D<R> map(K k, BiFunction<K, T, R> f) {
        return new KeyedMapping<K, T, R>(k, this, f);
    }

    default <R> D<R> apply(D<Function<T, R>> f) {
        return new Application<>(this, f);
    }

    default <R> D<R> bind(Function<T, D<R>> f) {
        return new Binding<>(this, f);
    }

    default D<Void> discard() {
        return () -> null;
    }

    class Pure<T> implements D<T> {
        T t;
        Pure(T t){
            this.t = t;
        }
        @Override
        public T compute() {
            return t;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Pure)) {
                return false;
            }
            Pure<?> other = (Pure<?>)obj;
            return t.equals(other.t);
        }
        @Override
        public int hashCode() {
            return Objects.hash(t);
        }
    }

    class KeyedPure<K, R> implements D<R> {
        K k;
        Function<K, R> f;
        KeyedPure(K k, Function<K, R> f) {
            this.k = k;
            this.f = f;
        }
        @Override
        public R compute() {
            return f.apply(k);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyedPure)) {
                return false;
            }
            KeyedPure<?, ?> other = (KeyedPure<?, ?>)obj;
            return k.equals(other.k);
        }
        @Override
        public int hashCode() {
            return Objects.hash(k);
        }
    }

    class Mapping<T, R> implements D<R> {
        D<T> t;
        Function<T, R> f;
        Mapping(D<T> t, Function<T, R> f) {
            this.t = t;
            this.f = f;
        }
        @Override
        public R compute() {
            return f.apply(t.get());
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Mapping)) {
                return false;
            }
            Mapping<?, ?> other = (Mapping<?, ?>)obj;
            return t.equals(other.t) && f.equals(other.f);
        }
        @Override
        public int hashCode() {
            return Objects.hash(t, f);
        }
    }

    class KeyedMapping<K, T, R> implements D<R> {
        K k;
        D<T> t;
        BiFunction<K, T, R> f;
        KeyedMapping(K k, D<T> t, BiFunction<K, T, R> f) {
            this.k = k;
            this.t = t;
            this.f = f;
        }
        @Override
        public R compute() {
            return f.apply(k, t.get());
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyedMapping)) {
                return false;
            }
            KeyedMapping<?, ?, ?> other = (KeyedMapping<?, ?, ?>)obj;
            return t.equals(other.t) && k.equals(other.k);
        }
        @Override
        public int hashCode() {
            return Objects.hash(t, k);
        }
    }

    class Application<T, R> implements D<R> {
        D<T> t;
        D<Function<T, R>> f;
        Application(D<T> t, D<Function<T, R>> f) {
            this.t = t;
            this.f = f;
        }
        @Override
        public R compute() {
            return f.get().apply(t.get());
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Application)) {
                return false;
            }
            Application<?, ?> other = (Application<?, ?>)obj;
            return t.equals(other.t) && f.equals(other.f);
        }
        @Override
        public int hashCode() {
            return Objects.hash(t, f);
        }
    }

    class Binding<T, R> implements D<R> {
        D<T> t;
        Function<T, D<R>> f;
        Binding(D<T> t, Function<T, D<R>> f) {
            this.t = t;
            this.f = f;
        }
        @Override
        public R compute() {
            return f.apply(t.get()).get();
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Binding)) {
                return false;
            }
            Binding<?, ?> other = (Binding<?, ?>)obj;
            return t.equals(other.t) && f.equals(other.f);
        }
        @Override
        public int hashCode() {
            return Objects.hash(t, f);
        }
    }
}
