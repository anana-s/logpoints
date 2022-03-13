package anana5.graph.rainfall;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;

/**
 * A lazy graph implementation using promises.
 */
public class Rain<T> {    
    
    final private LList<Drop<T, Rain<T>>> unfix;

    public LList<Drop<T, Rain<T>>> unfix() {
        return this.unfix;
    }

    private Rain(LList<Drop<T, Rain<T>>> drops) {
        this.unfix = drops;
    }

    @SafeVarargs
    public static <T> Rain<T> of(Drop<T, Rain<T>>... drops) {
        return new Rain<>(LList.of(drops));
    }

    public static <T> Rain<T> from(Iterable<Drop<T, Rain<T>>> drops) {
        return new Rain<>(LList.from(drops));
    }

    public static <T> Rain<T> fix(LList<Drop<T, Rain<T>>> drops) {
        return new Rain<>(drops);
    }
    
    /** 
     * @return Promise<Collection<Droplet<T, Rain<T>>>>
     */
    public Promise<Collection<Drop<T, Rain<T>>>> collect() {
        return unfix.collect();
    }

    /** 
     * @param func
     * @return R
     */
    public <R> R fold(Function<LList<Drop<T, R>>, R> func) {
        return func.apply(unfix.map(drop -> drop.fmap(let -> let.fold(func))));
    }

    
    /** 
     * @param func
     * @return Rain<T>
     */
    public static <S, T> Rain<T> unfold(S s, Function<S, LList<Drop<T, S>>> func) {
        return Rain.fix(func.apply(s).map(droplet -> droplet.fmap(let -> Rain.unfold(let, func))));
    }

    /** 
     * @param func
     * @return Rain<R>
     */
    public <R> Rain<R> map(Function<Vertex<T>, Vertex<R>> func) {
        Map<Vertex<T>, Drop<R, Rain<R>>> cache = new HashMap<>();
        return this.fold(droplets -> new Rain<R>(droplets.map(drop -> cache.computeIfAbsent(drop.vertex(), box -> new Drop<>(func.apply(box), drop.next())))));
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traverse(BiFunction<Vertex<T>, Vertex<T>, Promise<Void>> visitor) {
        return traverse(null, visitor, new HashSet<>());
    }

    private Promise<Void> traverse(Vertex<T> source, BiFunction<Vertex<T>, Vertex<T>, Promise<Void>> visitor, Set<Vertex<T>> visited) {
        return unfix.traverse(droplet -> {
            var target = droplet.vertex();
            if (visited.contains(target)) {
                return visitor.apply(source, target);
            }
            visited.add(target);
            return visitor.apply(source, target).then($ -> droplet.next().traverse(target, visitor, visited));
        });
    }

    /** 
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traverse(Function<Vertex<T>, Promise<Void>> visitor) {
        return traverse(visitor, new HashSet<>());
    }

    private Promise<Void> traverse(Function<Vertex<T>, Promise<Void>> visitor, Set<Vertex<T>> visited) {
        return unfix.traverse(droplet -> {
            var target = droplet.vertex();
            if (visited.contains(target)) {
                return Promise.nil();
            }
            visited.add(target);
            return visitor.apply(target).then($ -> droplet.next().traverse(visitor, visited));
        });
    }

    
    /** 
     * @param rain
     * @return Rain<T>
     */
    @SafeVarargs
    public static <T> Rain<T> merge(Rain<T>... rain) {
        @SuppressWarnings("unchecked")
        LList<Drop<T, Rain<T>>>[] droplets = new LList[rain.length];
        for (int i = 0; i < rain.length; i++) {
            droplets[i] = rain[i].unfix;
        }
        return new Rain<>(LList.merge(droplets));
    }

    public static <T> Rain<T> merge(LList<Rain<T>> rains) {
        return new Rain<>(rains.flatmap(r -> r.unfix));
    }

    public static <T> Rain<T> bind(Promise<Rain<T>> promise) {
        return new Rain<>(LList.bind(promise.map(rain -> rain.unfix)));
    }

    @Deprecated
    public Promise<Boolean> isEmpty() {
        return unfix.empty();
    }

    public Promise<Boolean> empty() {
        return unfix.empty();
    }

    public Rain<T> filter(Predicate<? super Vertex<T>> predicate) {
        var droplets = unfix().filter(droplet -> Promise.just(predicate.test(droplet.vertex())));
        droplets = droplets.map(droplet -> droplet.fmap(let -> let.filter(predicate)));
        return new Rain<>(droplets);
    }

    public Promise<Rain<T>> resolve() {
        Set<Vertex<T>> visited = new HashSet<>();
        Promise<Void> promise = fold(droplets -> droplets.traverse(droplet -> {
            var box = droplet.vertex();
            if (visited.contains(box)) {
                return Promise.nil();
            }
            visited.add(box);
            return droplet.next();
        }));
        return promise.map($ -> this);
    }
}
