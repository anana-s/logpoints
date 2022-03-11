package anana5.graph.rainfall;

import java.util.function.BiFunction;
import java.util.function.Function;

import anana5.graph.Vertex;

public class Drop<T, F> {

    final private Vertex<T> box;
    final private F f;

    protected Drop(Vertex<T> box, F next) {
        this.box = box;
        this.f = next;
    }

    protected Drop(Drop<T, F> droplet) {
        this.box = droplet.box;
        this.f = droplet.f;
    }

    public static <T, F> Drop<T, F> of(Vertex<T> box, F next) {
        return new Drop<>(box, next);
    }

    @Deprecated
    public Vertex<T> get() {
        return this.vertex();
    }

    public Vertex<T> vertex() {
        return this.box;
    }

    public T value() {
        return this.vertex().value();
    }

    public F next() {
        return this.f;
    }

    public <G> Drop<T, G> fmap(Function<? super F, ? extends G> func) {
        return new Drop<>(box, func.apply(f));
    }

    public <G> Drop<T, G> fmap(BiFunction<? super Vertex<T>, ? super F, ? extends G> func) {
        return new Drop<>(box, func.apply(box, f));
    }
}
