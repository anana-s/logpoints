package anana5.graph.rainfall;

import java.util.function.BiFunction;
import java.util.function.Function;

import anana5.graph.Box;

public class Droplet<T, F> {

    final private Box<T> box;
    final private F f;

    public Droplet(T value, F next) {
        this(new Box<>(value), next);
    }

    private Droplet(Box<T> box, F next) {
        this.box = box;
        this.f = next;
    }

    public Droplet(Droplet<T, F> droplet) {
        this.box = droplet.box;
        this.f = droplet.f;
    }

    public Box<T> get() {
        return this.box;
    }

    public F next() {
        return this.f;
    }

    public <S> Droplet<S, F> map(Function<T, S> func) {
        return new Droplet<>(new Box<>(func.apply(box.value())), this.f);
    }

    public <G> Droplet<T, G> fmap(Function<F, G> func) {
        return new Droplet<>(box, func.apply(f));
    }

    public <G> Droplet<T, G> fmap(BiFunction<Box<T>, F, G> func) {
        return new Droplet<>(box, func.apply(box, f));
    }
}
