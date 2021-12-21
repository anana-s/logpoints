package anana5.sense.logpoints;

public class MutuallyRecursive {
    public static void greet() {
        System.out.println("sweet pineapple");
        indirect();
    }

    public static void indirect() {
        greet();
    }

    public static void main(String[] args) {
        System.out.println("Started");
        indirect();
    }
}
