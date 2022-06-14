package anana5.graph.rainfall;

public class Drop<T, A> {

    final private T v;
    final private A f;

    protected Drop(T value, A next) {
        this.v = value;
        this.f = next;
    }

    protected Drop(Drop<T, A> droplet) {
        this.v = droplet.v;
        this.f = droplet.f;
    }

    public static <T, F> Drop<T, F> of(T value, F next) {
        return new Drop<>(value, next);
    }

    public T get() {
        return this.v;
    }

    public A next() {
        return this.f;
    }
}
