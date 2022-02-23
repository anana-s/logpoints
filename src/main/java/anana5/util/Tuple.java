package anana5.util;

public class Tuple<FST, SND> {
    private final FST a;
    private final SND b;
    public Tuple(FST fst, SND snd) {
        a = fst;
        b = snd;
    }
    public FST fst() {
        return a;
    }
    public SND snd() {
        return b;
    }
}
