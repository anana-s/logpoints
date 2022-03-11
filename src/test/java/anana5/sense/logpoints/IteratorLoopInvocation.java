package anana5.sense.logpoints;

import java.util.Arrays;

public class IteratorLoopInvocation {
    public static void print1(String str) {
        System.out.println(str);
    }
    public static void print2(String str) {
        System.out.println(str);
    }
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            print1(arg);
            print2(arg);
        }
        print1("sweet pineapple");
        print2("sweet pineapple");
    }
}
