package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.graph.rainfall.Drop;
import anana5.graph.rainfall.Rain;
import anana5.util.Tuple;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class SerialRefRainGraph implements Graph<StmtMatcher> {
    private final Rain<Box> rain;
    private final Long2ReferenceMap<Rain<Box>> next;

    private static long count = 1;
    private static long probe() {
        return count++;
    }

    public SerialRefRainGraph(Rain<Box> rain) {
        this.rain = rain;
        this.next = new Long2ReferenceOpenHashMap<>();
    }

    public void traverse(StmtMatcher root, BiFunction<StmtMatcher, StmtMatcher, Boolean> consumer) {
        Stack<Tuple<StmtMatcher, StmtMatcher>> stack = new Stack<>();
        for (StmtMatcher target : from(root)) {
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

    private StmtMatcher visit(Drop<Box, Rain<Box>> drop) {
        var id = probe();
        next.put(id, drop.next());
        var matcher = new StmtMatcher(id, drop.get());
        return matcher;
    }

    public ArrayList<StmtMatcher> roots() {
        return rain.unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();

    }

    @Override
    public ArrayList<StmtMatcher> from(StmtMatcher source) {
        return from(source.id());
    }

    public ArrayList<StmtMatcher> from(long source) {
        if (!next.containsKey(source)) {
            throw new UnsupportedOperationException("value not yet reached");
        }
        return next.get(source).unfix().map(this::visit).collect(Collectors.toCollection(ArrayList::new)).join();
    }

    @Override
    public ArrayList<StmtMatcher> to(StmtMatcher target) {
        throw new UnsupportedOperationException();
    }
}
