package anana5.sense.logpoints.fixtures;

public class InvocationWithEmptyBranching {
    static void test(boolean b) {
        if (b) {
            b = b ^ b;
        }
        b = b ^ b;
        return;
    }
    public static void main(String[] args) {
        System.out.println("sweet pineapple!");
        test(true);
        while(true) {
            System.out.println("sweet pineapple!");
        }
    }
}
