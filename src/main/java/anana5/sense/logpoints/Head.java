package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Head {
    /**
     *
     */
    private final Matcher matcher;
    private final SerializedVertex stmt;
    final Group group;

    Head(Matcher matcher, SerializedVertex stmt, Group group) {
        this.matcher = matcher;
        this.stmt = stmt;
        this.group = group;
    }

    public List<Head> accept(Line line) {
        List<Head> out = new ArrayList<>();
        this.matcher.match(stmt, line).map(match -> {
            for (SerializedVertex next : this.matcher.graph.from(match.serial())) {
                Group group = this.group.clone();
                group.add(match);
                out.add(new Head(this.matcher, next, group));
            }
            return null;
        });
        return Collections.unmodifiableList(out);
    }
}