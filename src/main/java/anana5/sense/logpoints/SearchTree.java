package anana5.sense.logpoints;

import java.util.Collection;

public class SearchTree {
    interface Evaluable<T extends Evaluable<T>> extends Comparable<T> {
        float heuristic();

        T fork();

        @Override
        default int compareTo(T o) {
            return Float.compare(this.heuristic(), o.heuristic());
        }
    }

    class ActionContext {
        boolean applied = false;

        void set() {
            if (applied) {
                throw new RuntimeException();
            }
            applied = true;
        }
    }

    interface State extends Evaluable<State> {
        Collection<? extends Action> actions();

    }

    interface Action extends Evaluable<Action> {
        void apply();
    }

    class SearchTreeNode<S extends State, A extends Action> {

    }
}
