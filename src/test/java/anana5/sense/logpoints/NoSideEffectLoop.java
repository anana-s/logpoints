package anana5.sense.logpoints;

public class NoSideEffectLoop {
    public static void main(String[] args) {
        System.out.println(args);
        for (String arg : args);
        System.out.println("sweet pineapple!");
    }
}
