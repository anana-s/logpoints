package anana5.util;

import java.util.function.Supplier;

public abstract class Match<T> implements Supplier<T> {
    private Supplier<T> result;

    public void set(Supplier<T> result) {
        this.result = result;
    }

    @Override
    public T get() {
        return result.get();
    }
}
