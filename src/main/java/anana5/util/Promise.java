package anana5.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import anana5.util.function.Effect;

public class Promise<T> implements Computation<T> {
    boolean resolved;
    T value;

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

    public static <T> Promise<T> pure(Supplier<T> supplier) {
        return Promise.from(Computation.pure(supplier));
    }

    public static Promise<Void> effect(Effect effect) {
        return Promise.from(Computation.pure(() -> {
            effect.apply();
            return null;
        }));
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
            if (resolved) {
                return Continuation.apply(k, value);
            } else {
                return Continuation.accept(c, k.then(this::resolve));
            }
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
    public <R> Promise<R> map(Function<T, R> f) {
        return Promise.from(Computation.super.map(f));
    }
    @Override
    public <R> Promise<R> apply(Computation<Function<T, R>> f) {
        return Promise.from(Computation.super.apply(f));
    }
    @Override
    public <S> Promise<S> bind(Function<T, Computation<S>> f) {
        return then(f);
    }
    public <S> Promise<S> then(Function<T, Computation<S>> f) {
        return Promise.from(Computation.super.bind(f));
    }
    public static <T> Promise<T> nil() {
        return Promise.from(Computation.nil());
    }
    public boolean resolved() {
        return this.resolved;
    }
    @Override
    public T run() {
        Computation.super.run(this::resolve);
        assert resolved == true;
        return value;
    }
    @Override
    public void run(Consumer<T> f) {
        f.accept(this.run());
    }
}
