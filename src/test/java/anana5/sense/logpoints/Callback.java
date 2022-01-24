package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Callback {
    
    List<Consumer<Object>> callbacks;

    public Callback() {
        callbacks = new ArrayList<>();
    }

    public void add(Consumer<Object> callback) {
        callbacks.add(callback);
    }

    public void run() {
        for (Consumer<Object> callback : callbacks) {
            callback.accept("sweet pineapple");
        }
    }

    public static void main(String[] args) {
        Callback o = new Callback();
        o.add(phrase -> System.out.println(phrase));
        o.run();
    }
}
