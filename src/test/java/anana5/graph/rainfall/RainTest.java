package anana5.graph.rainfall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import anana5.util.PList;

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
        Integer pActual = rain.<Integer>fold(drops -> drops.foldr(0, (acc, drop) -> drop.next() + drop.get() + acc));
        assertEquals(0, pActual);
    }

    @Test
    void map() {
        var actual = graph.get().map(v -> v + 1).<Integer>fold(drops -> drops.foldr(0, (acc, drop) -> drop.next() + drop.get() + acc));
        assertEquals(31, actual);
    }
}
