package anana5.sense.logpoints.fixtures;

public class InvokationWithEmptyBranching {
    static void test(boolean b) {
        if (b) {
            b = b ^ b;
        }
        b = b ^ b;
        return;
    }
    public static void main(String[] args) {
        test(true);
        System.out.println("sweet pineapple!");
    }
}
