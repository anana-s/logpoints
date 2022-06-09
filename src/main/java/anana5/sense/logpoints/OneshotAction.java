package anana5.sense.logpoints;

import anana5.sense.logpoints.SearchTree.Action;
import anana5.util.Ref;

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
    public float evaluate() {
        return delegate.evaluate();
    }

    @Override
    public void apply() {
        if (!context.done()) {
            delegate.apply();
            context.set();
        }
    }

    static class Context {
        private boolean done;

        public Context() {
            this.done = false;
        }
        
        void set() {
            this.done = true;
        }

        boolean done() {
            return done;
        }

        public Action oneshot(Action action) {
            return new OneshotAction(action, this);
        }
    }
}
