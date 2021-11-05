package anana5.sense.graph.java;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public class DFS<T> {
    Stack<T> stack = new Stack<>();
    Set<T> visited = new HashSet<>();

    Visitor<T> visit;

    @FunctionalInterface
    public interface Visitor<T> {
        public void visit(T t, Consumer<T> push);
    }

    public DFS(Visitor<T> visit) {
        this.visit = visit;
    }

    public void on(Iterable<T> ts) {
        for (T t : ts) {
            stack.add(t);
        }
        while(!stack.empty()) {
            T t = stack.pop();
            if (visited.contains(t)) {
                continue;
            }
            visited.add(t);

            visit.visit(t, s -> stack.add(s));
        }
    }
}
