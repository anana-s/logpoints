package anana5.sense.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;


public class Branch<S, T> extends Jungle.NodeF<S, Cont<Collection<T>>> {
    public Branch(S s) {
        super(s, Cont.of(() -> Collections.emptySet()));
    }

    public Branch(S s, Supplier<Collection<T>> next) {
        super(s, Cont.of(next));
    }
}
