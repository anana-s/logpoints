package anana5.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Promise<T> implements Computation<T> {
    private boolean resolved;
    private T value;

    static class Unresolved extends RuntimeException {
        Promise<?> promise;
        public Unresolved(Promise<?> promise) {
            this.promise = promise;
        }
    }

    public Promise() {
        this.resolved = false;
    }

    public Promise(Consumer<Consumer<T>> promise) {
        this();
        promise.accept(this::resolve);
    }

    public void resolve() {
        resolved = true;
    }

    public void resolve(T t) {
        if (!resolved) {
            value = t;
            resolved = true;
        }
    }

    public static <T> Promise<T> just(T t) {
        return Promise.from(Computation.just(t));
    }

    public static <T> Promise<T> just(Supplier<T> supplier) {
        return Promise.from(Computation.just(supplier));
    }

    public static Promise<Void> just(Runnable runnable) {
        return Promise.from(Computation.just(runnable));
    }

    public static <T> Promise<T> from(Computation<T> computation) {
        return new ComputationAdapter<>(computation);
    }

    static class ComputationAdapter<T> extends Promise<T> {
        Computation<T> c;
        ComputationAdapter(Computation<T> c) {
            this.c = c;
        }
        @Override
        public Continuation accept(Callback<T> k) throws Unresolved {
            if (resolved()) {
                return Continuation.apply(k, this.get());
            } else {
                return Continuation.accept(c, k.effect(this::resolve));
            }
        }
        @Override
        public void resolve(T t) {
            super.resolve(t);
            c = null;
        }
    }

    @Override
    public Continuation accept(Callback<T> k) throws Unresolved {
        if (resolved) {
            return Continuation.apply(k, value);
        } else {
            throw new Unresolved(this);
        }
    }

    @Override
    public Promise<T> effect(Consumer<T> consumer) {
        return Promise.from(Computation.super.effect(consumer));
    }

    @Override
    public <R> Promise<R> map(Function<? super T, ? extends R> f) {
        return Promise.from(Computation.super.map(f));
    }
    
    @Override
    public <R> Promise<R> apply(Computation<? extends Function<? super T, ? extends R>> f) {
        return Promise.from(Computation.super.apply(f));
    }
    
    @Override
    public <S> Promise<S> bind(Function<? super T, ? extends Computation<S>> f) {
        return then(f);
    }
    
    public <S> Promise<S> then(Function<? super T, ? extends Computation<S>> f) {
        return Promise.from(Computation.super.bind(f));
    }
    
    public static <T> Promise<T> nil() {
        return Promise.from(Computation.nil());
    }
    
    public boolean resolved() {
        return this.resolved;
    }
    
    public T join() {
        Computation.super.run(this::resolve);
        return value;
    }
    
    public T get() throws Unresolved {
        if (!resolved) {
            throw new Unresolved(this);
        };
        return value;
    }

    public static <T> Promise<LList<T>> all(LList<Promise<T>> promises) {
        return promises.fold(p -> p.then(listF -> listF.match(() -> Promise.nil(), (pT, pAcc) -> pT.then(t -> pAcc.then(acc -> Promise.just(LList.cons(t, acc)))))));
    }
}
