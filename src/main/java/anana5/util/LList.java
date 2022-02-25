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

    public LList(T item, LList<T> tail) {
        unfix = Promise.just(ListF.cons(item, tail));
    }

    public LList() {
        unfix = Promise.just(ListF.nil());
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

    public static <T> LList<T> nil() {
        return new LList<>(Promise.just(ListF.nil()));
    }

    public static <T> LList<T> cons(T item, LList<T> tail) {
        return new LList<>(item, tail);
    }

    public Promise<Maybe<T>> head() {
        return unfix.map(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(t)));
    }

    public Promise<Maybe<LList<T>>> tail() {
        return unfix.map(listF -> listF.match(() -> Maybe.nothing(), (t, f) -> Maybe.just(f)));
    }

    public <R> LList<R> map(Function<T, R> func) {
        return LList.bind(unfix.map(listF -> listF.match(() -> new LList<>(), (t, f) -> new LList<R>(func.apply(t), f.map(func)))));
        //return LList.bind(foldl(new LList<>(), (t, rs) -> func.apply(t).map(t$ -> new LList<R>(t$, rs))));
    }

    public static <S, T> LList<T> unfold(S s, Function<S, Promise<ListF<T, S>>> func) {
        return new LList<T>(s, func);
    }

    public <R> LList<R> flatmap(Function<T, LList<R>> func) {
        return LList.bind(unfix.map(listF -> listF.match(() -> new LList<R>(), (t, f) -> func.apply(t).concat(f.flatmap(func)))));
        //return LList.bind(this.map(func).foldl(new LList<>(), (llist, acc) -> Promise.just(llist.concat(acc))));
    }

    public LList<T> concat(LList<T> other) {
        return LList.bind(unfix.map(listF -> listF.match(() -> other, (t, f) -> new LList<>(t, f.concat(other)))));
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

    public Promise<Void> traverse(Consumer<T> consumer) {
        return unfix.then(listF -> listF.match(() -> Promise.nil(), (t, f) -> {
            consumer.accept(t);
            return Promise.nil();
        }));
    }

    public Promise<Collection<T>> collect() {
        return foldr(new ArrayList<>(), (t, out) -> {
            out.add(t);
            return out;
        });
    }

    public <R> Promise<R> foldr(R r, BiFunction<T, R, R> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> Promise.just(func.apply(t, r)).then(s -> next.foldr(s, func))));
    }

    public <R> Promise<R> foldl(R r, BiFunction<T, R, R> func) {
        return unfix.then(listF -> listF.match(() -> Promise.just(r), (t, next) -> next.foldl(r, func).map(s -> func.apply(t, s))));
        //return fold(p -> p.then(listF -> listF.match(() -> Promise.just(r), (s, next) -> next.then(t -> func.apply(s, t)))));
    }

    public <R> R fold(Function<Promise<ListF<T, R>>, R> func) {
        return func.apply(unfix.map(listF -> listF.fmap(f -> f.fold(func))));
    }

    public static <T> LList<T> bind(Promise<LList<T>> promise) {
        return new LList<>(promise.then(lList -> lList.unfix));
    }

    public Promise<Boolean> isEmpty() {
        return unfix.map(listF -> listF.match(() -> true, (t, f) -> false));
    }

    public LList<T> filter(Function<? super T, Promise<Boolean>> func) {
        Promise<LList<T>> out = fold(p -> {
            return p.then(listF -> listF.match(() -> Promise.just(new LList<>()), (head, tail$) -> {
                return func.apply(head).then(condition -> {
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

    public boolean resolved() {
        return unfix.resolved() && unfix.get().match(() -> true, (t, f) -> f.resolved());
    }

    public Promise<LList<T>> join() {
        return unfix.then(listF -> listF.match(() -> Promise.just(this), (t, f) -> f.join()));
    }

    public <R> Promise<R> match(Supplier<R> nil, BiFunction<T, LList<T>, R> cons) {
        return unfix.map(listF -> listF.match(() -> nil.get(), (t, f) -> cons.apply(t, f)));
    }

    public <R> LListMatch<R> match() {
        return new LListMatch<>();
    }

    public class LListMatch<R> extends Match<Promise<R>> {
        Supplier<R> nil;
        BiFunction<T, LList<T>, R> cons;
        public LListMatch() {
            set(() -> unfix.map(listF -> listF.match(nil, cons)));
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
