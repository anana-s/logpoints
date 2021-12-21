package anana5.sense.logpoints;

public class Recursive {
    public static void greet() {
        System.out.println("sweet pineapple");
        greet();
    }

    public static void main(String[] args) {
        System.out.println("Started");
        greet();
    }
}
