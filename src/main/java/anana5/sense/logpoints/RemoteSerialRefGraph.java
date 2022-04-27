package anana5.sense.logpoints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anana5.graph.Graph;
import anana5.util.Tuple;

public class RemoteSerialRefGraph implements Graph<SerialRef>, AutoCloseable {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Map<SerialRef, Set<SerialRef>> sources;
    private Set<SerialRef> roots;

    public static final Pattern pattern = Pattern.compile("(?<host>[^:]+)(?::(?<port>\\d{1,5}))?");

    private RemoteSerialRefGraph(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.sources = new HashMap<>();
        this.roots = null;
    }

    public static RemoteSerialRefGraph connect(String address) throws IOException {
        Matcher matcher = pattern.matcher(address);
        if (!matcher.matches()) {
            throw new RuntimeException("invalid address: " + address);
        };
        matcher.group("host");
        return new RemoteSerialRefGraph(matcher.group("host"), Integer.parseInt(matcher.group("port")));
    }

    @Override
    public void close() throws IOException {
        this.out.close();
        this.in.close();
        this.socket.close();
    }

    public void traverse(SerialRef root, BiFunction<SerialRef, SerialRef, Boolean> consumer) {
        Stack<Tuple<SerialRef, SerialRef>> stack = new Stack<>();
        for (SerialRef target : from(root)) {
            stack.push(Tuple.of(root, target));
        }
        while (!stack.isEmpty()) {
            var edge = stack.pop();
            var source = edge.fst();
            var target = edge.snd();
            if (!consumer.apply(source, target)) {
                continue;
            }
            for (var next : from(target)) {
                stack.push(Tuple.of(target, next));
            }
        }
    }

    public Set<SerialRef> roots() {
        if (roots == null) {
            try {
                roots = send(logpoints -> {
                    final var graph = logpoints.graph();
                    return graph.roots();
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return roots;
    }

    @Override
    public Set<SerialRef> from(SerialRef source) {
        if (sources.containsKey(source)) {
            return sources.get(source);
        }
        try {
            var targets = send(logpoints -> {
                final var graph = logpoints.graph();
                return graph.from(source);
            });
            sources.put(source, targets);
            return targets;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<SerialRef> to(SerialRef target) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public <R extends Serializable> R send(LogPointsRequest<R> request) throws IOException {
        out.writeObject(request);
        out.flush();
        R result;
        try {
            result = (R) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
