package anana5.sense.logpoints;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Ping {
    private static Logger log = LoggerFactory.getLogger(Ping.class);
    public static void main(String[] args) {
            ArgumentParser parser = ArgumentParsers.newFor("logpoints ping").build()
                .defaultHelp(true)
                .description("ping the logpoints deamon");

            parser.addArgument("-i", "--interval")
                .type(Integer.class)
                .setDefault(1000)
                .help("interval in milliseconds");

            parser.addArgument("address")
                .type(String.class);


            Namespace ns;
            try {
                 ns = parser.parseArgs(args);

            } catch (ArgumentParserException e) {
                parser.printHelp();
                return;
            }

            String address = ns.getString("address");
            int interval = ns.getInt("interval");

            log.debug("pinging {} with {} ms interval", address, interval);

            RemoteGraph client = null;
            while (true) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    try {
                        client.close();
                    } catch (IOException ee) {
                        ee.printStackTrace();
                    }
                    return;
                }

                if (client == null) {
                    try {
                        client = RemoteGraph.connect(address);
                    } catch (IOException e) {
                        log.error("connection refused");
                        continue;
                    }
                }

                System.out.print("ping");
                long ping = System.nanoTime();

                try {
                    client.send(g -> {
                        return true;
                    });
                } catch (IOException e) {
                    log.error("disconnected");
                    System.out.println();
                    continue;
                }

                long pong = System.nanoTime();
                System.out.println(" " + (pong - ping) / 1000000.0 + "ms pong");
            }
    }
}
