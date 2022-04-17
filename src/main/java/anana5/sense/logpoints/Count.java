package anana5.sense.logpoints;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Count {
    private static Logger log = LoggerFactory.getLogger(Count.class);

    public static void main(String[] args) {
        try {
            ArgumentParser parser = ArgumentParsers.newFor("logpoints ping").build()
                .defaultHelp(true)
                .description("ping the logpoints deamon");

            parser.addArgument("address")
                .type(String.class);

            Namespace ns = parser.parseArgs(args);

            // traverse graph
            try (var client = RemoteSerialRefGraph.connect(ns.getString("address"))) {
                var vcount = client.send(graph -> {
                    var vertices = new HashSet<SerialRef>();
                    for (var root : graph.roots()) {
                        if (root.sentinel() || vertices.contains(root)) {
                            continue;
                        }
                        vertices.add(root);
                        graph.traverse(root, (src, tgt) -> {
                            if (tgt.returns() || tgt.recursive() || tgt.sentinel() || vertices.contains(tgt)) {
                                return false;
                            }
                            vertices.add(tgt);
                            return true;
                        });
                    }
                    return vertices.size();
                });
                System.out.println(vcount);
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            e.printStackTrace();
        }
    }
    

    // private static class Counter implements Iterator<Integer> {
    //     private int count = 0;

    //     @Override
    //     public boolean hasNext() {
    //         return count <= Integer.MAX_VALUE;
    //     }

    //     @Override
    //     public Integer next() {
    //         return count++;
    //     }

    //     public Integer get() {
    //         return count;
    //     }
    // }
}
