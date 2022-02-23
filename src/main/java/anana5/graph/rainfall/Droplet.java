package anana5.graph.rainfall;

import java.util.function.BiFunction;
import java.util.function.Function;

import anana5.graph.Box;

public class Droplet<A, F> {

    final private Box<A> box;
    final private F f;

    public Droplet(Box<A> box, F next) {
        this.box = box;
        this.f = next;
    }

    public Droplet(Droplet<A, F> droplet) {
        this.box = droplet.box;
        this.f = droplet.f;
    }

    public Box<A> get() {
        return this.box;
    }

    public F next() {
        return this.f;
    }

    public <G> Droplet<A, G> fmap(Function<F, G> func) {
        return new Droplet<>(box, func.apply(f));
    }

    public <G> Droplet<A, G> fmap(BiFunction<Box<A>, F, G> func) {
        return new Droplet<>(box, func.apply(box, f));
    }
}
