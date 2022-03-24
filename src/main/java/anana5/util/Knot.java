package anana5.util;

import java.util.function.Function;

public class Knot<T> {
    Promise<T> knot;

    private Knot() {
        knot = Promise.just(null);
    }

    public static <A, B> Promise<A> tie(Function<Promise<A>, B> fa, Function<Promise<B>, A> fb) {
        Knot<B> knot = new Knot<>();
        var out = Promise.lazy(() -> fb.apply(knot.knot));
        knot.knot = Promise.lazy(() -> fa.apply(out));
        return out;
    }
}
