package anana5.sense.logpoints;

import anana5.sense.logpoints.SearchTree.Action;

class OneshotAction implements Action {
    private final Context context;
    private final Action delegate;

    OneshotAction(Action action) {
        this.delegate = action;
        this.context = new Context();
    }

    OneshotAction(Action action, Context context) {
        this.delegate = action;
        this.context = context;
    }

    @Override
    public double evaluate() {
        return delegate.evaluate();
    }

    @Override
    public SearchTree.State apply() {
        if (!context.done()) {
            SearchTree.State state = delegate.apply();
            context.set(state);
            return state;
        }
        return context.state();
    }

    @Override
    public Action clone() {
        return new OneshotAction(delegate.clone(), context);
    }

    static class Context {
        private SearchTree.State state;

        public Context() {
            this.state = null;
        }
        
        void set(SearchTree.State state) {
            this.state = state;
        }

        boolean done() {
            return state != null;
        }

        SearchTree.State state() {
            return state;
        }

        public Action oneshot(Action action) {
            return new OneshotAction(action, this);
        }

        Action clone(Action action) {
            return new OneshotAction(action.clone(), this);
        }
    }
}
