package anana5.graph.rainfall;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import anana5.graph.Edge;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.PList;
import anana5.util.Promise;

public class RainGraph<T> implements Graph<T, Vertex<T>, Edge<T, Vertex<T>>> {

    public class DropVertex implements Vertex<T> {
        private Drop<T, Rain<T>> drop;

        private DropVertex(Drop<T, Rain<T>> drop) {
            this.drop = drop;
        }

        @Override
        public T value() {
            return drop.get();
        }

        @Override
        public Collection<DropVertex> next() {
            return drop.next().unfix().map(DropVertex::new).collect().join();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RainGraph<?>.DropVertex && ((RainGraph<?>.DropVertex) obj).drop.equals(drop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(drop);
        }

    }

    public class DropEdge implements Edge<T, Vertex<T>> {
        private DropVertex source, target;

        private DropEdge(DropVertex source, DropVertex target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public RainGraph<T>.DropVertex source() {
            return this.source;
        }

        @Override
        public RainGraph<T>.DropVertex target() {
            return this.target;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RainGraph<?>.DropEdge && ((RainGraph<?>.DropEdge) obj).source.equals(source) && ((RainGraph<?>.DropEdge) obj).target.equals(target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }

    }


    private final Rain<T> rain;
    private final PList<Edge<T, Vertex<T>>> edges;

    private RainGraph(Rain<T> rain) {
        this.rain = rain;
        this.edges = collect(null, rain, new HashSet<>());
    }

    public static <T> RainGraph<T> of(Rain<T> rain) {
        return new RainGraph<>(rain);
    }

    public Rain<T> rain() {
        return rain;
    }

    @Override
    public void traverse(BiConsumer<? super Vertex<T>, ? super Vertex<T>> consumer) {
        edges.traverse(edge -> {
            consumer.accept(edge.source(), edge.target());
            return Promise.nil();
        }).join();
    }

    private Collection<Vertex<T>> verticesMemo = null;
    @Override
    public Collection<Vertex<T>> vertices() {
        if (verticesMemo == null) {
            verticesMemo = new HashSet<>();
            this.edges.foldr(null, (edge, acc) -> {
                verticesMemo.add(edge.target());
                return Promise.nil();
            }).join();
        }
        return verticesMemo;
    }

    private Collection<Edge<T, Vertex<T>>> edgesMemo = null;
    @Override
    public Collection<Edge<T, Vertex<T>>> edges() {
        if (edgesMemo == null) {
            edgesMemo = this.edges.collect().join();
        }
        return edgesMemo;
    }

    private PList<Edge<T, Vertex<T>>> collect(DropVertex prev, Rain<T> rain, Set<T> visited) {
        return rain.unfix().flatmap(drop -> {
            if (visited.contains(drop.get())) {
                return PList.of(new DropEdge(prev, new DropVertex(drop)));
            }
            visited.add(drop.get());
            DropVertex vertex = new DropVertex(drop);
            return PList.cons(new DropEdge(prev, vertex), collect(vertex, drop.next(), visited));
        });
    }
}
