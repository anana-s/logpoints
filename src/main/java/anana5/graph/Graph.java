package anana5.graph;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface Graph<T, V extends Vertex<T>, E extends Edge<T, V>> {
    void traverse(Consumer<? super V> consumer);
    void traverse(BiConsumer<? super V, ? super V> consumer);
    <R> R fold(R initial, BiFunction<Collection<? super R>, ? super V, ? super V> consumer);
    Collection<V> vertices();
    Collection<E> edges();
}
