package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

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
        State clone();
    }

    interface Action extends Evaluable<Action>, Cloneable {
        State apply();

        Action clone();
    }

    @FunctionalInterface
    interface Selector<T extends Evaluable<T>> extends Function<List<T>, T> {
    }
    

    private final SearchTreeNode root;
    private final Selector<Action> simulator;

    SearchTree(State state) {
        this.root = new SearchTreeNode(state, null);
        Random rng = new Random();
        this.simulator = actions -> actions.get(rng.nextInt(actions.size()));
    }

    void step() {
        Random rng = new Random();
        SearchTreeNode leaf = root.select(nodes -> nodes.stream().max((a, b) -> a.compareTo(b)).get());
        SearchTreeNode candidate = leaf.expand().get(rng.nextInt(leaf.expand().size()));
        float score = candidate.simulate(simulator, Integer.MAX_VALUE);
        candidate.backpropagate(score);
    }

    class SearchTreeNode implements Evaluable<SearchTreeNode> {
        private final State state;
        private final SearchTreeNode parent;
        private final int depth;
        private ArrayList<SearchTreeNode> children;
        private int discovered;
        private float total;

        SearchTreeNode(State state, SearchTreeNode parent) {
            this.state = state;
            this.parent = parent;
            if (parent == null) {
                this.depth = 0;
            } else {
                this.depth = parent.depth + 1;
            }
            this.children = new ArrayList<>();
            this.discovered = 0;
            this.total = 0;
        }

        @Override
        public float evaluate() {
            if (discovered <= 0) {
                return state.evaluate();
            }
            return total / discovered;
        }

        SearchTreeNode select(Selector<SearchTreeNode> selector) {
            SearchTreeNode selected = this;

            while (!selected.children.isEmpty()) {
                selected = selector.apply(selected.children);
            }

            return selected;
        }

        List<SearchTreeNode> expand() {
            children.clear();
            for (Action action : state.actions()) {
                SearchTreeNode child = new SearchTreeNode(action.clone().apply(), this);
                children.add(child);
            }
            return Collections.unmodifiableList(children);
        }

        float simulate(Selector<Action> selector, int depth) {
            State simulation = state.clone();
            //TODO: run simulation
            return simulation.evaluate();
        }

        void backpropagate(float score) {
            for (SearchTreeNode node = this; node != null; node = node.parent) {
                node.discovered++;
                node.total += score;
            }
        }
    }
}
