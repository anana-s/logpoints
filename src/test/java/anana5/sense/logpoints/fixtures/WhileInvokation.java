package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class WhileInvokation {
    static Random rng = new Random();
    static boolean test() {
        return rng.nextBoolean();
    }
    public static void main(String[] args) {
        while (test()) {
            if (test()) {
                System.out.println("hello");
            }
        }
    }
}
