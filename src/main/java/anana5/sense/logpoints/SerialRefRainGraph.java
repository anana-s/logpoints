package anana5.sense.logpoints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
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

    public void traverse(Set<SerialRef> seen, SerialRef root, BiConsumer<SerialRef, SerialRef> consumer) {
        Stack<Tuple<SerialRef, SerialRef>> stack = new Stack<>();
        seen.add(root);
        for (SerialRef target : from(root)) {
            stack.push(Tuple.of(root, target));
        }
        while (!stack.isEmpty()) {
            var edge = stack.pop();
            var source = edge.fst();
            var target = edge.snd();
            if (target.sentinel()) {
                continue;
            }
            consumer.accept(source, target);
            if (seen.contains(target) || target.sentinel()) {
                continue;
            }
            seen.add(source);
            for (var next : from(source)) {
                stack.push(Tuple.of(source, next));
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
            roots = rain.unfix().map(this::visit).collect(Collectors.toCollection(HashSet::new));
        }
        return roots;
    }

    @Override
    public HashSet<SerialRef> from(SerialRef source) {
        return memo.get(source).unfix().map(this::visit).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public HashSet<SerialRef> to(SerialRef target) {
        throw new UnsupportedOperationException();
    }
}
