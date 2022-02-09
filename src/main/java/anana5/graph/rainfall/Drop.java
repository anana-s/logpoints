package anana5.graph.rainfall;

import anana5.graph.Vertex;

public class Drop<T> implements Vertex<T> {

    private final Drop<T> parent;
    private final Droplet<T, ?> droplet;

    public Drop(Drop<T> parent, Droplet<T, ?> droplet) {
        this.parent = parent;
        this.droplet = droplet;
    }

    @Override
    public int id() {
        return droplet.hashCode();
    }

    @Override
    public Drop<T> parent() {
        return parent;
    }

    @Override
    public T get() {
        return droplet.drop;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Drop<?>)) {
            return false;
        }

        Drop<?> other = (Drop<?>)obj; 

        return droplet.equals(other.droplet);
    }
}
