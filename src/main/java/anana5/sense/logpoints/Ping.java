package anana5.sense.logpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Ping {
    private static Logger log = LoggerFactory.getLogger(Ping.class);
    public static void main(String[] args) {
        try {
            ArgumentParser parser = ArgumentParsers.newFor("logpoints ping").build()
                .defaultHelp(true)
                .description("ping the logpoints deamon");

            parser.addArgument("-i", "--interval")
                .type(Integer.class)
                .setDefault(1000)
                .help("interval in milliseconds");

            parser.addArgument("address")
                .type(String.class);


            Namespace ns = parser.parseArgs(args);

            String address = ns.getString("address");
            int interval = ns.getInt("interval");

            log.debug("connecting to {}", address);
            var client = Client.connect(address);
            log.debug("pinging {} with {} ms interval", address, interval);

            try {
                while (true) {
                    System.out.print("ping");
                    long ping = System.nanoTime();
                    client.send(g -> {
                        return true;
                    });
                    long pong = System.nanoTime();
                    System.out.println(" " + (pong - ping) / 1000000.0 + "ms pong");
                    Thread.sleep(interval);
                }
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
