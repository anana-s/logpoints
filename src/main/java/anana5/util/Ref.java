package anana5.util;

import java.io.Serializable;

public interface Ref<T> extends Serializable {
    byte[] hash();
    T get();
}
