package anana5.sense.logpoints;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Print {
    private static Logger log = LoggerFactory.getLogger(Print.class);

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

        // traverse graph
        try (var client = RemoteSerialRefGraph.connect(ns.getString("address")); var printer = new DotPrinter(System.out)) {
            var seen = new HashSet<SerialRef>();
            for (var root : client.roots()) {
                printer.discover(root);
                seen.add(root);
                client.traverse(root, (source, target) -> {
                    if (target.recursive()) {
                        return false;
                    }
                    printer.print(source, target);
                    if (seen.contains(target)) {
                        return false;
                    }
                    seen.add(target);
                    return true;
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
