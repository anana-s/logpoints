package anana5.sense.logpoints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.sense.logpoints.Box.Ref;
import anana5.util.Tuple;

public class SerialRefRainGraph implements Graph<SerialRef> {
    private final Rain<Ref> rain;
    private final Map<SerialRef, Rain<Ref>> memo;
    private HashSet<SerialRef> roots;

    public SerialRefRainGraph(Rain<Ref> rain) {
        this.rain = rain;
        this.memo = new HashMap<>();
        this.roots = null;
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

    private SerialRef visit(Drop<Ref, Rain<Ref>> drop) {
        SerialRef serial = new SerialRef(drop.get());
        memo.put(serial, drop.next());
        return serial;
    }

    public HashSet<SerialRef> roots() {
        if (roots == null) {
            roots = rain.unfix().map(this::visit).collect(Collectors.toCollection(HashSet::new)).join();
        }
        return roots;
    }

    @Override
    public HashSet<SerialRef> from(SerialRef source) {
        if (!memo.containsKey(source)) {
            throw new UnsupportedOperationException("value not yet reached");
        }
        return memo.get(source).unfix().map(this::visit).collect(Collectors.toCollection(HashSet::new)).join();

    }

    @Override
    public HashSet<SerialRef> to(SerialRef target) {
        throw new UnsupportedOperationException();
    }
}
