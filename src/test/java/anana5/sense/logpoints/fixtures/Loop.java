package anana5.sense.logpoints.fixtures;

public class Loop {
    public static void main(String[] args) {
        for (String arg : args) {
            if (!arg.equals("sweet")) {
                System.out.println("pineapple!");
            }
        }
    }
}
