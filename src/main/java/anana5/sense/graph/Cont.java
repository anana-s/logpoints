package anana5.sense.graph;

import java.util.function.Function;

@FunctionalInterface
public interface Cont <R, T> {

    R run (Function<T, R> k);

    static <R, T> Cont<R, T> of(T t) {
        return k -> k.apply(t);
    }

    default <S> Cont<R, S> bind(Function<T, Cont<R, S>> f) {
        return k -> run(t -> f.apply(t).run(k));
    }

    default <S> Cont<R, S> with(Function<Function<S, R>, Function<T, R>> f) {
        return k -> run(f.apply(k));
    }

    default Cont<R, T> mapCont(Function<R, R> f) {
        return k -> f.apply(run(k));
    }

    default <S> Cont<R, S> map(Function<T, S> f) {
        return k -> run(t -> k.apply(f.apply(t)));
    }
}
