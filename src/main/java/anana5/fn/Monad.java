package anana5.fn;

public interface Monad<M extends Functor<M>> extends Applicative<M> {
    <A> H<M, A> unit(A a);

    <A, B> H<M, B> bind(Function<A, H<M, B>> f, H<M, A> ma);

    @Override
    default <A> H<M, A> pure(A a) {
        return unit(a);
    }

    @Override
    default <A, B> H<M, B> apply(H<M, Function<A, B>> mf, H<M, A> ma) {
        return bind(f -> map(f, ma), mf);
    }

    @Override
    default <A, B> H<M, B> map(Function<A, B> f, H<M, A> ma) {
        return bind(a -> unit(f.apply(a)), ma);
    }
}
