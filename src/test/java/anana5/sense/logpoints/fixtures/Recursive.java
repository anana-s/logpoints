package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class Recursive {

    static Random rng = new Random();

    static void test() {
        if (rng.nextBoolean()) {
            System.out.print("[");
            test();
            System.out.print("]");
        }
    }

    public static void main(String[] args) {
        test();
        System.out.println();
    }
}
