package anana5.graph;

import java.util.Collection;

import anana5.util.Value;

public interface Vertex<T> {
    T value();
    Collection<? extends Vertex<T>> next();
}
