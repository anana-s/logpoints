package anana5.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class LList<T> {
    private final ListF<T, LList<T>> unfix;
    private final int length;

    public LList() {
        unfix = ListF.nil();
        length = 0;
    }

    private LList(T t, LList<T> path) {
        unfix = ListF.cons(t, path);
        length = path.length + 1;
    }

    public static <T> LList<T> nil() {
        return new LList<>();
    }

    public static <T> LList<T> cons(T t, LList<T> path) {
        return new LList<>(t, path);
    }

    public LList<T> push(T t) {
        return new LList<T>(t, this);
    }

    public boolean contains(T t) {
        return unfix.match(() -> false, (a, f) -> a.equals(t) || f.contains(t));
    }

    public Maybe<T> head() {
        return unfix.match(() -> Maybe.nothing(), (a, f) -> Maybe.just(a));
    }

    public Maybe<LList<T>> tail() {
        return unfix.match(() -> Maybe.nothing(), (a, f) -> Maybe.just(f));
    }

    public boolean empty() {
        return length == 0;
    }

    public int length() {
        return length;
    }

    public <R> R foldr(R r, BiFunction<R, T, R> func) {
        return unfix.match(() -> r, (t, f) -> f.foldr(func.apply(r, t), func));
    }


    public <R> R foldl(BiFunction<R, T, R> func, R r) {
        return unfix.match(() -> r, (t, f) -> func.apply(f.foldl(func, r), t));
    }

    public void traverse(Consumer<T> visitor) {
        var path = this;
        while (!path.empty()) {
            visitor.accept(path.head().get());
            path = path.tail().get();
        }
    }
}
