package anana5.sense.logpoints;

public class ForLoopNoLogging {
    public static void main(String[] args) {
        int total = 0;
        for (String arg : args) {
            total += arg.length();
        }
        System.exit(total);
    }
}
