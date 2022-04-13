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
        var actual = a.foldr(Promise.just(new ArrayList<>()), (acc, i) -> {
            return acc.effect(a -> {
                a.add(i);
            });
        });
        assertEquals(Arrays.asList(1, 2, 3), actual.join());
    }

    @Test
    void foldl() {
        var actual = a.foldl(Promise.just(new ArrayList<>()), (i, acc) -> {
            return acc.effect(a -> {
                a.add(i);
            });
        });
        assertEquals(Arrays.asList(3, 2, 1), actual.join());
    }

    @Test
    void collect() {
        assertEquals(Arrays.asList(1, 2, 3), a.collect(Collectors.toList()).join());
    }

    @Test
    void merge() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), PList.concat(a, b).collect(Collectors.toList()).join());
    }

    @Test
    void flatmap() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), c.flatmap(Function.identity()).collect(Collectors.toList()).join());
    }

    @Test
    void filter() {
        assertEquals(Arrays.asList(1,3), a.filter(value -> Promise.just(value != 2)).collect(Collectors.toList()).join());
    }

    @Test
    void traverse() {
        List<Integer> actual = new ArrayList<>();
        a.traverse(value -> {
            actual.add(value);
            return Promise.just(value < 2);
        }).join();
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
    void resolve() {
        List<Integer> actual = new ArrayList<>();
        a.map(i -> {
            actual.add(i);
            return i;
        }).resolve().join();

        assertEquals(Arrays.asList(1, 2, 3), actual);
    }

    @Test
    void contains() {
        assertTrue(a.contains(3).join());
    }

    @Test
    void any() {
        assertTrue(a.any(i -> Promise.just(i == 2)).join());
    }
}