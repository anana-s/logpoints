package anana5.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;

public class PList<T> implements Iterable<T> {
    final private Promise<ListF<T, PList<T>>> unfix;

    private PList(Promise<ListF<T, PList<T>>> promise) {
        unfix = promise;
    }

    public static <T> PList<T> fix(Promise<ListF<T, PList<T>>> promise) {
        return new PList<>(promise);
    }

    @Deprecated
    public static <T> PList<T> nil() {
        return PList.of();
    }

    public static <T> PList<T> cons(T item, PList<T> tail) {
        return PList.fix(Promise.just(ListF.cons(item, tail)));
    }

    @SafeVarargs
    public static <T> PList<T> of(T... ts) {
        return PList.from(Arrays.asList(ts));
    }

    public static <T> PList<T> from(Iterable<T> iter) {
        return PList.from(iter.iterator());
    }

    public static <T> PList<T> from(Iterator<T> iter) {
        return PList.fix(Promise.lazy(() -> {
            if (iter.hasNext()) {
                return ListF.cons(iter.next(), PList.from(iter));
            } else {
                return ListF.nil();
            }
        }));
    }

    public Promise<ListF<T, PList<T>>> unfix() {
        return unfix;
    }

    public static <S, T> PList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return PList.fix(func.apply(s).map(listF -> listF.fmap(s$ -> PList.unfold(s$, func))));
    }

    public PList<T> push(T item) {
        return PList.cons(item, this);
    }

    public T head() {
        var maybe = unfix.map(listF -> listF.match(() -> Maybe.<T>nothing(), (t, f) -> Maybe.just(t))).join();
        if (!maybe.check()) {
            throw new NoSuchElementException();
        }
        return maybe.get();
    }

    public PList<T> tail() {
        var maybe = unfix.map(listF -> listF.match(() -> Maybe.<PList<T>>nothing(), (t, f) -> Maybe.just(f))).join();
        if (!maybe.check()) {
            throw new NoSuchElementException();
        }
        return maybe.get();
    }

    public <R> PList<R> map(Function<T, R> func) {
        return PList.bind(unfix.map(listF -> listF.match(() -> PList.nil(), (t, f) -> PList.cons(func.apply(t), f.map(func)))));
        //return LList.bind(foldl(new LList<>(), (t, rs) -> func.apply(t).map(t$ -> new LList<R>(t$, rs))));
    }

    public <R> PList<R> flatmap(Function<T, PList<R>> func) {
        return PList.bind(unfix.map(listF -> listF.match(() -> PList.nil(), (t, f) -> func.apply(t).concat(f.flatmap(func)))));
        //return LList.bind(this.map(func).foldl(new LList<>(), (llist, acc) -> Promise.just(llist.concat(acc))));
    }

    public PList<T> concat(PList<T> other) {
        return PList.bind(unfix.map(listF -> listF.match(() -> other, (t, f) -> PList.cons(t, f.concat(other)))));
        // var out = unfix.then(listF -> listF.match(() -> other.unfix, (head, tail) -> Promise.just(ListF.cons(head, tail.concat(other)))));
        // return new LList<>(out);
    }

    @SafeVarargs
    public static <R> PList<R> merge(PList<R>... lists) {
        var llist = lists[lists.length - 1];
        for (int i = lists.length - 2; i >= 0; i--) {
            llist = lists[i].concat(llist);
        }
        return llist;
    }

    public void traverse(Function<T, Boolean> consumer) {
        _traverse(consumer).join();
    }

    public Promise<Void> _traverse(Function<T, Boolean> consumer) {
        return unfix.then(listF -> listF.match(() -> Promise.<Void>nil(), (head, tail) -> {
            if (consumer.apply(head)) {
                return tail._traverse(consumer);
            } else {
                return Promise.nil();
            }
        }));
    }

    // public Promise<Void> traverse(BiFunction<Promise<Void>, Promise<Void>, Promise<Void>> folder, Function<T, Promise<Void>> consumer) {
    //     return fold(p -> p.then(listF -> listF.match(() -> Promise.lazy(), (t, f) -> folder.apply(Promise.lazy().then($ -> consumer.apply(t)), f))));
    // }

    public <A, R> R collect(Collector<T, A, R> collector) {
        return collector.finisher().apply(
            foldr(collector.supplier().get(), (a, t) -> {
                collector.accumulator().accept(a, t);
                return a;
            })
        );
    }

    public <R> R foldr(R r, BiFunction<R, T, R> func) {
        return unfix.map(listF -> listF.match(() -> r, (t, next) -> next.foldr(func.apply(r, t), func))).join();
    }

    public <R> R foldl(R r, BiFunction<T, R, R> func) {
        return unfix.map(listF -> listF.match(() -> r, (t, next) -> func.apply(t, next.foldl(r, func)))).join();
    }

    public <R> R fold(Function<Promise<ListF<T, R>>, R> func) {
        return func.apply(unfix.map(listF -> listF.fmap(f -> f.fold(func))));
    }

    public static <T> PList<T> bind(Promise<PList<T>> promise) {
        return new PList<>(promise.then(lList -> lList.unfix));
    }

    public static <T> PList<T> bind(PList<Promise<T>> promises) {
        return PList.bind(promises.unfix.map(listF -> listF.match(() -> PList.<T>nil(), (p, next) -> PList.fix(p.map(t -> ListF.cons(t, PList.bind(next)))))));
    }

    public Boolean empty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false)).join();
    }

    public PList<T> filter(Function<? super T, ? extends Boolean> func) {
        return _filter(func).join();
    }

    public Promise<PList<T>> _filter(Function<? super T, ? extends Boolean> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(PList.<T>of()), (t, next) -> {
            if (func.apply(t)) {
                return next._filter(func).then(tail -> {
                    return Promise.just(PList.cons(t, tail));
                });
            } else {
                return next._filter(func);
            }
        }));
    }

    public boolean any(Function<? super T, ? extends Boolean> func) {
        return !filter(func).empty();
    }

    public LList<T> resolve() {
        return foldl(LList.of(), (head, acc) -> LList.cons(head, acc));
    }

    public boolean contains(T t) {
        return _contains(t).join();
    }

    private Promise<Boolean> _contains(T t) {
        return unfix.<Boolean>then(listF -> listF.match(() -> Promise.just(false), (head, tail) -> {
            if (head.equals(t)) {
                return Promise.just(true);
            } else {
                return tail._contains(t);
            }
        }));
    }

    @Override
    public Iterator<T> iterator() {
        return new PListIterator<>(this);
    }

    private static class PListIterator<T> implements Iterator<T> {
        private PList<T> cur;

        private PListIterator(PList<T> plist) {
            cur = plist;
        }

        @Override
        public boolean hasNext() {
            return !cur.empty();
        }

        @Override
        public T next() {
            return cur.unfix.join().match(() -> {
                throw new NoSuchElementException();
            }, (head, tail) -> {
                cur = tail;
                return head;
            });
        }

    }
}
