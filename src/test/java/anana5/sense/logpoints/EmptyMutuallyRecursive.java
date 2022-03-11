package anana5.sense.logpoints;

public class EmptyMutuallyRecursive {
    public static void a() {
        b();
    }
    public static void b() {
        a();
    }
    public static void main(String[] args) {
        a();
    }
}
