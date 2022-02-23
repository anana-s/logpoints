package anana5.graph;

import anana5.graph.rainfall.Droplet;

public class DirectedEdge<T> implements Edge<T> {

    private final Box<T> source;
    private final Box<T> target;

    public DirectedEdge(Droplet<T, ?> source, Droplet<T, ?> target) {
        this(source.get(), target.get());
    }

    public DirectedEdge(Box<T> source, Box<T> target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public Box<T> source() {
        return this.source;
    }

    @Override
    public Box<T> target() {
        return this.target;
    }
}
