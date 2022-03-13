package anana5.sense.logpoints.fixtures;

public class LoopInvoke {
    public static void test(String s) {
        if (s == null) {
            System.err.println("null");
            return;
        }
        return;
    }
    public static void main(String[] args) {
        for (String arg : args) {
            test(arg);
        }
    }
    
}
