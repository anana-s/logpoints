package anana5.sense.logpoints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import anana5.graph.Edge;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.sense.logpoints.Box.SerialRef;
import anana5.util.Tuple;

public class Client implements Graph<SerialRef, Client.V, Client.E>, AutoCloseable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Map<SerialRef, V> memo;
    private Collection<V> roots;


    public static final Pattern pattern = Pattern.compile("(?<host>[^:]+)(?::(?<port>\\d{1,5}))?");

    private Client(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.memo = new HashMap<>();
        this.roots = null;
    }

    public static Client connect(String address) throws IOException {
        Matcher matcher = pattern.matcher(address);
        if (!matcher.matches()) {
            throw new RuntimeException("invalid address: " + address);
        };
        matcher.group("host");
        return new Client(matcher.group("host"), Integer.parseInt(matcher.group("port")));
    }

    @Override
    public void close() throws Exception {
        this.out.close();
        this.in.close();
        this.socket.close();

    }

    @Override
    public Collection<V> vertices() {
        var vertices = send(graph -> {
            return new ArrayList<>(graph.join().vertices().stream().map(v -> v.value()).collect(Collectors.toList()));
        });
        return vertices.stream().map(this::vertex).collect(Collectors.toList());
    }

    @Override
    public Collection<E> edges() {
        var edges = send(graph -> {
            return new ArrayList<>(graph.join().edges().stream().map(e -> Tuple.of(e.source().value(), e.target().value())).collect(Collectors.toList()));
        });
        return edges.stream().map(this::edge).collect(Collectors.toList());
    }

    public V vertex(SerialRef value) {
        return memo.computeIfAbsent(value, v -> new V(v));
    }

    public E edge(Tuple<SerialRef, SerialRef> edge) {
        return edge(edge.fst(), edge.snd());
    }

    public E edge(SerialRef source, SerialRef target) {
        return new E(source, target);
    }

    public Collection<V> roots() {
        if (roots != null) {
            return roots;
        }
        var vertices = send(graph -> {
            return new ArrayList<>(graph.join().roots().stream().map(v -> v.value()).collect(Collectors.toList()));
        });
        return vertices.stream().map(v -> vertex(v)).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends E> from(V source) {
        SerialRef serial = source.value();
        var edges = send(graph -> {
            return new ArrayList<>(graph.join().from(graph.join().vertex(serial)).stream().map(e -> Tuple.of(e.source().value(), e.target().value())).collect(Collectors.toList()));
        });
        return edges.stream().map(e -> edge(e)).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends E> to(V target) {
        SerialRef serial = target.value();
        var edges = send(graph -> {
            return new ArrayList<>(graph.join().to(graph.join().vertex(serial)).stream().map(e -> Tuple.of(e.source().value(), e.target().value())).collect(Collectors.toList()));
        });
        return edges.stream().map(e -> edge(e)).collect(Collectors.toList());
    }

    public class V implements Vertex<SerialRef, V>, Serializable {
        private final SerialRef ref;
        private ArrayList<V> next;

        private V(SerialRef ref) {
            this.ref = ref;
            this.next = null;
        }

        @Override
        public SerialRef value() {
            return ref;
        }

        @Override
        public Collection<V> next() {
            if (next != null) {
                return next;
            }
            SerialRef ref = this.ref;
            var refs = Client.this.send(graph -> {
                return new ArrayList<>(graph.join().vertex(ref).next().stream().map(v -> v.value()).collect(Collectors.toList()));
            });
            return refs.stream().map(r -> vertex(r)).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == V.class && ((V) obj).ref.equals(ref);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ref.hashCode());
        }
    }

    public class E extends Tuple<Box.SerialRef, Box.SerialRef> implements Edge<Box.SerialRef, V> {
        private E(Box.SerialRef source, Box.SerialRef target) {
            super(source, target);
        }

        @Override
        public V source() {
            return vertex(fst());
        }

        @Override
        public V target() {
            return vertex(snd());
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

    public <R extends Serializable> R send(GraphRequest<R> request) {
        try {
            out.writeObject(request);
            out.flush();
            @SuppressWarnings("unchecked")
            R result = (R) in.readObject();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
