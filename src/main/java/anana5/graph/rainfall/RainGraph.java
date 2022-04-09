package anana5.graph.rainfall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import anana5.graph.Edge;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.PList;
import anana5.util.Tuple;

public class RainGraph<T> implements Graph<T, RainGraph<T>.DropVertex, RainGraph<T>.DropEdge> {
    private final Rain<T> rain;
    private Iterator<DropEdge> it;
    private Map<Drop<T, Rain<T>>, DropVertex> vertices = new LinkedHashMap<>();
    private Set<DropEdge> edges = new LinkedHashSet<>();
    private Map<DropVertex, Collection<DropEdge>> srcMemo = new HashMap<>();
    private Map<DropVertex, Collection<DropEdge>> tgtMemo = new HashMap<>();

    private RainGraph(Rain<T> rain) {
        this.rain = rain;
        this.it = collect(null, rain, new HashSet<>()).iterator();
    }

    public static <T> RainGraph<T> of(Rain<T> rain) {
        return new RainGraph<>(rain);
    }

    public Rain<T> rain() {
        return rain;
    }

    @Override
    public Collection<DropVertex> vertices() {
        return vertices.values();
    }

    @Override
    public Collection<DropEdge> edges() {
        return Collections.unmodifiableCollection(edges);
    }

    @Override
    public Collection<DropEdge> from(DropVertex source) {
        return Collections.unmodifiableCollection(srcMemo.get(source));
    }

    @Override
    public Collection<DropEdge> to(DropVertex target) {
        it.forEachRemaining((edge) -> {});
        return Collections.unmodifiableCollection(tgtMemo.get(target));
    }

    private PList<DropEdge> collect(DropVertex prev, Rain<T> rain, Set<T> visited) {
        return rain.unfix().flatmap(drop -> {
            if (visited.contains(drop.get())) {
                return PList.of(edge(prev, vertex(drop)));
            }
            visited.add(drop.get());
            DropVertex vertex = vertex(drop);
            return PList.cons(edge(prev, vertex), collect(vertex, drop.next(), visited));
        });
    }

    @Override
    public Collection<DropVertex> roots() {
        return rain.unfix().map(this::vertex).collect().join();
    }

    private synchronized DropVertex vertex(Drop<T, Rain<T>> drop) {
        return vertices.computeIfAbsent(drop, d -> new DropVertex(d));
    }

    private synchronized DropEdge edge(DropVertex source, DropVertex target) {
        var edge = new DropEdge(source, target);
        edges.add(edge);
        return edge;
    }

    public class DropVertex implements Vertex<T, DropVertex> {
        private Drop<T, Rain<T>> drop;

        private DropVertex(Drop<T, Rain<T>> drop) {
            this.drop = drop;
        }

        @Override
        public T value() {
            return drop.get();
        }

        @Override
        public List<DropVertex> next() {
            return drop.next().unfix().map(RainGraph.this::vertex).collect().join();
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == DropVertex.class && ((RainGraph<?>.DropVertex) obj).drop.equals(drop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(drop);
        }

    }

    public class DropEdge extends Tuple<DropVertex, DropVertex> implements Edge<T, DropVertex> {
        private DropEdge(DropVertex source, DropVertex target) {
            super(source, target);
        }

        @Override
        public RainGraph<T>.DropVertex source() {
            return this.fst();
        }

        @Override
        public RainGraph<T>.DropVertex target() {
            return this.snd();
        }
    }
}
