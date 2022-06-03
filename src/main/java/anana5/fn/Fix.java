package anana5.fn;

public abstract class Fix<F extends Functor<F>> implements Functor<Fix<F>> {
    protected final F f;

    public Fix(F f) {
        this.f = f;
    }

    public abstract <A> H<F, H<Fix<F>, A>> unfix(H<Fix<F>, A> fix);

    <A> A cata(Algebra<F, A> a, H<Fix<F>, A> fixa) {
        return a.apply(f.map(fixa1 -> cata(a, fixa1), unfix(fixa)));
    }

    @Override
    public <A, B> H<Fix<F>, B> map(Function<A, B> f, H<Fix<F>, A> fa) {
        // TODO Auto-generated method stub
        return null;
    }
}
