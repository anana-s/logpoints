package anana5.graph.rainfall;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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
    public <R> Rain<R> map(Function<T, R> func) {
        return this.fold(droplets -> Rain.<R>fix(droplets.map(drop -> Drop.of(func.apply(drop.get()), drop.next()))));
    }


    /**
     * @param visitor
     * @return Promise<Void>
     */
    public Promise<Void> traverse(BiFunction<T, T, Promise<Void>> visitor) {
        return traverse(null, visitor, new HashSet<>());
    }

    private Promise<Void> traverse(T source, BiFunction<T, T, Promise<Void>> visitor, Set<T> visited) {
        return unfix.traverse(droplet -> {
            var target = droplet.get();
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
    public Promise<Void> traverse(Function<T, Promise<Void>> visitor) {
        return traverse(visitor, new HashSet<>());
    }

    private Promise<Void> traverse(Function<T, Promise<Void>> visitor, Set<T> visited) {
        return unfix.traverse(droplet -> {
            var target = droplet.get();
            if (visited.contains(target)) {
                return Promise.lazy();
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
        return new Rain<>(LList.bind(promise.fmap(rain -> rain.unfix)));
    }

    @Deprecated
    public Promise<Boolean> isEmpty() {
        return unfix.empty();
    }

    public Promise<Boolean> empty() {
        return unfix.empty();
    }

    public Rain<T> filter(Function<? super T, ? extends Promise<? extends Boolean>> predicate) {
        var droplets = unfix().filter(droplet -> predicate.apply(droplet.get()));
        droplets = droplets.map(droplet -> droplet.fmap(let -> let.filter(predicate)));
        return new Rain<>(droplets);
    }

    public Rain<T> resolve() {
        Promise<Void> promise = traverse(t -> Promise.lazy());
        return Rain.bind(promise.fmap($ -> this));
    }
}
