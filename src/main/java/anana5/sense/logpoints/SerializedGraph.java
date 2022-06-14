package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.stream.Collectors;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;

public class SerializedGraph implements anana5.graph.Graph<GrapherVertex> {
    private final Rain<GrapherVertex> rain;
    private final Long2ReferenceMap<Rain<GrapherVertex>> next;

    public SerializedGraph(Rain<GrapherVertex> rain) {
        this.rain = rain;
        this.next = new Long2ReferenceOpenHashMap<>();
    }

    // public void traverse(GrapherVertex root, BiFunction<GrapherVertex, GrapherVertex, Boolean> consumer) {
    //     Stack<Tuple<GrapherVertex, GrapherVertex>> stack = new Stack<>();
    //     for (GrapherVertex target : from(root)) {
    //         stack.push(Tuple.of(root, target));
    //     }
    //     while (!stack.isEmpty()) {
    //         var edge = stack.pop();
    //         var source = edge.fst();
    //         var target = edge.snd();
    //         if (!consumer.apply(source, target)) {
    //             continue;
    //         }
    //         for (var next : from(target)) {
    //             stack.push(Tuple.of(target, next));
    //         }
    //     }
    // }

    private SerializedVertex visit(Drop<GrapherVertex, Rain<GrapherVertex>> drop) {
        var vertex = drop.get();
        next.put(vertex.id(), drop.next());
        var matcher = SerializedVertex.serialize(vertex);
        return matcher;
    }

    public ArrayList<SerializedVertex> roots() {
        return rain.unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();

    }

    @Override
    public ArrayList<SerializedVertex> from(GrapherVertex source) {
        return from(source.id());
    }

    public ArrayList<SerializedVertex> from(long source) {
        if (!next.containsKey(source)) {
            throw new UnsupportedOperationException("value not yet reached");
        }
        return next.get(source).unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();
    }

    @Override
    public ArrayList<GrapherVertex> to(GrapherVertex target) {
        throw new UnsupportedOperationException();
    }
}
