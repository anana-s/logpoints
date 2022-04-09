package anana5.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public interface Graph<T, V extends Vertex<T, V>, E extends Edge<T, ? extends V>> {
    default void traverse(Consumer<? super E> consumer) {
        Set<E> seen = new HashSet<>();
        for (V root : roots()) {
            traverse(root, consumer, seen);
        }
    }
    default void traverse(V root, Consumer<? super E> consumer) {
        traverse(root, consumer, new HashSet<>());
    }
    private void traverse(V root, Consumer<? super E> consumer, Set<E> seen) {
        Stack<E> stack = new Stack<>();
        for (E edge : from(root)) {
            stack.push(edge);
        }
        while (!stack.isEmpty()) {
            E edge = stack.pop();
            consumer.accept(edge);
            seen.add(edge);
            for (E next : from(edge.target())) {
                if (seen.contains(next)) {
                    continue;
                }
                stack.push(next);
            }
        }
    }
    Collection<? extends V> roots();
    Collection<? extends V> vertices();
    Collection<? extends E> edges();
    Collection<? extends E> from(V source);
    Collection<? extends E> to(V target);
}
