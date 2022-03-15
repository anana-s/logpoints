package anana5.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LList<T> {
    final private Promise<ListF<T, LList<T>>> unfix;

    private LList(Promise<ListF<T, LList<T>>> promise) {
        unfix = promise;
    }

    public static <T> LList<T> fix(Promise<ListF<T, LList<T>>> promise) {
        return new LList<>(promise);
    }

    @Deprecated
    public static <T> LList<T> nil() {
        return LList.of();
    }

    public static <T> LList<T> cons(T item, LList<T> tail) {
        return LList.fix(Promise.just(ListF.cons(item, tail)));
    }

    @SafeVarargs
    public static <T> LList<T> of(T... ts) {
        return LList.from(Arrays.asList(ts));
    }

    public static <T> LList<T> from(Iterable<T> iter) {
        return LList.from(iter.iterator());
    }

    public static <T> LList<T> from(Iterator<T> iter) {
        return LList.fix(Promise.lazy(() -> {
            if (iter.hasNext()) {
                return ListF.cons(iter.next(), LList.from(iter));
            } else {
                return ListF.nil();
            }
        }));
    }

    public static <S, T> LList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return LList.fix(func.apply(s).fmap(listF -> listF.fmap(s$ -> LList.unfold(s$, func))));
    }

    public LList<T> push(T item) {
        return LList.cons(item, this);
    }

    public Promise<Maybe<T>> head() {
        return unfix.fmap(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(t)));
    }

    public Promise<Maybe<LList<T>>> tail() {
        return unfix.fmap(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(f)));
    }

    public <R> LList<R> map(Function<T, R> func) {
        return LList.bind(unfix.fmap(listF -> listF.match(() -> LList.nil(), (t, f) -> LList.cons(func.apply(t), f.map(func)))));
        //return LList.bind(foldl(new LList<>(), (t, rs) -> func.apply(t).map(t$ -> new LList<R>(t$, rs))));
    }

    public <R> LList<R> flatmap(Function<T, LList<R>> func) {
        return LList.bind(unfix.fmap(listF -> listF.match(() -> LList.nil(), (t, f) -> func.apply(t).concat(f.flatmap(func)))));
        //return LList.bind(this.map(func).foldl(new LList<>(), (llist, acc) -> Promise.just(llist.concat(acc))));
    }

    public LList<T> concat(LList<T> other) {
        return LList.bind(unfix.fmap(listF -> listF.match(() -> other, (t, f) -> LList.cons(t, f.concat(other)))));
        // var out = unfix.then(listF -> listF.match(() -> other.unfix, (head, tail) -> Promise.just(ListF.cons(head, tail.concat(other)))));
        // return new LList<>(out);
    }

    @SafeVarargs
    public static <R> LList<R> merge(LList<R>... lists) {
        var llist = lists[lists.length - 1];
        for (int i = lists.length - 2; i >= 0; i--) {
            llist = lists[i].concat(llist);
        }
        return llist;
    }

    public Promise<Void> traverse(Function<T, Promise<Void>> consumer) {
        return unfix.then(listF -> listF.match(() -> Promise.lazy(), (t, next) -> consumer.apply(t).then($ -> next.traverse(consumer))));
    }

    public Promise<Void> traverse(BiFunction<Promise<Void>, Promise<Void>, Promise<Void>> folder, Function<T, Promise<Void>> consumer) {
        return fold(p -> p.then(listF -> listF.match(() -> Promise.lazy(), (t, f) -> folder.apply(Promise.lazy().then($ -> consumer.apply(t)), f))));
    }

    public Promise<Collection<T>> collect() {
        Collection<T> collection = new ArrayList<>();

        return traverse(t -> {
            collection.add(t);
            return Promise.lazy();
        }).fmap($ -> collection);
    }

    public <R> Promise<R> foldr(R r, BiFunction<T, R, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> func.apply(t, r).then(s -> next.foldr(s, func))));
    }

    public <R> Promise<R> foldl(R r, BiFunction<T, R, Promise<R>> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> next.foldl(r, func).then(s -> func.apply(t, s))));
        //return fold(p -> p.then(listF -> listF.match(() -> Promise.just(r), (s, next) -> next.then(t -> func.apply(s, t)))));
    }

    public <R> Promise<R> fold(Function<Promise<ListF<T, Promise<R>>>, Promise<R>> func) {
        return func.apply(unfix.fmap(listF -> listF.fmap(f -> f.fold(func))));
    }

    public static <T> LList<T> bind(Promise<LList<T>> promise) {
        return new LList<>(promise.then(lList -> lList.unfix));
    }

    public static <T> LList<T> bind(LList<Promise<T>> promises) {
        return LList.bind(Promise.all(promises));
    }

    @Deprecated
    public Promise<Boolean> isEmpty() {
        return unfix.fmap(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public Promise<Boolean> empty() {
        return unfix.fmap(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public LList<T> filter(Function<? super T, ? extends Promise<? extends Boolean>> func) {
        var out = this.<LList<T>>foldl(LList.of(), (head, tail) -> {
            return func.apply(head).then(b -> {
                if (b) {
                    return Promise.just(LList.cons(head, tail));
                } else {
                    return Promise.just(tail);
                }
            });
        });

        return LList.bind(out);
    }

    public LList<T> resolve() {
        return LList.bind(traverse(i -> Promise.lazy()).fmap($ -> this));
    }

    public <R> Promise<R> match(Supplier<R> nil, BiFunction<T, LList<T>, R> cons) {
        return unfix.fmap(listF -> listF.match(() -> nil.get(), (t, f) -> cons.apply(t, f)));
    }

    public <R> LListMatch<R> match() {
        return new LListMatch<>();
    }

    public class LListMatch<R> extends Match<Promise<R>> {
        Supplier<R> nil;
        BiFunction<T, LList<T>, R> cons;
        public LListMatch() {
            set(() -> unfix.fmap(listF -> listF.match(nil, cons)));
        }

        public LListMatch<R> nil(Supplier<R> nil) {
            this.nil = nil;
            return this;
        }

        public LListMatch<R> cons(BiFunction<T, LList<T>, R> cons) {
            this.cons = cons;
            return this;
        }
    }
}
