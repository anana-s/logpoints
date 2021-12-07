package anana5.sense.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;


public class Branch<T> extends Jungle.NodeF<T, Cont<Collection<T>>> {
    public Branch(T s) {
        super(s, Cont.of(() -> Collections.emptySet()));
    }

    public Branch(T s, Supplier<Collection<T>> next) {
        super(s, Cont.of(next));
    }

    public Branch(T s, Cont<Collection<T>> next) {
        super(s, next);
    }

    public <Node extends Jungle.NodeF<T, Cont<Collection<T>>>> Branch(Node other) {
        super(other);
    }
}
