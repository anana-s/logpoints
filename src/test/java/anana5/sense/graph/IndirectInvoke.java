package anana5.sense.graph;

public class IndirectInvoke {
    public static void greet() {
        System.out.println("sweet pineapple");
    }

    public static void indirect() {
        greet();
    }

    public static void main(String[] args) {
        indirect();
    }
}
