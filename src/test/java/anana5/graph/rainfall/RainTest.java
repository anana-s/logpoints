package anana5.graph.rainfall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;

public class RainTest {
    private static class V implements Vertex<Integer> {
        private final int value;
        private V(int value) {
            this.value = value;
        }
        @Override
        public Integer value() {
            return value;
        }
        @Override
        public int id() {
            return value;
        }
        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof V && ((V) obj).id() == this.id();
        }
        @Override
        public int hashCode() {
            return id();
        }
        public static V of(int value) {
            return new V(value);
        }
    }

    private final LList<Integer> a = LList.of(1, 2, 3);

    private final Supplier<Rain<Integer>> graph = () -> Rain.of(
        Drop.of(V.of(1), Rain.of(
            Drop.of(V.of(2), Rain.of(
                Drop.of(V.of(4), Rain.of(
                    Drop.of(V.of(5), Rain.of())
                ))
            )),
            Drop.of(V.of(3), Rain.of(
                Drop.of(V.of(4), Rain.of(
                    Drop.of(V.of(5), Rain.of())
                ))
            ))
        ))
    );

    @Test
    void traverse() {
        var actual = new ArrayList<>();
        graph.get().traverse(v -> {
            actual.add(v.value());
            return Promise.nil();
        }).join();
        assertEquals(Arrays.asList(1, 2, 4, 5, 3), actual);
    }

    @Test
    void unfold() {
        var actual = new ArrayList<>();
        Rain<Integer> copy = Rain.unfold(graph.get(), rain -> {
            return rain.unfix().map(drop -> Drop.of(V.of(drop.value() + 1), drop.next()));
        });

        copy.traverse(v -> {
            actual.add(v.value());
            return Promise.nil();
        }).join();
    
        assertEquals(Arrays.asList(2, 3, 5, 6, 4), actual);
    }

    @Test
    void fold() {
        Integer actual = graph.get().<Promise<Integer>>fold(drops -> drops.foldr(0, (drop, acc) -> drop.next().map(n -> acc + n + drop.value()))).join();
        assertEquals(24, actual);
    }

    @Test
    void paramorph() {
        Integer actual = Rain.<LList<Integer>, Integer>unfold(a, a$ -> LList.bind(a$.match(() -> LList.of(), (x, xs) -> LList.of(new Drop<>(new V(x), xs)))))
            .<Promise<Integer>>fold(droplets -> droplets.head().then(maybe -> maybe.match(() -> Promise.just(0), droplet -> droplet.next().map(x -> x + droplet.value())))).join();
        assertEquals(6, actual);
    }

    @Test
    void filter() {
        Rain<Integer> rain = Rain.unfold(0, i -> {
            return LList.of(new Drop<>(new V(i), i + 1));
        });

        rain = rain.filter(box -> {
            return box.value() < 3;
        });

        List<Integer> actual = new ArrayList<>();

        rain.traverse((v) -> {
            actual.add(v.value());
            return Promise.nil();
        }).join();
        assertEquals(Arrays.asList(0, 1, 2), actual);
    }

    @Test
    void mergeEmtpy() {
        Rain<Integer> rain = Rain.merge(LList.of());
        Promise<Integer> pActual = rain.fold(droplets -> droplets.foldl(Promise.just(0), (droplet, pAcc) -> null).then(Function.identity()));
        assertEquals(0, pActual.join());
    }

    @Test
    void map() {
        var actual = graph.get().map(v -> V.of(v.value() + 1)).<Integer>fold(drops -> drops.foldl(0, (drop, acc) -> Promise.just(drop.next() + drop.value() + acc)).join());
        assertEquals(31, actual);
    }

    @Test
    void resolve() {
        var actual = new ArrayList<>();
        Rain.bind(graph.get().resolve()).traverse(v -> {
            actual.add(v.value());
            return Promise.nil();
        }).join();
        assertEquals(Arrays.asList(1, 2, 4, 5, 3), actual);
    }
}
