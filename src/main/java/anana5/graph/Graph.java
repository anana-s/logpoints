package anana5.graph;

import java.util.Collection;

public interface Graph<T> {
    Collection<T> from(T source);
    Collection<T> to(T target);
}
