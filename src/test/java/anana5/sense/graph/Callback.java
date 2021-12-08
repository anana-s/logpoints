package anana5.sense.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Callback {
    
    List<Consumer<Object>> callbacks;

    public Callback() {
        callbacks = new ArrayList<>();
    }

    public void add(Consumer<Object> callback) {
        System.out.println("Added Callback: " + callback.toString());
        callbacks.add(callback);
    }

    public void run() {
        for (Consumer<Object> callback : callbacks) {
            System.out.println("Calling: " + callback.toString());
            callback.accept("sweet pineapple");
        }
    }

    public static void say(String phrase) {
        System.out.println(phrase);
    }

    public static void indirect(String phrase) {
        say(phrase);
    }

    static class Greeter implements Consumer<Object> {
        @Override
        public void accept(Object s) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {
        System.out.println("Callbacks started");
        Callback o = new Callback();
        o.add(phrase -> System.out.println(phrase));
        o.add(new Greeter());
        o.run();
    }
}
