package anana5.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
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

    public static <S, T> PList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return PList.fix(func.apply(s).map(listF -> listF.fmap(s$ -> PList.unfold(s$, func))));
    }

    public PList<T> push(T item) {
        return PList.cons(item, this);
    }

    public Promise<Maybe<T>> head() {
        return unfix.map(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(t)));
    }

    public Promise<Maybe<PList<T>>> tail() {
        return unfix.map(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(f)));
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

    public Promise<Void> traverse(Function<T, Promise<Void>> consumer) {
        return unfix.then(listF -> listF.match(() -> Promise.lazy(), (t, next) -> consumer.apply(t).then($ -> next.traverse(consumer))));
    }

    // public Promise<Void> traverse(BiFunction<Promise<Void>, Promise<Void>, Promise<Void>> folder, Function<T, Promise<Void>> consumer) {
    //     return fold(p -> p.then(listF -> listF.match(() -> Promise.lazy(), (t, f) -> folder.apply(Promise.lazy().then($ -> consumer.apply(t)), f))));
    // }

    public Promise<List<T>> collect() {
        List<T> collection = new ArrayList<>();

        return traverse(t -> {
            collection.add(t);
            return Promise.lazy();
        }).map($ -> collection);
    }

    public <A, R> R collect(Collector<T, A, R> collector) {
        return collector.finisher().apply(
            foldr(collector.supplier().get(), (a, t) -> {
                collector.accumulator().accept(a, t);
                return Promise.just(a);
            }).join()
        );
    }

    public <R> Promise<R> foldr(R r, BiFunction<R, T, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> func.apply(r, t).then(s -> next.foldr(s, func))));
    }

    public <R> Promise<R> foldl(R r, BiFunction<T, R, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> next.foldl(r, func).then(s -> func.apply(t, s))));
    }

    // public <R> Promise<R> fold(Function<Promise<ListF<T, Promise<R>>>, Promise<R>> func) {
    //     return func.apply(unfix.map(listF -> listF.fmap(f -> f.fold(func))));
    // }

    public static <T> PList<T> bind(Promise<PList<T>> promise) {
        return new PList<>(promise.then(lList -> lList.unfix));
    }

    public static <T> PList<T> bind(PList<Promise<T>> promises) {
        return PList.bind(promises.unfix.map(listF -> listF.match(() -> PList.<T>nil(), (p, next) -> PList.fix(p.map(t -> ListF.cons(t, PList.bind(next)))))));
    }

    @Deprecated
    public Promise<Boolean> isEmpty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public Promise<Boolean> empty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public PList<T> filter(Function<? super T, ? extends Promise<? extends Boolean>> func) {
        var out = this.<PList<T>>foldl(PList.of(), (head, tail) -> {
            return func.apply(head).then(b -> {
                if (b) {
                    return Promise.just(PList.cons(head, tail));
                } else {
                    return Promise.just(tail);
                }
            });
        });

        return PList.bind(out);
    }

    public Promise<LList<T>> resolve() {
        return unfix.then(listF -> listF.match(() -> Promise.just(LList.of()), (t, next) -> next.resolve().then(l -> Promise.just(LList.cons(t, l)))));
    }

    public <R> Promise<R> match(Supplier<R> nil, BiFunction<T, PList<T>, R> cons) {
        return unfix.map(listF -> listF.match(() -> nil.get(), (t, f) -> cons.apply(t, f)));
    }

    public <R> LListMatch<R> match() {
        return new LListMatch<>();
    }

    public class LListMatch<R> extends Match<Promise<R>> {
        Supplier<R> nil;
        BiFunction<T, PList<T>, R> cons;
        public LListMatch() {
            set(() -> unfix.map(listF -> listF.match(nil, cons)));
        }

        public LListMatch<R> nil(Supplier<R> nil) {
            this.nil = nil;
            return this;
        }

        public LListMatch<R> cons(BiFunction<T, PList<T>, R> cons) {
            this.cons = cons;
            return this;
        }
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
}
