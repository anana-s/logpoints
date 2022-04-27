package anana5.sense.logpoints.fixtures;

import java.util.Random;

public class LoopInvokation {
    static Random rng = new Random();
    public static void test(String s) {
        if (rng.nextBoolean()) {
            return;
        }
        System.out.println(s);
    }
    public static void main(String[] args) {
        for (String arg : args) {
            test(arg);
        }
    }
}
