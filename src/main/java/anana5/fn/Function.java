package anana5.fn;

@FunctionalInterface
public interface Function<A, B> {
    B apply(A a);
}
