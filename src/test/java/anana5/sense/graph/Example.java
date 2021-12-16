package anana5.sense.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Example {
    @FunctionalInterface
    interface ICallBack {
        public void accept(String s) throws IOException;
    }

    static Logger logger = LoggerFactory.getLogger(Example.class);
    
    List<ICallBack> callbacks;

    public Example() {
        callbacks = new ArrayList<>();
    }

    public void add(ICallBack callback) {
        logger.info("Added Callback: " + callback.toString());
        callbacks.add(callback);
    }

    public void run() {
        logger.info("Started");
        for (ICallBack callback : callbacks) {
            try {
                callback.accept("sweet pineapple");
            } catch (IOException e) {
                logger.info("Io Error");
            }
        }
        logger.info("Stopped");
    }

    public static void main(String[] args) {
        Example o = new Example();
        o.add(phrase -> logger.info(phrase));
        logger.info("Created");
        if (args.length != 0) {
            logger.info("Wrong number of args");
            System.exit(1);
        } else {
            o.run();
        }
    }
}
