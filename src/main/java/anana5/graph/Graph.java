package anana5.graph;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Graph<T> {
    void traverse(Consumer<Vertex<T>> consumer);
    void traverse(BiConsumer<Vertex<T>, Vertex<T>> consumer);
}
