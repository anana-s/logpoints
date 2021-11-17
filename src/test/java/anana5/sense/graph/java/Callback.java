package anana5.sense.graph.java;

import java.util.function.Consumer;

public class Callback {
    Consumer<String> callback;

    public Callback(Consumer<String> callback) {
        this.callback = callback;
    }

    public void run() {
        callback.accept("sweet pineapple");
    }

    public static void say(String phrase) {
        System.out.println(phrase);
    }

    public static void main(String[] args) {
        new Callback(phrase -> say(phrase)).run();
    }
}
