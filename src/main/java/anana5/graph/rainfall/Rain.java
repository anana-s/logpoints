package anana5.graph.rainfall;

import java.util.Arrays;
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

import anana5.graph.Box;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;

/**
 * A lazy graph implementation using promises.
 */
public class Rain<T> implements Graph<T> {    
    
    final private LList<Droplet<T, Rain<T>>> droplets;

    public LList<Droplet<T, Rain<T>>> unfix() {
        return this.droplets;
    }

    @SafeVarargs
    public Rain(Droplet<T, Rain<T>>... droplets) {
        this(Arrays.asList(droplets));
    }

    public Rain(Rain<T> other) {
        this.droplets = other.droplets;
    }
    
    public Rain(Iterable<Droplet<T, Rain<T>>> droplets) {
        this(new LList<>(droplets));
    }

    public <S> Rain(S s, Function<S, LList<Droplet<T, S>>> func) {
        this(func.apply(s).map(droplet -> Promise.just(droplet.fmap(let -> new Rain<>(let, func)))));
    }
    
    public Rain(LList<Droplet<T, Rain<T>>> droplets) {
        this.droplets = droplets;
    }

    
    /** 
     * @return Promise<Collection<Droplet<T, Rain<T>>>>
     */
    public Promise<Collection<Droplet<T, Rain<T>>>> collect() {
        return droplets.collect();
    }

    /** 
     * @param func
     * @return R
     */
    public <R> R fold(Function<LList<Droplet<T, R>>, R> func) {
        return func.apply(droplets.map(drop -> Promise.pure(() -> drop.fmap(let -> let.fold(func)))));
    }

    
    /** 
     * @param func
     * @return Rain<T>
     */
    public static <T, S> Rain<T> unfold(S s, Function<S, LList<Droplet<T, S>>> func) {
        return new Rain<>(s, func);
    }

    /** 
     * @param func
     * @return Rain<R>
     */
    public <R> Rain<R> map(Function<Box<T>, Box<R>> func) {
        Map<Box<T>, Droplet<R, Rain<R>>> cache = new HashMap<>();
        return this.fold(droplets -> new Rain<R>(droplets.map(drop -> Promise.just(cache.computeIfAbsent(drop.get(), box -> new Droplet<>(func.apply(drop.get()), drop.next()))))));
    }

    @Override
    public void traverse(Consumer<Vertex<T>> consumer) {
        Promise<Void> task = traverse((source, target) -> {
            consumer.accept(target);
            return Promise.nil();
        });

        task.run();
    }

    @Override
    public void traverse(BiConsumer<Vertex<T>, Vertex<T>> consumer) {
        Promise<Void> task = traverse((source, target) -> {
            consumer.accept(source, target);
            return Promise.nil();
        });

        task.run();
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traverse(BiFunction<Vertex<T>, Vertex<T>, Promise<Void>> visitor) {
        return traverse(new Box<>(null), visitor, new HashSet<>());
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    private Promise<Void> traverse(Box<T> source, BiFunction<Vertex<T>, Vertex<T>, Promise<Void>> visitor, Set<Box<T>> cache) {
        return droplets.traverse((droplet) -> {
            var target = droplet.get();
            if (cache.contains(target)) {
                return visitor.apply(source, target);
            }
            cache.add(target);
            var rain = droplet.next();
            return visitor.apply(source, target).then((void$) -> rain.traverse(target, visitor, cache));
        });
    }

    
    /** 
     * @param rain
     * @return Rain<T>
     */
    @SafeVarargs
    public static <T> Rain<T> merge(Rain<T>... rain) {
        @SuppressWarnings("unchecked")
        LList<Droplet<T, Rain<T>>>[] droplets = new LList[rain.length];
        for (int i = 0; i < rain.length; i++) {
            droplets[i] = rain[i].droplets;
        }
        return new Rain<>(LList.merge(droplets));
    }

    public static <T> Rain<T> merge(LList<Rain<T>> rains) {
        return new Rain<>(rains.flatmap(r -> r.droplets.unbind()));
    }

    public static <T> Rain<T> bind(Promise<Rain<T>> promise) {
        return new Rain<>(LList.bind(promise.map(rain -> rain.droplets)));
    }

    public Promise<Boolean> isEmpty() {
        return droplets.isEmpty();
    }

    public Rain<T> filter(Predicate<Box<T>> predicate) {
        var droplets = unfix().filter(droplet -> Promise.just(predicate.test(droplet.get())));
        droplets = droplets.map(droplet -> Promise.just(droplet.fmap(let -> let.filter(predicate))));
        return new Rain<>(droplets);
    }
    
}
