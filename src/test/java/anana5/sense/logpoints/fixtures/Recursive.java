package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class Recursive {

    static Random rng = new Random();

    static void test() {
        if (rng.nextBoolean()) {
            test();
            System.out.println("sweet pineapple!");
        }
    }

    public static void main(String[] args) {
        test();
    }
}
