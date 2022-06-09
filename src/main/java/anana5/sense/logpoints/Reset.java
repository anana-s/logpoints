package anana5.sense.logpoints;

import java.io.IOException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Reset {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints ping").build()
            .defaultHelp(true)
            .description("ping the logpoints deamon");

        parser.addArgument("address")
            .type(String.class);

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.printHelp();
            return;
        }

        try (var client = RemoteGraph.connect(ns.getString("address"))) {
            client.send(logpoints -> {
                logpoints.reset();
                return null;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
