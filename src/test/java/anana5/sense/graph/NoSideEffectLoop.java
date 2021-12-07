package anana5.sense.graph;

public class NoSideEffectLoop {
    public static void main(String[] args) {
        while(true) {
            String[] sgra = args;
            int a = 1 + 1;
        }
    }
}
