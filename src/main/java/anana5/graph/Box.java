package anana5.graph;

public class Box<T> implements Vertex<T> {
    private T t;

    public Box(T t) {
        this.t = t;
    }

    @Override
    public T value() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }

    public int id() {
        return this.hashCode();
    }
}
