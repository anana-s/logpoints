package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import anana5.util.PList;

class State implements SearchTree.State {
    private PList<Line> lines;
    private final List<Head> heads;
    private final List<Line> skips;

    private State(Collection<Head> heads, List<Line> skips, PList<Line> lines) {
        this.heads = new ArrayList<>(heads);
        this.skips = new ArrayList<>(skips);
        this.lines = lines;
    }

    class SkipAction implements SearchTree.Action {
        @Override
        public void apply() {
            skips.add(advance());
        }

        @Override
        public float evaluate() {
            return -1;
        }
    }

    class NextStateAction implements SearchTree.Action {
        private final int idx;

        NextStateAction(int idx) {
            this.idx = idx;
        }

        @Override
        public void apply() {
            Head head = head(idx);
            head.accept(advance());
        }

        @Override
        public float evaluate() {
            //TODO: implement
            return 0;
        }
    }

    @Override
    public float evaluate() {
        return 0;
    }

    @Override
    public Collection<SearchTree.Action> actions() {
        Collection<SearchTree.Action> actions = new ArrayList<>();
        OneshotAction.Context context = new OneshotAction.Context();
        actions.add(context.oneshot(new SkipAction()));

        for (int i = 0; i < heads.size(); i++) {
            actions.add(context.oneshot(new NextStateAction(i)));
        }

        return actions;
    }

    Line line() {
        return lines.head().join();
    }

    Line advance() {
        var line = lines.head().join();
        lines = lines.tail();
        return line;
    }

    public Collection<Group> groups() {
        return heads.stream().map(head -> head.group).distinct().collect(Collectors.toList());
    }

    public Head head(int i) {
        return heads.get(i);
    }

    @Override
    public State clone() {
        return new State(heads, skips, lines);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        State other = (State) obj;
        return Objects.equals(heads, other.heads);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heads);
    }

}