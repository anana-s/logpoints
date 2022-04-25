package anana5.sense.logpoints;

import java.util.Collection;

interface Head {
    Head previous();
    Collection<Head> next();
    boolean done();
}