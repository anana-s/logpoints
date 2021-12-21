package anana5.sense.logpoints;

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
        logger.debug("added callback: {}", callback.toString());
        callbacks.add(callback);
    }

    public void run() throws IOException {
        logger.debug("started");
        for (ICallBack callback : callbacks) {
            try {
                callback.accept("sweet pineapple");
            } catch (IOException e) {
                logger.debug("encountered io error");
                throw e;
            }
        }
        logger.debug("stopped");
    }

    public static void main(String[] args) {
        Example o = new Example();
        o.add(phrase -> logger.info("call {}", phrase));
        logger.info("initialized");
        if (args.length != 0) {
            logger.error("wrong number of args");
            System.exit(1);
            return;
        } else {
            try {
                o.run();
            } catch (IOException e) {
                logger.error("cannot recover from io error");
                System.exit(2);
                return;
            }
        }
        logger.info("successfully ran all");
    }
}
