package anana5.sense.logpoints;

import java.util.Arrays;

public class Iterable {
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            System.out.println(arg);
        }
        System.out.println("sweet pineapple");
    }
}
