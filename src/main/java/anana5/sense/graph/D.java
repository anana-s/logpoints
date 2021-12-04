package anana5.sense.graph;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
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

    default <R> D<R> map(Function<T, R> f) {
        return new Mapping<>(this, f);
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
