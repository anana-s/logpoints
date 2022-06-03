package anana5.fn;

public interface Functor<F extends Functor<F>> {
    <A, B> H<F, B> map(Function<A, B> f, H<F, A> fa);
}
