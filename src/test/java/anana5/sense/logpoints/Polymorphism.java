package anana5.sense.logpoints;

public class Polymorphism {
    public static void main(String[] args) {
        I i = new A();
        if (args.length == 0) {
            i = new B();
        }
        i.run();
    }
    public interface I {
        void run();
    }
    public static class A implements I{
        @Override
        public void run() {
            new B().run();
        }
    }
    public static class B implements I {
        @Override
        public void run() {
            new A().run();
        }
    }
}
