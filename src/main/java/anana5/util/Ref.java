package anana5.util;

public interface Ref<T> {
    byte[] hash();
    T get();
}
