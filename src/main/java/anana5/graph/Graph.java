package anana5.graph;

import java.util.Collection;

public interface Graph<T> {
    Collection<? extends T> from(T source);
    Collection<? extends T> to(T target);
}
