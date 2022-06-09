package anana5.sense.logpoints;

final class Match {
    private final SerializedVertex ref;
    private final Line line;

    Match(SerializedVertex ref, Line line) {
        this.ref = ref;
        this.line = line;
    }

    public SerializedVertex serial() {
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