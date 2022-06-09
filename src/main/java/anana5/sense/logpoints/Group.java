package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.List;

class Group {
    final List<Match> matches;

    Group(List<Match> matches) {
        this.matches = new ArrayList<>(matches);
    }

    void add(Match match) {
        this.matches.add(match);
    }

    @Override
    public String toString() {
        return matches.toString();
    }

    @Override
    protected Group clone() {
        return new Group(matches);
    }
}