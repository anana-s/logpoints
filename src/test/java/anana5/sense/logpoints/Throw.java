package anana5.sense.logpoints;

public class Throw {
    public static void main(String[] args) {
        try {
            for (String arg : args) {
                throw new Exception(arg);
            }
            System.out.println("sweet pineapple");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
