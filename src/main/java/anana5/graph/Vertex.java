package anana5.graph;

public interface Vertex<T> {
    T value();
    byte[] hash();
}
