package anana5.sense.logpoints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anana5.graph.Graph;
import anana5.util.Tuple;

public class Client implements Graph<SerialRef>, AutoCloseable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Set<SerialRef> roots;
    private Map<SerialRef, Set<SerialRef>> sources;
    private Map<SerialRef, Set<SerialRef>> targets;

    public static final Pattern pattern = Pattern.compile("(?<host>[^:]+)(?::(?<port>\\d{1,5}))?");

    private Client(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.roots = null;
        this.sources = new HashMap<>();
        this.targets = new HashMap<>();
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

    public void traverse(SerialRef root, BiConsumer<SerialRef, SerialRef> consumer) {
        traverse(root, consumer, new HashSet<>());
    }

    private void traverse(SerialRef root, BiConsumer<SerialRef, SerialRef> consumer, Set<SerialRef> seen) {
        Stack<Tuple<SerialRef, SerialRef>> stack = new Stack<>();
        seen.add(root);
        for (SerialRef target : from(root)) {
            stack.push(Tuple.of(root, target));
        }
        while (!stack.isEmpty()) {
            var edge = stack.pop();
            var target = edge.fst();
            var source = edge.snd();
            consumer.accept(target, source);
            if (seen.contains(target) || target.recursive()) {
                continue;
            }
            seen.add(target);
            for (var next : from(target)) {
                stack.push(Tuple.of(target, next));
            }
        }
    }

    @Override
    public Set<SerialRef> all() {
        return send(graph -> {
            return new HashSet<>(graph.all());
        });
    }

    public Set<SerialRef> roots() {
        if (roots == null) {
            roots = send(graph -> {
                return new HashSet<>(graph.roots());
            });
        }
        return roots;
    }

    @Override
    public Set<SerialRef> from(SerialRef source) {
        if (sources.containsKey(source)) {
            return sources.get(source);
        }
        var targets = send(graph -> {
            return new HashSet<>(graph.from(source));
        });
        sources.put(source, targets);
        return targets;
    }

    @Override
    public Set<SerialRef> to(SerialRef target) {
        if (targets.containsKey(target)) {
            return targets.get(target);
        }
        var sources = send(graph -> {
            return new HashSet<>(graph.from(target));
        });
        targets.put(target, sources);
        return sources;
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
