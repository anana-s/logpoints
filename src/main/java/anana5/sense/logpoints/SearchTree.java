package anana5.sense.logpoints;

import java.util.Collection;

class SearchTree {
    interface Evaluable<T extends Evaluable<T>> extends Comparable<T> {
        float evaluate();

        @Override
        default int compareTo(T o) {
            return Float.compare(this.evaluate(), o.evaluate());
        }
    }

    interface State extends Evaluable<State>, Cloneable {
        Collection<? extends Action> actions();
    }

    interface Action extends Evaluable<Action> {
        void apply();
    }

    class SearchTreeNode<S extends State, A extends Action> {

    }
}
