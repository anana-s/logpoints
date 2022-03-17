package anana5.graph.rainfall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
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
    void traverse() {
        var actual = new ArrayList<>();
        graph.get().traverse(v -> {
            actual.add(v);
            return Promise.lazy();
        }).join();
        assertEquals(Arrays.asList(1, 2, 4, 5, 3), actual);
    }

    @Test
    void unfold() {
        var actual = new ArrayList<>();
        Rain<Integer> copy = Rain.unfold(graph.get(), rain -> {
            return rain.unfix().map(drop -> Drop.of(drop.get() + 1, drop.next()));
        });

        copy.traverse(v -> {
            actual.add(v);
            return Promise.lazy();
        }).join();

        assertEquals(Arrays.asList(2, 3, 5, 6, 4), actual);
    }

    @Test
    void fold() {
        Integer actual = graph.get().<Promise<Integer>>fold(drops -> drops.foldr(0, (drop, acc) -> drop.next().map(n -> acc + n + drop.get()))).join();
        assertEquals(24, actual);
    }

    @Test
    void paramorph() {
        Integer actual = Rain.<PList<Integer>, Integer>unfold(a, a$ -> PList.bind(a$.match(() -> PList.of(), (x, xs) -> PList.of(new Drop<>(x, xs)))))
            .<Promise<Integer>>fold(droplets -> droplets.head().then(maybe -> maybe.match(() -> Promise.just(0), droplet -> droplet.next().map(x -> x + droplet.get())))).join();
        assertEquals(6, actual);
    }

    @Test
    void filter() {
        Rain<Integer> rain = Rain.unfold(0, i -> {
            return PList.of(new Drop<>(i, i + 1));
        });

        rain = rain.filter(i -> {
            return Promise.just(i < 3);
        });

        List<Integer> actual = new ArrayList<>();

        rain.traverse((v) -> {
            actual.add(v);
            return Promise.lazy();
        }).join();
        assertEquals(Arrays.asList(0, 1, 2), actual);
    }

    @Test
    void mergeEmtpy() {
        Rain<Integer> rain = Rain.merge(PList.of());
        Promise<Integer> pActual = rain.fold(droplets -> droplets.foldl(Promise.just(0), (droplet, pAcc) -> null).then(Function.identity()));
        assertEquals(0, pActual.join());
    }

    @Test
    void map() {
        var actual = graph.get().map(v -> v + 1).<Integer>fold(drops -> drops.foldl(0, (drop, acc) -> Promise.just(drop.next() + drop.get() + acc)).join());
        assertEquals(31, actual);
    }

    @Test
    void resolve() {
        var actual = new ArrayList<>();
        graph.get().resolve().then(rain -> rain.traverse(v -> {
            actual.add(v);
            return Promise.lazy();
        })).join();
        assertEquals(Arrays.asList(1, 2, 4, 5, 3), actual);
    }
}
