package anana5.graph.rainfall;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.util.PList;

public class RainGraph<T> implements Graph<T> {
    private final PList<Drop<T, Rain<T>>> roots;
    private Map<T, PList<Drop<T, Rain<T>>>> sources = new HashMap<>();

    private RainGraph(Rain<T> rain) {
        this.roots = rain.unfix();
    }

    public static <T> RainGraph<T> of(Rain<T> rain) {
        return new RainGraph<>(rain);
    }

    public Set<T> roots() {
        return roots.map(this::visit).collect(Collectors.toSet()).join();
    }

    @Override
    public Set<T> from(T source) {
        return sources.get(source).map(this::visit).collect(Collectors.toSet()).join();
    }

    @Override
    public Set<T> to(T target) {
        throw new UnsupportedOperationException();
    }

    private T visit(Drop<T, Rain<T>> drop) {
        T t = drop.get();
        sources.computeIfAbsent(t, k -> drop.next().unfix());
        return t;
    }
}
