package anana5.sense.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Callback {
    @FunctionalInterface
    interface ICallBack {
        public void accept(String s);
    }
    
    List<Consumer<String>> callbacks;

    public Callback() {
        callbacks = new ArrayList<>();
    }

    public void add(Consumer<String> callback) {
        callbacks.add(callback);
    }

    public void run() {
        for (Consumer<String> callback : callbacks) {
            callback.accept("sweet pineapple");
        }
    }

    public static void say(String phrase) {
        System.out.println(phrase);
    }

    public static void indirect(String phrase) {
        say(phrase);
    }

    public static void main(String[] args) {
        Callback o = new Callback();
        o.add(phrase -> System.out.println(phrase));
        o.add(phrase -> say(phrase));
        o.add(phrase -> indirect(phrase));
        o.run();
    }
}
