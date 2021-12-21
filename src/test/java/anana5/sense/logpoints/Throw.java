package anana5.sense.logpoints;

public class Throw {
    public static void main(String[] args) {
        while (true) {
            try {
                throw new Exception("sweet pineapple");
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }
    }
}
