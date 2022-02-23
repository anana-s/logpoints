package anana5.util.promise;

import anana5.util.Promise;

@FunctionalInterface
public interface BiFunction<A, B, R> extends java.util.function.BiFunction<A, B, Promise<R>> {}
