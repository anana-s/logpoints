package anana5.graph;

import java.util.function.Consumer;

public interface Graph<T> {
    void traverse(Consumer<Vertex<T>> consumer);
}
