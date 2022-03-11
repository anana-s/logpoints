package anana5.util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ListF<A, F> {

    <B> ListF<B, F> map(Function<A, B> func);
    <B> ListF<B, F> map(BiFunction<A, F, B> func);
    <G> ListF<A, G> fmap(Function<F, G> func);
    <G> ListF<A, G> fmap(BiFunction<A, F, G> func);
    <B, G> ListF<B, G> bind(BiFunction<A, F, ListF<B, G>> func);
    <R> R match(Supplier<R> supplier, BiFunction<A ,F, R> func);
    <R> ListFMatch<A, F, R> match();

    public static <A, F> ListF<A, F> nil() {
        return new Nil<>();
    }
    
    public static <A, F> ListF<A, F> cons(A a, F f) {
        return new Cons<>(a, f);
    }

    public class Nil<A, F> implements ListF<A, F> {
        @Override
        public <B> ListF<B, F> map(BiFunction<A, F, B> func) {
            return new Nil<>();
        }

        @Override
        public <B> ListF<B, F> map(Function<A, B> func) {
            return new Nil<>();
        }

        @Override
        public <G> ListF<A, G> fmap(BiFunction<A, F, G> func) {
            return new Nil<>();
        }

        @Override
        public <G> ListF<A, G> fmap(Function<F, G> func) {
            return new Nil<>();
        }

        @Override
        public <B, G> ListF<B, G> bind(BiFunction<A, F, ListF<B, G>> func) {
            return new Nil<>();
        }

        @Override
        public <R> R match(Supplier<R> supplier, BiFunction<A, F, R> func) {
            return supplier.get();
        }

        @Override
        public <R> ListFMatch<A, F, R> match() {
            return new ListFMatch<A, F, R>() {
                @Override
                public ListFMatch<A, F, R> nil(Supplier<R> supplier) {
                    set(supplier);
                    return this;
                }
            };
        }
    }

    public class Cons<A, F> implements ListF<A, F> {
        final public A a;
        final public F f;
    
        public Cons(A a, F f) {
            this.a = a;
            this.f = f;
        }
    
        @Override
        public <B> ListF<B, F> map(Function<A, B> func) {
            return new Cons<>(func.apply(a), f);
        }
    
        @Override
        public <B> ListF<B, F> map(BiFunction<A, F, B> func) {
            return new Cons<>(func.apply(a, f), f);
        }
    
        @Override
        public <G> ListF<A, G> fmap(Function<F, G> func) {
            return new Cons<>(a, func.apply(f));
        }
    
        @Override
        public <G> ListF<A, G> fmap(BiFunction<A, F, G> func) {
            return new Cons<>(a, func.apply(a, f));
        }
    
        @Override
        public <B, G> ListF<B, G> bind(BiFunction<A, F, ListF<B, G>> func) {
            return func.apply(a, f);
        }

        @Override
        public <R> R match(Supplier<R> supplier, BiFunction<A, F, R> func) {
            return func.apply(a, f);
        }

        @Override
        public <R> ListFMatch<A, F, R> match() {
            return new ListFMatch<A, F, R>() {
                @Override
                public ListFMatch<A, F, R> cons(BiFunction<A, F, R> func) {
                    set(() -> func.apply(a, f));
                    return this;
                }
            };
        }
    }

    public class ListFMatch<T, F, R> extends Match<R> {
        public ListFMatch<T, F, R> nil(Supplier<R> supplier) {
            return this;
        }
        public ListFMatch<T, F, R> cons(BiFunction<T, F, R> func) {
            return this;
        }
    }
}
