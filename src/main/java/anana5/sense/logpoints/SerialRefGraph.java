package anana5.sense.logpoints;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.graph.rainfall.RainGraph;
import anana5.sense.logpoints.Box.Ref;

public class SerialRefGraph implements Graph<SerialRef> {
    private final RainGraph<Box.Ref> graph;
    private final Map<SerialRef, Box.Ref> memo;

    public SerialRefGraph(RainGraph<Box.Ref> graph) {
        this.graph = graph;
        this.memo = new HashMap<>();
    }

    public void prepare() {
        for (Ref ref : this.graph.all()) {
            visit(ref);
        };
    }

    private SerialRef visit(Ref ref) {
        SerialRef serial = new SerialRef(ref);
        memo.put(serial, ref);
        return serial;
    }

    public Set<SerialRef> roots() {
        return graph.roots().stream().map(this::visit).collect(Collectors.toSet());
    }

    @Override
    public Set<SerialRef> all() {
        return graph.all().stream().map(this::visit).collect(Collectors.toSet());
    }

    @Override
    public Set<SerialRef> from(SerialRef source) {
        Ref ref = memo.get(source);
        return graph.from(ref).stream().map(this::visit).collect(Collectors.toSet());
    }

    @Override
    public Set<SerialRef> to(SerialRef target) {
        Ref ref = memo.get(target);
        return graph.to(ref).stream().map(this::visit).collect(Collectors.toSet());
    }
}
