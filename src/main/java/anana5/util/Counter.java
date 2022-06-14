package anana5.util;

import java.util.Iterator;

public class Counter implements Iterable<Long>, Iterator<Long> {
    private long count = 0;

    @Override
    public Iterator<Long> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Long next() {
        return probe();
    }

    public long probe() {
        return count++;
    }
}
