package anana5.sense.logpoints.fixtures;

public class LoopInvokation {
    public static void test(String s) {
        System.out.println(s);
    }
    public static void main(String[] args) {
        for (String arg : args) {
            test(arg);
        }
    }
}
