package anana5.util;

import java.util.Objects;

public class Tuple<FST, SND> {
    private final FST a;
    private final SND b;
    private Tuple(FST fst, SND snd) {
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
        return obj == this || obj instanceof Tuple<?, ?> && ((Tuple<?, ?>) obj).a.equals(a) && ((Tuple<?, ?>) obj).b.equals(b);
    }
    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
