package anana5.sense.logpoints.fixtures;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InvokationWithEmptyBranching {
    static Random rng = new Random();
    static void test1() {
        if (rng.nextBoolean()) {
            System.out.println("sweet pineapple!");
        }
        return;
    }
    static void test2() {
        var b = rng.nextBoolean();
        if (b) {
            b = b ^ b;
        }
        b = b ^ b;
        return;
    }
    static void test3(List<String> args) {
        for (String arg : args) {
            test1();
        }
    }
    public static void main(String[] args) {
        test1();
        test2();
        test3(Arrays.asList(args));
    }
}
