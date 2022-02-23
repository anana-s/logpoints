package anana5.sense.logpoints;

import java.util.Arrays;

public class IteratorLoopInvocation {
    public static void print(String str) {
        System.out.println(str);
    }
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            print(arg);
            print(arg);
            print(arg);
        }
        print("sweet pineapple");
    }
}
