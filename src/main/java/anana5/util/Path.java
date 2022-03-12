package anana5.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Path<T> {
    private final ListF<T, Path<T>> unfix;
    private final int length;

    public Path() {
        unfix = ListF.nil();
        length = 0;
    }

    private Path(T t, Path<T> path) {
        unfix = ListF.cons(t, path);
        length = path.length + 1;
    }

    public static <T> Path<T> nil() {
        return new Path<>();
    }

    public static <T> Path<T> cons(T t, Path<T> path) {
        return new Path<>(t, path);
    }

    public Path<T> push(T t) {
        return new Path<T>(t, this);
    }

    public boolean contains(T t) {
        return unfix.match(() -> false, (a, f) -> a.equals(t) || f.contains(t));
    }

    public Maybe<T> head() {
        return unfix.match(() -> Maybe.nothing(), (a, f) -> Maybe.just(a));
    }

    public Maybe<Path<T>> tail() {
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
