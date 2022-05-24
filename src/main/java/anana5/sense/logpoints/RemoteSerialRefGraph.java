package anana5.sense.logpoints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anana5.graph.Graph;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;

public class RemoteSerialRefGraph implements Graph<StmtMatcher>, AutoCloseable {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final Long2ReferenceMap<List<StmtMatcher>> sources;

    public static final Pattern pattern = Pattern.compile("(?<host>[^:]+)(?::(?<port>\\d{1,5}))?");

    private RemoteSerialRefGraph(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.sources = new Long2ReferenceOpenHashMap<>();
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

    public List<StmtMatcher> roots() {
        if (sources.containsKey(0)) {
            return sources.get(0);
        }
        try {
            List<StmtMatcher> roots = send(logpoints -> {
                final var graph = logpoints.graph();
                return graph.roots();
            });
            sources.put(0, roots);
            return roots;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StmtMatcher> from(StmtMatcher source) {
        return from(source.id());
    }

    public List<StmtMatcher> from(long id) {
        if (sources.containsKey(id)) {
            return sources.get(id);
        }
        try {
            final var targets = send(logpoints -> {
                final var graph = logpoints.graph();
                return graph.from(id);
            });
            sources.put(id, targets);
            return targets;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<StmtMatcher> to(StmtMatcher target) {
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
