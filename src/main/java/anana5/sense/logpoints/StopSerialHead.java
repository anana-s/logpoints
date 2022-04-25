package anana5.sense.logpoints;

import java.util.Collection;
import java.util.Collections;

import anana5.util.PList;

class StopSerialHead extends BaseHead {

    public StopSerialHead(Head previous, PList<String> lines) {
        super(previous, lines);
    }

    @Override
    public Collection<Head> next() {
        return Collections.emptyList();
    }

    @Override
    public boolean done() {
        return super.done();
        // return true;
    }
}