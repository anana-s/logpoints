package anana5.graph.rainfall;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Droplet<A, F> {

    final public A drop;
    final public F let;

    public Droplet(A ref, F next) {
        this.drop = ref;
        this.let = next;
    }

    public Droplet(Droplet<A, F> droplet) {
        this.drop = droplet.drop;
        this.let = droplet.let;
    }

    public <B> Droplet<B, F> map(Function<A, B> f) {
        return new Droplet<>(f.apply(drop), let);
    }

    public <B> Droplet<B, F> map(BiFunction<A, F, B> f) {
        return new Droplet<>(f.apply(drop, let), let);
    }

    public <G> Droplet<A, G> fmap(Function<F, G> f) {
        return new Droplet<>(drop, f.apply(let));
    }

    public <G> Droplet<A, G> fmap(BiFunction<A, F, G> f) {
        return new Droplet<>(drop, f.apply(drop, let));
    }

    public void accept(Consumer<A> consumer) {
        consumer.accept(drop);
    }

    public void accept(BiConsumer<A, F> consumer) {
        consumer.accept(drop, let);
    }

    public Drop<A> freeze(Drop<A> parent) {
        return new Drop<>(parent, this);
    }
}
