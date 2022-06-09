package anana5.sense.logpoints;

import anana5.util.Ref;

final class Line implements Ref<String> {
    private final int index;
    private final String value;

    Line(int index, String value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("%d: %s", index, value);
    }
}