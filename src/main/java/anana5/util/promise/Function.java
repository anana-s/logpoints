package anana5.util.promise;

import anana5.util.Promise;

@FunctionalInterface
public interface Function<A, R> extends java.util.function.Function<A, Promise<R>> {}
