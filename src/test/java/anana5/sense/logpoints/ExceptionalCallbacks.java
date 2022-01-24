package anana5.sense.logpoints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExceptionalCallbacks {
    @FunctionalInterface
    interface Consumer<T> {
        void accept(T t) throws IOException;
    }
    List<Consumer<String>> callbacks;
    public ExceptionalCallbacks() {
        callbacks = new ArrayList<>();
    }
    public void add(Consumer<String> callback) {
        callbacks.add(callback);
    }
    public void run() throws IOException {
        for (Consumer<String> callback : callbacks) {
            callback.accept("sweet pineapple");
        }
    }
    public static void main(String[] args) {
        var o = new ExceptionalCallbacks();
        o.add(phrase -> System.out.println(phrase));
        try {
            o.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    
}
