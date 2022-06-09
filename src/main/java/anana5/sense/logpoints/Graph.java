package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.util.Tuple;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;

public class Graph implements anana5.graph.Graph<SerializedVertex> {
    private final Rain<Vertex> rain;
    private final Long2ReferenceMap<Rain<Vertex>> next;

    private static long count = 1;
    private static long probe() {
        return count++;
    }

    public Graph(Rain<Vertex> rain) {
        this.rain = rain;
        this.next = new Long2ReferenceOpenHashMap<>();
    }

    public void traverse(SerializedVertex root, BiFunction<SerializedVertex, SerializedVertex, Boolean> consumer) {
        Stack<Tuple<SerializedVertex, SerializedVertex>> stack = new Stack<>();
        for (SerializedVertex target : from(root)) {
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

    private SerializedVertex visit(Drop<Vertex, Rain<Vertex>> drop) {
        var id = probe();
        next.put(id, drop.next());
        var matcher = new SerializedVertex(id, drop.get());
        return matcher;
    }

    public ArrayList<SerializedVertex> roots() {
        return rain.unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();

    }

    @Override
    public ArrayList<SerializedVertex> from(SerializedVertex source) {
        return from(source.id());
    }

    public ArrayList<SerializedVertex> from(long source) {
        if (!next.containsKey(source)) {
            throw new UnsupportedOperationException("value not yet reached");
        }
        return next.get(source).unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();
    }

    @Override
    public ArrayList<SerializedVertex> to(SerializedVertex target) {
        throw new UnsupportedOperationException();
    }
}
