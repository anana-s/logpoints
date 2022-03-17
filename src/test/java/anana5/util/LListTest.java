package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class LListTest {

    private final PList<Integer> a = PList.of(1, 2, 3);
    private final PList<Integer> b = PList.of(4, 5, 6);
    private final PList<PList<Integer>> c = PList.of(a, b);

    @Test
    void foldr() {
        var actual = PList.bind(a.foldr(PList.of(), (a, b) -> Promise.just(b.push(a)))).collect().join();
        assertEquals(Arrays.asList(3, 2, 1), actual);
    }

    @Test
    void foldl() {
        var actual = PList.bind(a.foldl(PList.of(), (a, b) -> Promise.just(b.push(a)))).collect().join();
        assertEquals(Arrays.asList(1, 2, 3), actual);
    }

    @Test
    void collect() {
        a.collect().bind(actual -> {
            assertEquals(Arrays.asList(1, 2, 3), actual);
            return Promise.<Void>lazy();
        }).join();
    }

    @Test
    void merge() {
        PList.merge(a, b).collect().then(r$ -> {
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), r$);
            return Promise.<Void>lazy();
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
        var out = a.filter(value -> Promise.just(value != 2));
        out.collect().bind(actual -> {
            assertEquals(Arrays.asList(1,3), actual);
            return Promise.lazy();
        }).join();
    }

    @Test
    void traverse() {
        List<Integer> actual = new ArrayList<>();
        a.traverse(value -> {
            actual.add(value);
            return Promise.lazy();
        }).join();
        assertEquals(Arrays.asList(1,2,3), actual);
    }

    @Test
    void bind() {
        List<Integer> actual = new ArrayList<>();
        PList.unfold(3, i -> {
            if (i == 0) {
                return Promise.just(ListF.nil());
            }
            actual.add(i);
            return Promise.just(ListF.cons(i, i - 1));
        }).traverse(i -> Promise.lazy()).join();

        assertEquals(Arrays.asList(3, 2, 1), actual);
    }
}