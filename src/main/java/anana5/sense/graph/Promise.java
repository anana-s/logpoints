package anana5.sense.graph;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Promise<T> implements Computation<T> {
    Computation<T> c;
    T cache;
    boolean resolved;
    public Promise(Supplier<T> pure) {
       this(Computation.of(pure));
    }
    public Promise(Promise<T> other) {
        this.c = other.c;
        this.cache = other.cache;
    }
    public Promise(Computation<T> continuation) {
        this.c = continuation;
    }
    @Override
    public Continuation accept(Callback<T> k) {
        if (resolved) {
            return Continuation.apply(k, cache);
        } else {
            return Continuation.accept(c, k.then(this::resolve));
        }
    }
    @Override
    public <R> Promise<R> map(Function<T, R> f) {
        return new Promise<>(Computation.super.map(f));
    }
    @Override
    public <R> Promise<R> apply(Computation<Function<T, R>> f) {
        return new Promise<>(Computation.super.apply(f));
    }
    @Override
    public <S> Promise<S> bind(Function<T, Computation<S>> f) {
        return new Promise<>(Computation.super.bind(f));
    }
    @Override
    public Promise<T> then(Consumer<T> f) {
        return new Promise<>(Computation.super.then(f));
    }
    public static <T> Promise<T> nil() {
        return new Promise<>(Computation.nil());
    }
    public void resolve(T t) {
        if (!resolved) {
            cache = t;
            resolved = true;
        }
    }
    public boolean resolved() {
        return this.resolved;
    }
    @Override
    public T run() {
        Computation.super.run(this::resolve);
        assert cache != null;
        return cache;
    }
    @Override
    public void run(Consumer<T> f) {
        f.accept(this.run());
    }
    @Override
    public int hashCode() {
        return Objects.hash(c);
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Promise)) {
            return false;
        }
        Promise<?> other = (Promise<?>)obj;
        return c.equals(other.c);
    }
}
