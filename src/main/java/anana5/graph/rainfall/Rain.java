package anana5.graph.rainfall;

import java.util.function.Function;

import anana5.util.PList;
import anana5.util.Promise;

/**
 * A lazy graph implementation using promises.
 */
public class Rain<T> {

    final private PList<Drop<T, Rain<T>>> unfix;

    public PList<Drop<T, Rain<T>>> unfix() {
        return this.unfix;
    }

    private Rain(PList<Drop<T, Rain<T>>> drops) {
        this.unfix = drops;
    }

    @SafeVarargs
    public static <T> Rain<T> of(Drop<T, Rain<T>>... drops) {
        return new Rain<>(PList.of(drops));
    }

    public static <T> Rain<T> from(Iterable<Drop<T, Rain<T>>> drops) {
        return new Rain<>(PList.from(drops));
    }

    public static <T> Rain<T> fix(PList<Drop<T, Rain<T>>> drops) {
        return new Rain<>(drops);
    }

    /**
     * @param func
     * @return R
     */
    public <R> R fold(Function<PList<Drop<T, R>>, R> func) {
        return func.apply(unfix.map(drop -> Drop.of(drop.get(), drop.next().fold(func))));
    }

    public <R> Rain<R> unfold(Function<Rain<T>, PList<Drop<R, Rain<T>>>> func) {
        return Rain.unfold(this, func);
    }

    /**
     * @param func
     * @return Rain<T>
     */
    public static <S, T> Rain<T> unfold(S s, Function<S, PList<Drop<T, S>>> func) {
        return Rain.fix(func.apply(s).map(drop -> Drop.of(drop.get(), Rain.unfold(drop.next(), func))));
    }

    /**
     * @param func
     * @return Rain<R>
     */
    public <R> Rain<R> map(Function<T, R> func) {
        return Rain.fix(unfix.map(drop -> Drop.of(func.apply(drop.get()), drop.next().map(func))));
    }

    /**
     * @param rain
     * @return Rain<T>
     */
    @SafeVarargs
    public static <T> Rain<T> merge(Rain<T>... rains) {
        var unfix = rains[rains.length - 1].unfix;
        for (int i = rains.length - 2; i >= 0; i--) {
            unfix = unfix.concat(rains[i].unfix());
        }
        return Rain.fix(unfix);
    }

    public static <T> Rain<T> merge(PList<Rain<T>> rains) {
        return new Rain<>(rains.flatmap(r -> r.unfix));
    }

    public static <T> Rain<T> bind(Promise<Rain<T>> promise) {
        return new Rain<>(PList.bind(promise.map(rain -> rain.unfix)));
    }

    public Promise<Boolean> empty() {
        return unfix.empty();
    }
}
