package anana5.sense.logpoints.fixtures;

import java.util.Arrays;

public class Fixture1 {

    static void shouldThrow(boolean should) {
        if (should) {
            throw new RuntimeException("boom");
        }
    }

    static void target(String[] args) {
        for (String arg : Arrays.asList(args)) {
            if (arg.equals("sweet pineapple")) {
                System.out.println("sweet pineapple");
            }
            try {
                shouldThrow(true);
            } catch (Exception e) {
                System.out.println("eek");
            }
        }
    }

    static void indirection1(String[] args) {
        target(args);
    }

    static void indirection2(String[] args) {
        target(args);
    }

    public static void main(String[]  args) {
        target(args);
        target(args);
    }
}
