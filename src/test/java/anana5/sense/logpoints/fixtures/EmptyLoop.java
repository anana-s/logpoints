package anana5.sense.logpoints.fixtures;

public class EmptyLoop {
    public static void main(String[] args) {
        for (String arg : args) {
            String.format("{}", arg);
        }
    }
}
