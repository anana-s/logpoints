package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import anana5.graph.rainfall.Droplet;
import anana5.graph.rainfall.Rain;

public class RainTest {

    private final LList<Integer> a = new LList<>(1, 2, 3);

    @Test
    void paramorph() {
        Rain<Integer> rain = Rain.unfold(a, a$ -> LList.bind(a$.match(() -> LList.nil(), (x, xs) -> new LList<>(new Droplet<Integer, LList<Integer>>(x, xs)))));

        Promise<Integer> out = rain.fold(droplets -> droplets.head().then(maybe -> maybe.match(() -> Promise.just(0), droplet -> droplet.next().map(x -> x + droplet.get().value()))));
        var actual = out.join();

        assertEquals(6, actual);
    }

    @Test
    void filter() {
        Rain<Integer> rain = Rain.unfold(0, i -> {
            return new LList<>(new Droplet<>(i, i + 1));
        });

        rain = rain.filter(box -> {
            return box.value() < 3;
        });

        List<Integer> actual = new ArrayList<>();
        rain.traverse(box -> {
            actual.add(box.value());
        });

        assertEquals(Arrays.asList(0, 1, 2), actual);
    }
}
