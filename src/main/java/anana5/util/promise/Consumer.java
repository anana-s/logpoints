package anana5.util.promise;

import anana5.util.Promise;

@FunctionalInterface
public interface Consumer<A> extends java.util.function.Function<A, Promise<Void>> {}
