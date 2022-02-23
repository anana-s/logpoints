package anana5.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LList<T> {
    final private Promise<ListF<T, LList<T>>> unfix;

    public LList(T item, LList<T> tail) {
        unfix = Promise.just(ListF.cons(item, tail));
    }
    
    @SafeVarargs
    public LList(T... ts) {
        this(Arrays.asList(ts));
    }

    public LList(Iterable<T> iter) {
        this(iter.iterator());
    }

    public LList(Iterator<T> iter) {
        this(iter, $ -> {
            if (iter.hasNext()) {
                return Promise.just(ListF.cons(iter.next(), iter));
            } else {
                return Promise.just(ListF.nil());
            }
        });
    }

    public LList(Promise<ListF<T, LList<T>>> promise) {
        unfix = promise;
    }

    public <S> LList(S s, Function<S, Promise<ListF<T, S>>> func) {
        unfix = func.apply(s).map(listF -> listF.fmap(s$ -> new LList<>(s$, func)));
    }

    public Promise<T> head() {
        return unfix.map(listF -> listF.match(() -> null, (a, f) -> a));
    }

    public LList<T> tail() {
        return LList.<T>bind(unfix.map(listF -> listF.match(() -> new LList<>(), (a, f) -> f)));
    }

    public <R> LList<R> map(Function<T, Promise<R>> func) {
        return LList.bind(foldl(new LList<>(), (t, rs) -> func.apply(t).map(t$ -> new LList<R>(t$, rs))));
    }

    public static <S, T> LList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return new LList<T>(s, func);
    }

    public <R> LList<R> flatmap(Function<T, Promise<LList<R>>> func) {
        Promise<LList<R>> out = this.map(func).foldl(new LList<>(), (llist, acc) -> Promise.just(llist.concat(acc)));
        return LList.bind(out);
    }

    public LList<T> concat(LList<T> other) {
        var out = unfix.bind(listF -> listF.match(() -> other.unfix, (head, tail) -> Promise.just(ListF.cons(head, tail.concat(other)))));
        return new LList<>(out);
    }

    @SafeVarargs
    public static <R> LList<R> merge(LList<R>... lists) {
        return new LList<>(Arrays.asList(lists)).flatmap(LList::unbind);
    }

    public <R> Promise<Void> traverse(Function<T, Promise<Void>> consumer) {
        return foldr(null, (t, null$) -> {
            return consumer.apply(t);
        });
    }

    public Promise<Collection<T>> collect() {
        return foldr(new ArrayList<>(), (t, out) -> {
            out.add(t);
            return Promise.just(out);
        });
    }

    public <R> Promise<R> foldr(R r, BiFunction<T, R, Promise<R>> func) {
        return unfix.bind(listF -> listF.match(() -> Promise.just(r), (s, next) -> {
            var cur = func.apply(s, r);
            return cur.bind(t -> next.foldr(t, func));
        }));
    }

    public <R> Promise<R> foldl(R r, BiFunction<T, R, Promise<R>> func) {
        return fold(p -> p.bind(listF -> listF.match(() -> Promise.just(r), (s, next) -> next.bind(t -> func.apply(s, t)))));
    }

    public <R> R fold(Function<Promise<ListF<T, R>>, R> func) {
        return func.apply(unfix.map(listF -> listF.fmap(f -> f.fold(func))));
    }

    public static <T> LList<T> bind(Promise<LList<T>> promise) {
        return new LList<>(promise.bind(lList -> lList.unfix));
    }

    public Promise<Boolean> isEmpty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public LList<T> filter(Function<? super T, Promise<Boolean>> func) {
        Promise<LList<T>> out = fold(p -> {
            return p.bind(listF -> listF.match(() -> Promise.just(new LList<>()), (head, tail$) -> {
                return func.apply(head).bind(condition -> {
                    if (condition) {
                        return tail$.map(tail -> new LList<T>(head, tail));
                    } else {
                        return tail$;
                    }
                });
            }));
        });

        return LList.bind(out);
    }

    public Promise<LList<T>> unbind() {
        return Promise.just(this);
    }
}
