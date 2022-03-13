package anana5.sense.logpoints.fixtures;

public class LoopInvokationWithBranching {
    public static Void test(String s) {
        if (s == null) {
            System.err.println("null");
        }
        return null;
    }
    public static void main(String[] args) {
        for (String arg : args) {
            test(arg);
        }
    }
    
}
