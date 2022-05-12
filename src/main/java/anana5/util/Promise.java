package anana5.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Promise<T> implements Computation<T> {

    private enum State {
        SCHEDULED,
        PENDING,
        RESOLVED
    }

    private State state;
    private T value;

    public static class Unresolved extends RuntimeException {
        Promise<?> promise;
        public Unresolved(Promise<?> promise) {
            this.promise = promise;
        }
        Promise<?> promise() {
            return promise;
        }
    }

    private Promise() {
        this.state = State.SCHEDULED;
    }

    private Promise(Consumer<Consumer<T>> function) {
        this();
        this.state = State.PENDING;
        function.accept(this::resolve);
    }

    public void resolve(T t) {
        if (!resolved()) {
            value = t;
            state = State.RESOLVED;
        }
    }

    public static <T> Promise<T> of(Consumer<Consumer<T>> function) {
        return new Promise<>(function);
    }

    public static <T> Promise<T> just(T t) {
        return Promise.of(resolve -> resolve.accept(t));
    }

    public static <T> Promise<T> nil() {
        return Promise.of(resolve -> resolve.accept(null));
    }

    public static <T> Promise<T> lazy() {
        return Promise.from(Computation.nil());
    }

    public static <T> Promise<T> lazy(Supplier<T> supplier) {
        return Promise.from(Computation.just(supplier));
    }

    public static Promise<Void> lazy(Runnable runnable) {
        return Promise.from(Computation.just(runnable));
    }

    public static <T> Promise<T> from(Computation<T> computation) {
        return new ComputationAdapter<>(computation);
    }

    public static class ComputationAdapter<T> extends Promise<T> {
        Computation<T> c;
        ComputationAdapter(Computation<T> c) {
            this.c = c;
        }

        @Override
        public Continuation accept(Callback<T> k) {
            switch(super.state) {
                case RESOLVED:
                    return Continuation.apply(k, super.value);
                case PENDING:
                    throw new Unresolved(this);
                case SCHEDULED:
                    // continue computation
                    super.state = State.PENDING;
                    return Continuation.accept(c, k.map(s -> { resolve(s); return s;}));
                default:
                    throw new RuntimeException(String.format("unexpected state [%s]", super.state));
            }
            // try {
            //     return super.accept(k);
            // } catch (ExecutionException e) {
            //     if (e.getCause() instanceof Unresolved) {
            //         if (pending()) {
            //             throw e;
            //         }
            //         super.state = State.PENDING;
            //         return Continuation.accept(c, k.map(s -> {
            //             resolve(s);
            //             return s;
            //         }));
            //     }
            //     throw e;
            // }
        }

        @Override
        public void resolve(T t) {
            super.resolve(t);
            c = null; // release computation
        }
    }

    @Override
    public Continuation accept(Callback<T> k) {
        if (resolved()) {
            return Continuation.apply(k, value);
        } else {
            throw new Unresolved(this);
        }
    }

    @Override
    public Promise<T> effect(Consumer<? super T> consumer) {
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

    public boolean resolved() {
        return state.equals(State.RESOLVED);
    }

    public boolean pending() {
        return state.equals(State.PENDING);
    }

    public boolean done() {
        return resolved();
    }

    public State state() {
        return state;
    }

    public boolean scheduled() {
        return state.equals(State.SCHEDULED);
    }

    public Continuation continuation() {
        return this.accept(Callback.pure(this::resolve));
    }

    public synchronized T join() {
        Computation.super.run(this::resolve);
        return value;
    }
}
