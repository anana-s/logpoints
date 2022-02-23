package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import anana5.graph.Box;
import anana5.graph.rainfall.Droplet;
import anana5.graph.rainfall.Rain;

public class RainTest {

    private final LList<Integer> a = new LList<>(1, 2, 3);
    
    @Test
    void unfold() {
        Rain<Integer> rain = Rain.unfold(3, i -> {
            if (i < 0) {
                return new LList<>();
            }

            return new LList<>(new Droplet<>(new Box<>(i), i - 1));
        });

        List<Integer> actual = new ArrayList<>();
        rain.traverse(box -> {
            actual.add(box.value());
        });

        assertEquals(Arrays.asList(3, 2, 1, 0), actual);
    }

    @Test
    void fold() {
        Rain<Integer> rain = Rain.unfold(3, i -> {
            if (i < 0) {
                return new LList<>();
            }

            return new LList<>(new Droplet<>(new Box<>(i), i - 1));
        });

        Promise<Integer> out = rain.fold(droplets -> {
            return droplets.foldr(0, (droplet, a) -> {
                return droplet.next().map(b -> {
                    return droplet.get().value() + a + b;
                });
            });
        });

        Integer actual = out.run();

        assertEquals(6, actual);
    }

    @Test
    void filter() {
        Rain<Integer> rain = Rain.unfold(3, i -> {
            return new LList<>(new Droplet<>(new Box<>(i), i - 1));
        });

        rain = rain.filter(box -> {
            return !(box.value() < 0);
        });

        List<Integer> actual = new ArrayList<>();
        rain.traverse(box -> {
            actual.add(box.value());
        });

        assertEquals(Arrays.asList(3, 2, 1, 0), actual);
    }
}
