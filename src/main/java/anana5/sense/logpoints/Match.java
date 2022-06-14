package anana5.sense.logpoints;

final class Match {
    private final GrapherVertex ref;
    private final Line line;

    Match(GrapherVertex ref, Line line) {
        this.ref = ref;
        this.line = line;
    }

    public GrapherVertex vertex() {
        return ref;
    }

    public Line line() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%d ~ %s", ref.id(), line.toString());
    }
}