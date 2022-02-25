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
import java.util.function.Supplier;

import anana5.graph.Box;
import anana5.graph.Graph;
import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;

/**
 * A lazy graph implementation using promises.
 */
public class Rain<T> implements Graph<T> {    
    
    final private LList<Droplet<T, Rain<T>>> unfix;

    public LList<Droplet<T, Rain<T>>> unfix() {
        return this.unfix;
    }

    @SafeVarargs
    public Rain(Droplet<T, Rain<T>>... droplets) {
        this(Arrays.asList(droplets));
    }

    public Rain(Rain<T> other) {
        this.unfix = other.unfix;
    }
    
    public Rain(Iterable<Droplet<T, Rain<T>>> droplets) {
        this(new LList<>(droplets));
    }

    public <S> Rain(S s, Function<S, LList<Droplet<T, S>>> func) {
        this(func.apply(s).map(droplet -> droplet.fmap(let -> new Rain<>(let, func))));
    }
    
    public Rain(LList<Droplet<T, Rain<T>>> droplets) {
        this.unfix = droplets;
    }

    
    /** 
     * @return Promise<Collection<Droplet<T, Rain<T>>>>
     */
    public Promise<Collection<Droplet<T, Rain<T>>>> collect() {
        return unfix.collect();
    }

    /** 
     * @param func
     * @return R
     */
    public <R> R fold(Function<LList<Droplet<T, R>>, R> func) {
        return func.apply(unfix.map(drop -> drop.fmap(let -> let.fold(func))));
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
    public <R> Rain<R> map(Function<T, R> func) {
        Map<Box<T>, Droplet<R, Rain<R>>> cache = new HashMap<>();
        return this.fold(droplets -> new Rain<R>(droplets.map(drop -> cache.computeIfAbsent(drop.get(), box -> new Droplet<>(func.apply(box.value()), drop.next())))));
    }

    @Override
    public void traverse(Consumer<Vertex<T>> consumer) {
        traverse((source, target) -> {
            consumer.accept(target);
        });
    }

    @Override
    public void traverse(BiConsumer<Vertex<T>, Vertex<T>> consumer) {
        traversal((source, target) -> {
            consumer.accept(source, target);
        }).join();
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traversal(BiConsumer<Vertex<T>, Vertex<T>> visitor) {
        return traversal(new Box<>(null), visitor, new HashSet<>());
    }

    
    /** 
     * @param visitor
     * @return Promise<Void>
     */
    private Promise<Void> traversal(Box<T> source, BiConsumer<Vertex<T>, Vertex<T>> visitor, Set<Box<T>> visited) {
        return unfix.foldr(Promise.<Void>nil(), (droplet, p) -> {
            var target = droplet.get();
            if (visited.contains(target)) {
                visitor.accept(source, target);
                return p.then($ -> Promise.nil());
            }
            visited.add(target);
            visitor.accept(source, target);
            return p.then($ -> droplet.next().traversal(target, visitor, visited));
        }).bind(Function.<Promise<Void>>identity());
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

    public Promise<Boolean> isEmpty() {
        return unfix.isEmpty();
    }

    public Rain<T> filter(Predicate<Box<T>> predicate) {
        var droplets = unfix().filter(droplet -> Promise.just(predicate.test(droplet.get())));
        droplets = droplets.map(droplet -> droplet.fmap(let -> let.filter(predicate)));
        return new Rain<>(droplets);
    }

    public boolean resolved() {
        return fold(llist -> llist.fold(p -> p.resolved() && p.get().match(() -> true, (t, f) -> t.next() && f)));
    }

    public Promise<Rain<T>> join() {
        BiFunction<Droplet<T, Promise<Void>>, Promise<Void>, Promise<Void>> func = (drop, tail) -> drop.next().then($ -> tail);
        Supplier<Promise<Void>> supp = () -> Promise.nil();
        var promise = fold(llist -> llist.fold(p -> p.then(listF -> listF.match(supp, func))));
        return promise.map($ -> this);
    }
}
