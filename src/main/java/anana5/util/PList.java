package anana5.util;

import java.util.Arrays;
import java.util.HashSet;
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

    public Promise<T> head() {
        return unfix.map(listF -> listF.match(() -> Maybe.<T>nothing(), (t, f) -> Maybe.just(t))).map(maybe -> {
            if (!maybe.check()) {
                throw new NoSuchElementException();
            }
            return maybe.get();
        });
    }

    public PList<T> tail() {
        return PList.bind(unfix.map(listF -> listF.match(() -> Maybe.<PList<T>>nothing(), (t, f) -> Maybe.just(f))).map(maybe -> {
            if (!maybe.check()) {
                throw new NoSuchElementException();
            }
            return maybe.get();
        }));
    }

    public <R> PList<R> map(Function<? super T, ? extends R> func) {
        return PList.bind(unfix.map(listF -> listF.match(() -> PList.nil(), (t, f) -> PList.cons(func.apply(t), f.map(func)))));
        //return LList.bind(foldl(new LList<>(), (t, rs) -> func.apply(t).map(t$ -> new LList<R>(t$, rs))));
    }

    public <R> PList<R> flatmap(Function<? super T, PList<R>> func) {
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

    public Promise<Void> traverse(Function<T, ? extends Promise<? extends Boolean>> consumer) {
        return unfix.then(listF -> listF.match(() -> Promise.<Void>nil(), (head, tail) -> {
            return consumer.apply(head).then(b -> {
                if (b) {
                    return tail.traverse(consumer);
                } else {
                    return Promise.nil();
                }
            });
        }));
    }

    // public Promise<Void> traverse(BiFunction<Promise<Void>, Promise<Void>, Promise<Void>> folder, Function<T, Promise<Void>> consumer) {
    //     return fold(p -> p.then(listF -> listF.match(() -> Promise.lazy(), (t, f) -> folder.apply(Promise.lazy().then($ -> consumer.apply(t)), f))));
    // }

    public <A, R> Promise<R> collect(Collector<? super T, A, R> collector) {
        return foldr(Promise.just(collector.supplier().get()), (a, t) -> {
            return a.effect(acc -> collector.accumulator().accept(acc, t));
        }).map(a -> collector.finisher().apply(a));
    }

    public <R> Promise<R> foldr(Promise<R> r, BiFunction<Promise<R>, T, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> r, (t, next) -> next.foldr(func.apply(r, t), func)));
    }

    public <R> Promise<R> foldl(Promise<R> r, BiFunction<T, Promise<R>, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> r, (t, next) -> func.apply(t, next.foldl(r, func))));
    }

    public <R> R fold(Function<Promise<ListF<T, R>>, R> func) {
        return func.apply(unfix.map(listF -> listF.fmap(f -> f.fold(func))));
    }

    public static <T> PList<T> bind(Promise<PList<T>> promise) {
        return PList.fix(promise.then(lList -> lList.unfix));
    }

    public static <T> PList<T> bind(PList<Promise<T>> promises) {
        return PList.bind(promises.unfix.map(listF -> listF.match(() -> PList.<T>nil(), (p, next) -> PList.fix(p.map(t -> ListF.cons(t, PList.bind(next)))))));
    }

    public Promise<Boolean> empty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public PList<T> filter(Function<? super T, ? extends Promise<? extends Boolean>> func) {
        return PList.<T>bind(unfix.then(listF -> listF.match(() -> Promise.just(PList.<T>of()), (t, next) -> {
            return func.apply(t).map(b -> {
                if (b) {
                    return PList.cons(t, next.filter(func));
                } else {
                    return next.filter(func);
                }
            });
        })));
    }

    public Promise<Boolean> any(Function<? super T, ? extends Promise<? extends Boolean>> func) {
        return filter(func).empty().map(b -> !b);
    }

    public Promise<Void> resolve() {
        return foldl(Promise.lazy(), (head, acc) -> acc);
    }

    public Promise<Boolean> contains(T t) {
        return unfix.<Boolean>then(listF -> listF.match(() -> Promise.just(false), (head, tail) -> {
            if (head.equals(t)) {
                return Promise.just(true);
            } else {
                return tail.contains(t);
            }
        }));
    }

    public PList<T> unique() {
        var seen = new HashSet<T>();
        return filter(t -> Promise.lazy(() -> {
            if (seen.contains(t)) {
                return false;
            } else {
                seen.add(t);
                return true;
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
            return !cur.empty().join();
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

    public Promise<Long> count() {
        return foldr(Promise.just(0L), (p, t) -> p.map(a -> a + 1L));
    }
}
