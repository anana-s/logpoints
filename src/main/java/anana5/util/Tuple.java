package anana5.util;

import java.io.Serializable;
import java.util.Objects;

public class Tuple<FST, SND> implements Serializable {
    private final FST a;
    private final SND b;
    public Tuple(FST fst, SND snd) {
        a = fst;
        b = snd;
    }
    public static <FST, SND> Tuple<FST, SND> of(FST fst, SND snd) {
        return new Tuple<FST, SND>(fst, snd);
    }
    public FST fst() {
        return a;
    }
    public SND snd() {
        return b;
    }
    @Override
    public boolean equals(Object obj) {
        return obj == this || obj.getClass() == Tuple.class && ((Tuple<?, ?>) obj).a.equals(a) && ((Tuple<?, ?>) obj).b.equals(b);
    }
    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    public static <A, B> Tuple<PList<A>, PList<B>> unzip(PList<Tuple<A, B>> tuples) {
        return Tuple.of(tuples.map(Tuple::fst), tuples.map(Tuple::snd));
    }
}
