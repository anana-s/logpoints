package anana5.sense.graph.java;

public class Delayed<T> implements D<T> {
    T ref = null;
    private D<T> d;
    public Delayed() {
        set(() -> ref);
    }
    public Delayed(D<T> d) {
        if (d instanceof Delayed<?>) {
            set(((Delayed<T>)d).d);
        } else {
            set(d);
        }
    }
    public Delayed<T> set(D<T> d) {
        this.d = d;
        return this;
    }
    public T get() {
        if (ref == null) {
            ref = d.get();
        }
        return ref;
    }
}
