package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class LListTest {

    private final LList<Integer> a = new LList<>(1, 2, 3);
    private final LList<Integer> b = new LList<>(4, 5, 6);
    private final LList<LList<Integer>> c = new LList<>(a, b);

    @Test
    void foldr() {
        var actual = LList.bind(a.foldr(LList.nil(), (a, b) -> LList.cons(a, b))).collect().join();
        assertEquals(Arrays.asList(3, 2, 1), actual);
    }

    @Test
    void foldl() {
        var actual = LList.bind(a.foldl(LList.nil(), (a, b) -> LList.cons(a, b))).collect().join();
        assertEquals(Arrays.asList(1, 2, 3), actual);
    }

    @Test
    void collect() {
        a.collect().bind(actual -> {
            assertEquals(Arrays.asList(1, 2, 3), actual);
            return Promise.<Void>nil();
        }).join();
    }

    @Test
    void merge() {
        LList.merge(a, b).collect().then(r$ -> {
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), r$);
            return Promise.<Void>nil();
        }).join();
    }

    @Test
    void flatmap() {
        c.flatmap(Function.identity()).collect().then(actual -> {
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), actual);
            return Promise.just(true);
        }).join();
    }

    @Test
    void filter() {
        var out = a.filter((Function<Integer, Promise<Boolean>>)value -> Promise.just(value != 2));
        out.collect().bind(actual -> {
            assertEquals(Arrays.asList(1,3), actual);
            return Promise.nil();
        }).join();
    }
}