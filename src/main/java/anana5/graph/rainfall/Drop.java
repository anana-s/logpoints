package anana5.graph.rainfall;

import java.util.function.BiFunction;
import java.util.function.Function;

import anana5.graph.Vertex;

public class Drop<T, F> {

    final private T v;
    final private F f;

    protected Drop(T value, F next) {
        this.v = value;
        this.f = next;
    }

    protected Drop(Drop<T, F> droplet) {
        this.v = droplet.v;
        this.f = droplet.f;
    }

    public static <T, F> Drop<T, F> of(T value, F next) {
        return new Drop<>(value, next);
    }

    public T get() {
        return this.v;
    }

    public F next() {
        return this.f;
    }

    public <G> Drop<T, G> fmap(Function<? super F, ? extends G> func) {
        return new Drop<>(v, func.apply(f));
    }

    public <G> Drop<T, G> fmap(BiFunction<? super T, ? super F, ? extends G> func) {
        return new Drop<>(v, func.apply(v, f));
    }
}
