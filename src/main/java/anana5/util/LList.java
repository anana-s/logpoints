package anana5.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class LList<T> {
    private final ListF<T, LList<T>> unfix;
    private final int length;

    private LList() {
        unfix = ListF.nil();
        length = 0;
    }

    private LList(ListF<T, LList<T>> unfix) {
        this.unfix = unfix;
        length = unfix.match(() -> 0, (x, xs) -> 1 + xs.length());
    }

    private LList(T t, LList<T> path) {
        unfix = ListF.cons(t, path);
        length = path.length + 1;
    }

    public static <T> LList<T> fix(ListF<T, LList<T>> unfix) {
        return new LList<>(unfix);
    }

    @SafeVarargs
    public static <T> LList<T> of(T... ts) {
        var llist = LList.<T>nil();
        for (int i = ts.length - 1; i >= 0; i--) {
            llist = LList.cons(ts[i], llist);
        }
        return llist;
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


    public <R> R foldl(R r, BiFunction<R, T, R> func) {
        return unfix.match(() -> r, (t, f) -> func.apply(f.foldl(r, func), t));
    }

    public void traverse(Consumer<T> visitor) {
        var path = this;
        while (!path.empty()) {
            visitor.accept(path.head().get());
            path = path.tail().get();
        }
    }

    public <R> LList<R> map(Function<T, R> func) {
        return foldl(LList.<R>nil(), (l, t) -> LList.cons(func.apply(t), l));
    }
}
