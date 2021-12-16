package anana5.sense.graph;

public class NoSideEffectLoop {
    public static void main(String[] args) {
        System.out.println(args);
        for (String arg : args);
        System.out.println("sweet pineapple!");
    }
}
