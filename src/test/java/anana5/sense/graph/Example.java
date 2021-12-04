package anana5.sense.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Example {
    @FunctionalInterface
    interface ICallBack {
        public void accept(String s) throws IOException;
    }
    
    List<ICallBack> callbacks;

    public Example() {
        callbacks = new ArrayList<>();
    }

    public void add(ICallBack callback) {
        System.out.println("Added Callback: " + callback.toString());
        callbacks.add(callback);
    }

    public void run() {
        System.out.println("Started");
        for (ICallBack callback : callbacks) {
            try {
                callback.accept("sweet pineapple");
            } catch (IOException e) {
                System.out.println("Io Error");
            }
        }
        System.out.println("Stopped");
    }

    public static void main(String[] args) {
        Example o = new Example();
        o.add(phrase -> System.out.println(phrase));
        System.out.println("Created");
        if (args.length != 0) {
            System.out.println("Wrong number of args");
            System.exit(1);
        } else {
            o.run();
        }
    }
}
