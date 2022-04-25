package anana5.util;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Maybe<T> extends Ref<T> {
    <R> MaybeMatch<T, R> match();
    <R> R match(Supplier<R> nil, Function<T, R> cons);
    boolean check();
    T get();
    <R> Maybe<R> fmap(Function<T, R> func);

    static <T> Nothing<T> nothing() {
        return new Nothing<>();
    }

    static class Nothing<T> implements Maybe<T> {
        @Override
        public boolean check() {
            return false;
        }
        @Override
        public T get() {
            return null;
        }
        @Override
        public <R> Maybe<R> fmap(Function<T, R> func) {
            return new Nothing<>();
        }

        @Override
        public <R> MaybeMatch<T, R> match() {
            return new MaybeMatch<>() {
                @Override
                public MaybeMatch<T, R> nothing(Supplier<R> func) {
                    set(func);
                    return this;
                }
            };
        }

        @Override
        public <R> R match(Supplier<R> nil, Function<T, R> cons) {
            return nil.get();
        }
    }

    static <T> Just<T> just(T t) {
        return new Just<>(t);
    }

    static class Just<T> implements Maybe<T> {
        final public T value;

        public Just(T value) {
            this.value = value;
        }

        @Override
        public boolean check() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public <R> Maybe<R> fmap(Function<T, R> func) {
            return new Just<>(func.apply(value));
        }

        @Override
        public <R> MaybeMatch<T, R> match() {
            return new MaybeMatch<>() {
                @Override
                public MaybeMatch<T, R> just(Function<T, R> func) {
                    set(() -> func.apply(value));
                    return this;
                }
            };
        }

        @Override
        public <R> R match(Supplier<R> nil, Function<T, R> cons) {
            return cons.apply(value);
        }
    }

    public class MaybeMatch<T, R> extends Match<R> {
        public MaybeMatch<T, R> nothing(Supplier<R> func) {
            return this;
        }
        public MaybeMatch<T, R> just(Function<T, R> func) {
            return this;
        }
    }
}
