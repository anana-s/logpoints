package anana5.graph;

import java.util.Collection;

public interface Vertex<T, V extends Vertex<T, V>> {
    T value();
    Collection<V> next();
}
