package anana5.sense.logpoints;

public class MultipleInvocation {
    static void test() {
        System.out.println("sweet pineapple");
    }
    public static void main(String[] args) {
        test();
        test();
        test();
    }
}
