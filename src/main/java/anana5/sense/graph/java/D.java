package anana5.sense.graph.java;

import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface D<T> {

    public T get();

    default public <S> D<S> bind(Function<T, D<S>> f) {
        return f.apply(get());
    }

    default public void bind(Consumer<T> c) {
        c.accept(get());
    }

    default public <R> D<R> map(Function<T, R> f) {
        return () -> f.apply(get());
    }
}
