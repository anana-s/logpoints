package anana5.sense.logpoints.fixtures;

import java.io.IOException;
import java.time.LocalDateTime;
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

    List<ICallBack> callbacks = new ArrayList<>();

    public Example() {
        logger.debug("initialized");
    }

    public void add(ICallBack callback) {
        logger.debug("added callback: {}", callback.toString());
        callbacks.add(callback);
    }

    public void run() throws RuntimeException {
        logger.debug("started");
        for (ICallBack callback : callbacks) {
            try {
                callback.accept("sweet pineapple !");
            } catch (IOException e) {
                logger.error("encountered io error: {}", e.getLocalizedMessage());
                logger.error("ignoring");
            } catch (RuntimeException e) {
                logger.debug("encountered runtime error: {}", e.getLocalizedMessage());
                throw e;
            }
        }
        logger.debug("stopped");
    }

    public static void main(String[] args) {
        logger.info("started {}", LocalDateTime.now());
        Example o = new Example();
        o.add(phrase -> logger.info("call {} 1", phrase));
        o.add(phrase -> logger.info("call {} 2", phrase));
        if (args.length != 0) {
            logger.error("wrong number of args");
            System.exit(1);
            return;
        } else {
            try {
                o.run();
            } catch (RuntimeException e) {
                logger.error("cannot recover from runtime error");
                System.exit(2);
                return;
            }
        }
        logger.info("done {}", LocalDateTime.now());
    }
}
