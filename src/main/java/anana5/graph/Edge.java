package anana5.graph;

public interface Edge<T> {
    Vertex<T> source();
    Vertex<T> target();
}
