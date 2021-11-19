package anana5.sense.graph;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

@FunctionalInterface
public interface Defered<T> {
    T compute();

    Map<Defered<? extends Object>, Object> cache = new WeakHashMap<>();

    default T value() {
        @SuppressWarnings("unchecked")
        T cached = (T)cache.computeIfAbsent(this, Defered::compute);
        return cached;
    }

    default <R> Defered<R> map(Function<T, R> f) {
        return () -> f.apply(value());
    }

    default <R> Defered<R> pure(R t) {
        return map(s -> t);
    }

    default <R> Defered<R> then(Defered<Function<T, R>> f) {
        return map(s -> f.value().apply(s));
    }

    default <R> Defered<R> bind(Function<T, Defered<R>> f) {
        return map(t -> f.apply(t).value());
    }

    default Defered<Void> discard() {
        return () -> null;
    }
}
