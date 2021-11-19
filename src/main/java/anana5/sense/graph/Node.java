package anana5.sense.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;

public class Node<T> extends HashSet<T> {
    public Node() {
        super();
    }

    public Node(T tie) {
        super(Collections.singleton(tie));
    }
    
    public Node(Collection<T> ties) {
        super(ties);
    }

    public <R> Node<R> map(Function<T, R> f) {
        Node<R> node = new Node<>();
        for (T t : this) {
            node.add(f.apply(t));
        }
        return node;
    }

    public <R> Node<R> flatmap(Function<T, Node<R>> f) {
        Node<R> node = new Node<>();
        for (T t : this) {
            node.addAll(f.apply(t));
        }
        return node;
    }
}
