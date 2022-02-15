package anana5.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LList<T> {
    final private Promise<ListF<T, LList<T>>> unfix;

    public LList(T item, LList<T> tail) {
        unfix = Promise.just(ListF.cons(item, tail));
    }
    
    public LList() {
        this(Promise.just(ListF.nil()));
    }

    public LList(Iterable<T> iter) {
        this(iter.iterator());
    }

    public LList(Iterator<T> iter) {
        this(iter, $ -> Promise.of(() -> {
            if (iter.hasNext()) {
                return ListF.cons(iter.next(), iter);
            } else {
                return ListF.nil();
            }
        }));
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

    public Promise<LList<T>> tail() {
        return unfix.map(listF -> listF.match(() -> null, (a, f) -> f));
    }

    public <R> LList<R> map(Function<T, R> func) {
        return unfold(this, lList -> lList.unfix.map(promise -> promise.map(func)));
        //return fold(p -> new LList<R>(p.map(listF -> listF.map(func))));
    }

    public static <S, T> LList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return new LList<T>(s, func);
    }

    public <R> LList<R> flatmap(Function<T, LList<R>> func) {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    @SafeVarargs
    public static <R> LList<R> merge(LList<R>... lists) {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    public <R> Promise<Void> traverse(Function<T, Promise<Void>> consumer) {
        return fold(null, (t, null$) -> {
            return consumer.apply(t);
        });
    }

    public Promise<Collection<T>> collect() {
        return fold(new ArrayList<>(), (t, out) -> {
            out.add(t);
            return Promise.just(out);
        });
    }

    public <R> Promise<R> fold(R r, BiFunction<T, R, Promise<R>> func) {
        return fold(p -> p.bind(listF -> listF.match(() -> Promise.just(r), (t, r$) -> r$.bind(r$$ -> func.apply(t, r$$)))));
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
}
