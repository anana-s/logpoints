package anana5.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Promise<T> extends Computation<T>, Ref<T> {

    public enum State {
        PENDING, RESOLVING, RESOLVED, REJECTED
    }

    public State state();
    public T resolve(T t);

    public static class Unresolved extends RuntimeException {
        Promise<?> promise;
        public Unresolved(Promise<?> promise) {
            this.promise = promise;
        }
        Promise<?> promise() {
            return promise;
        }
    }

    public static <T> Promise<T> of(Consumer<Consumer<T>> function) {
        return new CachingPromise<>(function);
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

    public static <T> Promise<T> from(Computation<T> computation) {
        return new ComputationAdapter<>(computation);
    }


    public static class ResolvedPromise<T> implements Promise<T> {
        private T value;

        private ResolvedPromise(T t) {
            value = t;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public State state() {
            return State.RESOLVED;
        }

        @Override
        public T resolve(T t) {
            return t;
        }
    }

    public static class CachingPromise<T> implements Promise<T> {
        private State state;
        private T value;

        private CachingPromise() {
            this.state = State.PENDING;
        }

        private CachingPromise(Consumer<Consumer<T>> function) {
            this();
            function.accept(this::resolve);
        }

        @Override
        public T resolve(T t) {
            if (!resolved()) {
                value = t;
                state = State.RESOLVED;
            }
            // otherwise do nothing;
            return value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public State state() {
            return state;
        }
    }

    public static class ComputationAdapter<T> extends CachingPromise<T> {
        Computation<T> c;
        ComputationAdapter(Computation<T> c) {
            this.c = c;
        }

        @Override
        public Continuation accept(Callback<T> k) {
            switch(super.state) {
                case RESOLVED:
                    return Continuation.apply(k, super.value);
                case RESOLVING:
                    throw new Unresolved(this);
                case PENDING:
                    // continue computation
                    super.state = State.RESOLVING;
                    return Continuation.accept(c, k.map(this::resolve));
                default:
                    throw new RuntimeException(String.format("unexpected state [%s]", super.state));
            }
        }

        @Override
        public T resolve(T t) {
            try {
                return super.resolve(t);
            } finally {
                c = null; // release computation
            }
        }
    }

    @Override
    public default Continuation accept(Callback<T> k) {
        if (resolved()) {
            return Continuation.apply(k, get());
        } else {
            throw new Unresolved(this);
        }
    }

    @Override
    public default Promise<T> effect(Consumer<? super T> consumer) {
        return Promise.from(Computation.super.effect(consumer));
    }

    @Override
    public default <R> Promise<R> map(Function<? super T, ? extends R> f) {
        return Promise.from(Computation.super.map(f));
    }

    @Override
    public default <R> Promise<R> apply(Computation<? extends Function<? super T, ? extends R>> f) {
        return Promise.from(Computation.super.apply(f));
    }

    @Override
    public default <S> Promise<S> bind(Function<? super T, ? extends Computation<S>> f) {
        return then(f);
    }

    public default <S> Promise<S> then(Function<? super T, ? extends Computation<S>> f) {
        return Promise.from(Computation.super.bind(f));
    }

    public default boolean resolved() {
        return state().equals(State.RESOLVED);
    }

    public default boolean resolving() {
        return state().equals(State.RESOLVING);
    }

    public default boolean pending() {
        return state().equals(State.PENDING);
    }

    public default boolean rejected() {
        return state().equals(State.REJECTED);
    }

    public default boolean done() {
        return resolved() || rejected();
    }

    public default Continuation continuation() {
        return this.accept(Callback.pure(this::resolve));
    }

    public default T join() {
        synchronized (this) {
            Computation.super.run(this::resolve);
            return get();
        }
    }
}
