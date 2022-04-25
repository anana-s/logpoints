package anana5.graph.rainfall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import anana5.util.PList;
import anana5.util.Promise;

public class RainTest {

    private final PList<Integer> a = PList.of(1, 2, 3);

    private final Supplier<Rain<Integer>> graph = () -> Rain.of(
        Drop.of(1, Rain.of(
            Drop.of(2, Rain.of(
                Drop.of(4, Rain.of(
                    Drop.of(5, Rain.of())
                ))
            )),
            Drop.of(3, Rain.of(
                Drop.of(4, Rain.of(
                    Drop.of(5, Rain.of())
                ))
            ))
        ))
    );

    @Test
    void mergeEmtpy() {
        Rain<Integer> rain = Rain.merge(PList.of());
        Promise<Integer> pActual = rain.<Promise<Integer>>fold(drops -> drops.foldr(Promise.just(0), (acc, drop) -> acc.then(a -> drop.next().map(n -> n + drop.get() + a))));
        assertEquals(0, pActual.join());
    }

    @Test
    void map() {
        var asdf = new ArrayList<>();
        var actual = graph.get().map(v -> {
            asdf.add(v);
            return v + 1;
        }).<Promise<Integer>>fold(drops -> drops.foldr(Promise.just(0), (acc, drop) -> acc.then(a -> drop.next().map(n -> n + drop.get() + a))));
        assertEquals(Arrays.asList(), asdf);
        assertEquals(31, actual.join());
        assertEquals(Arrays.asList(1,2,3,4,5,4,5), asdf);
    }
}
