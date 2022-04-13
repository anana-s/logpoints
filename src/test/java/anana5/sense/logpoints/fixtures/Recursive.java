package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class Recursive {

    static Random rng = new Random();

    static void test1() {
        if (rng.nextBoolean()) {
            System.out.print("[");
            test1();
            System.out.print("]");
        }
    }

    static void test2() {
        if (rng.nextBoolean()) {
            test2();
        }
        System.out.print("2");
    }

    static void test4() {
        if (rng.nextBoolean()) {
            test4a();
        }
        System.out.print("4");
    }

    static void test4a() {
        if (rng.nextBoolean()) {
            test4();
        }
        System.out.print("4a");
    }

    public static void main(String[] args) {
        test1();
        test2();
        test4();
        System.out.println();
    }
}
