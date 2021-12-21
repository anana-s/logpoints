package anana5.sense.logpoints;

public class ExceptionLoop {
    public static void main(String[] args) {
        for (String arg : args) {
            try {
                System.out.println(arg);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("sweet pineapple");
    }
}
