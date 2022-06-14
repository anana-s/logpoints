package anana5.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class LList<T> implements Iterable<T> {
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

    public static <T> LList<T> from(Iterable<T> ts) {
        return LList.from(ts.iterator());
    }

    public static <T> LList<T> from(Iterator<T> ts) {
        if (!ts.hasNext()) {
            return LList.nil();
        }

        return LList.cons(ts.next(), from(ts));
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

    public T head() {
        return unfix.match(() -> {
            throw new NoSuchElementException();
        }, (a, f) -> a);
    }

    public LList<T> tail() {
        return unfix.match(() -> this, (a, f) -> f);
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
            visitor.accept(path.head());
            path = path.tail();
        }
    }

    public <R> LList<R> map(Function<T, R> func) {
        return foldl(LList.<R>nil(), (l, t) -> LList.cons(func.apply(t), l));
    }

    @Override
    public Iterator<T> iterator() {
        return new LListIterator<>(this);
    }

    private static class LListIterator<T> implements Iterator<T> {
        private LList<T> cur;
        private LListIterator(LList<T> list) {
            cur = list;
        }

        @Override
        public boolean hasNext() {
            return !cur.empty();
        }

        @Override
        public T next() {
            return cur.unfix.match(() -> {
                throw new NoSuchElementException();
            }, (t, f) -> {
                cur = f;
                return t;
            });
        }

    }

    public Stream<T> stream() {
        return Stream.iterate(this, LList::tail).takeWhile(llist -> !llist.empty()).map(LList::head);
    }

    public <A, R> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    public static <S, T> LList<T> unfold(S s, Function<S, ListF<T, S>> func) {
        return LList.fix(func.apply(s).fmap(r -> unfold(r, func)));
    }
}
