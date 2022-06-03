package anana5.graph.rainfall;

import anana5.fn.Function;
import anana5.fn.Functor;
import anana5.fn.H;

public class DropFunctor<T> implements Functor<DropFunctor<T>> {

    private static final DropFunctor<Object> INSTANCE = new DropFunctor<>();

    @SuppressWarnings("unchecked")
    public static final <T> DropFunctor<T> instance() {
        return (DropFunctor<T>)INSTANCE;
    }

    @Override
    public <A, B> Drop<T, B> map(Function<A, B> f, H<DropFunctor<T>, A> fa) {
        var drop = (Drop<T, A>) fa;
        T t = drop.get();
        A a = drop.next();
        B b = f.apply(a);
        return new Drop<>(t, b);
    }
}
