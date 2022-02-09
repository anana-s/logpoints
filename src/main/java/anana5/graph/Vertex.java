package anana5.graph;

public interface Vertex<T> {
    int id();
    Vertex<T> parent();
    T get();
}
