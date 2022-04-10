package anana5.graph.rainfall;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.util.PList;
import anana5.util.Tuple;

public class RainGraph<T> implements Graph<T> {
    private final PList<T> roots;
    private Iterator<Tuple<T, PList<T>>> it;
    private Map<T, PList<T>> sources = new HashMap<>();
    private Map<T, PList<T>> targets = new HashMap<>();

    private RainGraph(Rain<T> rain) {
        this.roots = rain.unfix().map(Drop::get);
        this.it = collect(null, rain, new HashMap<>()).iterator();
    }

    public static <T> RainGraph<T> of(Rain<T> rain) {
        return new RainGraph<>(rain);
    }

    public Set<T> roots() {
        return roots.collect(Collectors.toSet());
    }

    @Override
    public Set<T> all() {
        resolve();
        return sources.keySet();
    }

    @Override
    public Set<T> from(T source) {
        if (sources.containsKey(source)) {
            return sources.get(source).collect(Collectors.toSet());
        }
        while (it.hasNext()) {
            Tuple<T, PList<T>> tuple = it.next();
            if (source.equals(tuple.fst())) {
                return tuple.snd().collect(Collectors.toSet());
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public Set<T> to(T target) {
        resolve();
        return targets.get(target).collect(Collectors.toSet());
    }

    public synchronized RainGraph<T> resolve() {
        while (it.hasNext()) {
            it.next();
        }
        return this;
    }

    private void visit(T prev, T t, PList<T> next) {
        sources.computeIfAbsent(t, k -> next);
        targets.compute(t, (k, list) -> list == null ? PList.of(prev) : list.push(prev));
    }

    private PList<Tuple<T, PList<T>>> collect(T prev, Rain<T> rain, Map<T, PList<T>> visited) {
        return rain.unfix().flatmap(drop -> {
            T t = drop.get();
            if (visited.containsKey(t)) {
                visit(prev, t, PList.of());
                return PList.of();
            }
            PList<T> next = drop.next().unfix().map(Drop::get);
            visit(prev, t, next);
            visited.put(t, next);
            return PList.cons(Tuple.of(t, next), collect(prev, drop.next(), visited));
        });
    }
}
