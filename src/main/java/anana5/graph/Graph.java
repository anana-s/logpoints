package anana5.graph;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface Graph<T, V extends Vertex<T>, E extends Edge<T, V>> {
    void traverse(BiConsumer<? super V, ? super V> consumer);
    Collection<V> vertices();
    Collection<E> edges();
}
