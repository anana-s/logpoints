package anana5.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class PList<T> {
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

    public Promise<Collection<T>> collect() {
        Collection<T> collection = new ArrayList<>();

        return traverse(t -> {
            collection.add(t);
            return Promise.lazy();
        }).map($ -> collection);
    }

    public <R> Promise<R> foldr(R r, BiFunction<T, R, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> func.apply(t, r).then(s -> next.foldr(s, func))));
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
        return PList.bind(Promise.all(promises));
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

    public PList<T> resolve() {
        return PList.bind(traverse(i -> Promise.lazy()).map($ -> this));
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
}
