package anana5.fn;

public interface Applicative<F extends Functor<F>> extends Functor<F> {
    <A> H<F, A> pure(A a);

    <A, B> H<F, B> apply(H<F, Function<A, B>> f, H<F, A> ma);

    @Override
    default <A, B> H<F, B> map(Function<A, B> f, H<F, A> fa) {
        return apply(pure(f), fa);
    }
}
