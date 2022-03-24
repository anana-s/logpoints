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
        }).then(i -> {
            actual.add(1);
            return Promise.just(i);
        }).join();
        assertEquals(Arrays.asList(0, 1), actual);
    }
    @Test
    void resolution() {
        var actual = new ArrayList<>();
        var promise = Promise.lazy(() -> {
            actual.add(0);
            return 0;
        });

        promise.then(i -> Promise.just(i)).join();
        promise.map(i -> i).join();
        promise.then(i -> Promise.just(i)).map(i -> i).join();

        promise.join();

        assertEquals(Arrays.asList(0), actual);
    }
}
