package anana5.sense.logpoints;

public class PolymorphicRecursion {
    public static void main(String[] args) {
        boolean b = args.length == 0;
        I i = new A();
        if (b) {
            i = new B();
        }
        i.run(b);
    }
    public interface I {
        void run(boolean b);
    }
    public static class A implements I{
        @Override
        public void run(boolean b) {
            I i = new A();
            if (b) {
                i = new B();
            }
            i.run(b);
        }
    }
    public static class B implements I {
        @Override
        public void run(boolean b) {
            I i = new A();
            if (b) {
                i = new B();
            }
            i.run(b);
        }
    }
}
