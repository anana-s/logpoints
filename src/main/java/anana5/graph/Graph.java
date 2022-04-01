package anana5.graph;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface Graph<T, V extends Vertex<T>, E extends Edge<T>> {
    void traverse(Consumer<? super E> consumer);
    Collection<V> vertices();
    Collection<E> edges();
}
