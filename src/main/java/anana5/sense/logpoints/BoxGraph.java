package anana5.sense.logpoints;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


import anana5.graph.Edge;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.graph.rainfall.RainGraph;
import anana5.util.Tuple;

public class BoxGraph implements Graph<SerialRef, BoxGraph.V, BoxGraph.E> {
    private final RainGraph<Box.Ref> graph;
    private final Map<SerialRef, RainGraph<Box.Ref>.DropVertex> memo;

    public BoxGraph(RainGraph<Box.Ref> graph) {
        this.graph = graph;
        this.memo = new HashMap<>();
    }

    public Collection<V> roots() {
        return graph.roots().stream().map(this::vertex).collect(Collectors.toList());
    }

    @Override
    public Collection<V> vertices() {
        return graph.vertices().stream().map(this::vertex).collect(Collectors.toList());
    }

    @Override
    public Collection<E> edges() {
        return graph.edges().stream().map(this::edge).collect(Collectors.toList());
    }

    public static String encode(byte[] hash) {
        return Base64.getEncoder().encodeToString(hash);
    }

    public static byte[] decode(String hash) {
        return Base64.getDecoder().decode(hash);
    }

    public V vertex(SerialRef value) {
        return new V(value);
    }

    private V vertex(RainGraph<Box.Ref>.DropVertex dv) {
        var serial = new SerialRef(dv.value());
        memo.put(serial, dv);
        return vertex(serial);
    }

    private E edge(RainGraph<Box.Ref>.DropEdge edge) {
        return edge(edge.source(), edge.target());
    }

    private E edge(RainGraph<Box.Ref>.DropVertex source, RainGraph<Box.Ref>.DropVertex target) {
        return edge(vertex(source), vertex(target));
    }

    private E edge(V source, V target) {
        return new E(source, target);
    }
    
    public class V implements Vertex<SerialRef, V> {
        private final SerialRef serial;

        public V(SerialRef serial) {
            this.serial = serial;
        }

        @Override
        public SerialRef value() {
            return serial;
        }

        @Override
        public Collection<V> next() {
            return memo.get(serial).next().stream().map(BoxGraph.this::vertex).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == V.class && ((V) obj).serial.equals(serial);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serial.hashCode());
        }
    }

    public class E extends Tuple<V, V> implements Edge<SerialRef, V> {
        private E(V source, V target) {
            super(source, target);
        }

        @Override
        public V source() {
            return fst();
        }

        @Override
        public V target() {
            return snd();
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == E.class && ((E) obj).fst().equals(fst()) && ((E) obj).snd().equals(snd());
        }

        @Override
        public int hashCode() {
            return Objects.hash(fst(), snd());
        }
    }

    @Override
    public Collection<? extends E> from(V source) {
        return source.next().stream().map(target -> edge(source, target)).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends E> to(V target) {
        RainGraph<Box.Ref>.DropVertex dv = memo.get(target.serial);
        return graph.to(dv).stream().map(this::edge).collect(Collectors.toList());
    }
}
