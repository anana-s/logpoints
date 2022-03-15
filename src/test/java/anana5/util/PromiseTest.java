package anana5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class PromiseTest {
    @Test
    void order() {
        var actual = new ArrayList<>();
        Promise.lazy(() -> {
            actual.add(0);
            return 0;
        }).effect(i -> {
            actual.add(1);
        }).join();
        assertEquals(Arrays.asList(0, 1), actual);
    }
}
