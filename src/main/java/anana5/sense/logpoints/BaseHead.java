package anana5.sense.logpoints;

import anana5.util.PList;

public abstract class BaseHead implements Head {
    private final Head prev;
    private final PList<String> lines;

    public BaseHead(Head prev, PList<String> lines) {
        this.prev = prev;
        this.lines = lines;
    }

    @Override
    public boolean done() {
        return lines.empty().join();
    }

    @Override
    public Head previous() {
        return prev;
    }

    protected PList<String> lines() {
        return lines;
    }
    
}
