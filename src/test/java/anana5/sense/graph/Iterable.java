package anana5.sense.graph;

import java.util.Arrays;

public class Iterable {
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            System.out.println(arg);
        }
    }
}
