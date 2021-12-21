package anana5.sense.logpoints;

import java.util.Arrays;

public class ExceptionIterable {
    public static void main(String[] args) {
        for (String arg : Arrays.asList(args)) {
            try {
                System.out.println(arg);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("sweet pineapple");
    }
    
}
