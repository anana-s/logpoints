package anana5.sense.graph;

public class Recursive {
    public static void greet() {
        System.out.println("sweet pineapple");
        greet();
    }

    public static void main(String[] args) {
        greet();
    }
}
