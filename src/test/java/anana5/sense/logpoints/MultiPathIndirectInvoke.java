package anana5.sense.logpoints;

public class MultiPathIndirectInvoke {
    public static void greet() {
        System.out.println("sweet pineapple");
    }

    public static void indirect() {
        greet();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            greet();
        } else {
            indirect();
        }
    }
}
