package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class Recursive {

    static Random rng = new Random();

    interface Test {
        void test();
    }

    static class A implements Test {
        Test other;

        @Override
        public void test() {
            if (rng.nextBoolean()) {
                other.test();
            }
        }
    }



    static class B implements Test {
        Test other;

        @Override
        public void test() {
            if (rng.nextBoolean()) {
                other.test();
            } else {
                Recursive.test(other);
            }
        }
    }

    static void test(Test other) {
        if (rng.nextBoolean()) {
            other.test();
        }
    }

    public static void main(String[] args) {
        new A().test();
    }
}
