package anana5.sense.logpoints;

public class MutuallyRecursive {
    public static void greet(boolean b) {
        System.out.println("sweet pineapple");
        indirect(b);
    }

    public static void indirect(boolean b) {
        if (!b) {
            return;
        }
        greet(b);
    }

    public static void main(String[] args) {
        indirect(args.length == 0);
        System.out.println("sweet pineapple");
    }
}
