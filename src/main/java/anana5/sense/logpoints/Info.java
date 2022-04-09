package anana5.sense.logpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Info {
    private static Logger log = LoggerFactory.getLogger(Info.class);

    public static void main(String[] args) {
        try {
            ArgumentParser parser = ArgumentParsers.newFor("logpoints ping").build()
                .defaultHelp(true)
                .description("ping the logpoints deamon");

            parser.addArgument("address")
                .type(String.class);

            Namespace ns = parser.parseArgs(args);

            // traverse graph
            try (var client = Client.connect(ns.getString("address"))) {
                var vcount = client.send(graph -> {
                    return graph.vertices().size();
                });
                var ecount = client.send(graph -> {
                    return graph.edges().size();
                });
                System.out.println(client.vertices() + " " + client.edges().size());
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            e.printStackTrace();
        }
    }
    
}
