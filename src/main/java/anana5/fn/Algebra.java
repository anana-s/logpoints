package anana5.fn;

@FunctionalInterface
public interface Algebra<F, A> extends Function<H<F, A>, A> {
    default A algebra(H<F, A> fa) {
        return apply(fa);
    }
}
