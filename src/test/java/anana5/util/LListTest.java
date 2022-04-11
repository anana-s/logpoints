package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LListTest {

    private final PList<Integer> a = PList.of(1, 2, 3);
    private final PList<Integer> b = PList.of(4, 5, 6);
    private final PList<PList<Integer>> c = PList.of(a, b);

    @Test
    void foldr() {
        var actual = a.foldr(new ArrayList<>(), (acc, i) -> {
            acc.add(i);
            return acc;
        });
        assertEquals(Arrays.asList(1, 2, 3), actual);
    }

    @Test
    void foldl() {
        var actual = a.foldr(new ArrayList<>(), (acc, i) -> {
            acc.add(i);
            return acc;
        });
        assertEquals(Arrays.asList(3, 2, 1), actual);
    }

    @Test
    void collect() {
        assertEquals(Arrays.asList(1, 2, 3), a.collect(Collectors.toList()));
    }

    @Test
    void merge() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), PList.merge(a, b).collect(Collectors.toList()));
    }

    @Test
    void flatmap() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), c.flatmap(Function.identity()).collect(Collectors.toList()));
    }

    @Test
    void filter() {
        assertEquals(Arrays.asList(1,3), a.filter(value -> value != 2).collect(Collectors.toList()));
    }

    @Test
    void traverse() {
        List<Integer> actual = new ArrayList<>();
        a.traverse(value -> {
            actual.add(value);
            return value < 2;
        });
        assertEquals(Arrays.asList(1,2), actual);
    }

    // @Test
    // void bind() {
    //     List<Integer> actual = new ArrayList<>();
    //     PList.unfold(3, i -> {
    //         if (i == 0) {
    //             return Promise.just(ListF.nil());
    //         }
    //         actual.add(i);
    //         return Promise.just(ListF.cons(i, i - 1));
    //     }).traverse(i -> Promise.lazy()).join();

    //     assertEquals(Arrays.asList(3, 2, 1), actual);
    // }

    @Test
    void contains() {
        assertTrue(a.contains(3));
    }
}