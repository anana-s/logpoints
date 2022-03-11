package anana5.sense.logpoints;

import java.util.Arrays;

public class InvocationFollowingEmptyIteratorLoop {
    public static void print(String str) {
        System.out.println(str);
    }
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            arg.toString();
        }
        print("sweet pineapple");
    }
}
