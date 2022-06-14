package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static java.lang.Math.*;

class SearchTree {
    interface Evaluable<T extends Evaluable<T>> extends Comparable<T> {
        double evaluate();

        @Override
        default int compareTo(T o) {
            return Double.compare(this.evaluate(), o.evaluate());
        }
    }

    interface State extends Evaluable<State>, Cloneable {
        List<? extends Action> actions();
        State clone();
    }

    interface Action extends Evaluable<Action>, Cloneable {
        State apply();

        Action clone();
    }

    @FunctionalInterface
    interface Selector<T extends Evaluable<T>> extends Function<List<? extends T>, T> {
    }
    

    private final SearchTreeNode root;
    private final Random random;

    private final double c;
    private final int depth;

    SearchTree(State state, int depth, double c) {
        this.root = new SearchTreeNode(state, null);
        this.random = new Random();
        
        this.depth = depth;
        this.c = c;
    }

    void step() {
        SearchTreeNode leaf = root.select();
        List<SearchTreeNode> candidates = leaf.expand();
        SearchTreeNode candidate = candidates.get(random.nextInt(candidates.size()));
        double score = candidate.simulate();
        candidate.backpropagate(score);
    }

    class SearchTreeNode implements Evaluable<SearchTreeNode> {
        private final State state;
        private final SearchTreeNode parent;
        private ArrayList<SearchTreeNode> children;
        private int n;
        private double w;

        SearchTreeNode(State state, SearchTreeNode parent) {
            this.state = state;
            this.parent = parent;

            this.children = null;

            this.n = 0;
            this.w = 0;
        }

        @Override
        public double evaluate() {
            if (n <= 0) {
                return Double.MAX_VALUE;
            }

            // UCT recommended selection formula; see https://link.springer.com/chapter/10.1007/11871842_29.
            return w / n + c * sqrt(log(parent.n) / n);
        }

        SearchTreeNode select() {
            SearchTreeNode selected = this;

            while (selected.isExpanded() && !selected.isTerminal()) {
                selected = Collections.max(selected.children());
            }

            return selected;
        }

        List<SearchTreeNode> children() {
            if (!isExpanded()) {
                throw new IllegalStateException("node is not yet expanded");
            }

            return Collections.unmodifiableList(children);
        }

        List<SearchTreeNode> expand() {
            if (isExpanded()) {
                return Collections.unmodifiableList(children);
            }

            children = new ArrayList<>();
            for (Action action : state.actions()) {
                SearchTreeNode child = new SearchTreeNode(action.clone().apply(), this);
                children.add(child);
            }

            return Collections.unmodifiableList(children);
        }

        double simulate() {
            State simulation = state.clone();
            Random rng = new Random();

            for (int d = 0; d < depth; d++) {
                List<? extends Action> actions = simulation.actions();
                Action action = actions.get(rng.nextInt(actions.size()));
                simulation = action.apply();
            }

            return simulation.evaluate();
        }

        void backpropagate(double score) {
            for (SearchTreeNode node = this; node != null; node = node.parent) {
                node.n++;
                node.w += score;
            }
        }

        boolean isExpanded() {
            return children != null;
        }

        boolean isTerminal() {
            if (!isExpanded()) {
                throw new IllegalStateException("node is not yet expanded");
            }
            return children.isEmpty();
        }
    }
}
