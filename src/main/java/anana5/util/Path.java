package anana5.util;

public class Path<T> {
    private ListF<T, Path<T>> unfix;

    public Path() {
        unfix = ListF.nil();
    }

    private Path(T t, Path<T> path) {
        unfix = ListF.cons(t, path);
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
        return unfix.match(() -> true, (a, f) -> false);
    }
}
