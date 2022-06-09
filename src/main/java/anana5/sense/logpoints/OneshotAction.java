package anana5.sense.logpoints;

import anana5.sense.logpoints.SearchTree.Action;

class OneshotAction implements Action {
    private final Action delegate;
    private boolean done;

    OneshotAction(Action action) {
        this.delegate = action;
        this.done = false;
    }

    @Override
    public float evaluate() {
        return delegate.evaluate();
    }

    @Override
    public void apply() {
        if (!done) {
            delegate.apply();
            done = true;
        }
    }
}
