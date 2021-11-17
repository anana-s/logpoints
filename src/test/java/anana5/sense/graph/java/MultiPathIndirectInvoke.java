package anana5.sense.graph.java;

public class MultiPathIndirectInvoke {
    public static void greet() {
        System.out.println("sweet pineapple");
    }

    public static void indirect() {
        greet();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("sweet pineapple");
        } else {
            System.out.println("sweet pineapple");
        }
    }
}
