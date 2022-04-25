package anana5.util;

public interface Ref<T> {
    T get();
    default boolean check() {
        return get() != null;
    }
}
