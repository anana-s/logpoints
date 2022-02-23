package anana5.sense.logpoints;

public class ForLoopNoLogging {
    public static void main(String[] args) {
        int count = 0;
        for (String arg : args) {
            count += 1;
        }
        System.exit(count);
    }
}
