package anana5.sense.logpoints;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface Request<T, R extends Serializable> extends Serializable, Function<T, R> {
    @Override
    default R apply(T t) {
        return accept(t);
    }
    R accept(T t);
}
