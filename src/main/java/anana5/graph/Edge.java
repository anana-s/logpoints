package anana5.graph;

public interface Edge<T, V extends Vertex<T>> {
    V source();
    V target();
}
