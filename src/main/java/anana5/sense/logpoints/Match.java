package anana5.sense.logpoints;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.util.ListF;
import anana5.util.PList;
import anana5.util.Promise;
import anana5.util.Tuple;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Match implements Callable<Head> {
    static final Logger log = LoggerFactory.getLogger(Match.class);

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints match").build()
            .defaultHelp(true)
            .description("match logs against logpoints");

        parser.addArgument("address")
            .type(String.class);

        parser.addArgument("input")
            .nargs("?")
            .type(FileInputStream.class)
            .setDefault(System.in);

        // parser.addArgument("output")
        //     .nargs("?")
        //     .type(PrintStream.class)
        //     .setDefault(System.out);

        parser.addArgument("--pattern")
            .setDefault("^(?<m>[^\\(\\s]*)\\((?<s>[^\\:\\s]*)\\:(?<l>\\d+)\\)");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.printHelp();
            return;
        }

        // traverse graph
        try (var in = ns.<InputStream>get("input"); var graph = RemoteSerialRefGraph.connect(ns.get("address"))) {
            // create a plist of all logging statements
            var reader = new BufferedReader(new InputStreamReader(in));
            final var lines = PList.<BufferedReader, String>unfold(reader, r -> {
                return Promise.lazy(() -> {
                    try {
                        var line = r.readLine();
                        if (line == null) {
                            return ListF.nil();
                        }
                        return ListF.cons(line, r);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            });

            var matcher = new Match(graph, graph.roots(), lines);

            var head = matcher.call();
            if (head == null) {
                log.info("no match");
                return;
            }

            log.info("matched");

        } catch (EOFException e) {
            log.info("connection closed");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    final Queue<Head> queue;

    private Match(Graph<SerialRef> graph, Collection<SerialRef> roots, PList<String> lines) throws IOException {
        List<Head> heads = roots.stream().map(serial -> new SerialHead(null, graph, serial, lines)).collect(Collectors.toList());
        this.queue = Collections.asLifoQueue(new ConcurrentLinkedDeque<>(heads));
    }

    public Head call() throws IOException {
        // main loop
        while (!queue.isEmpty()) {
            var head = queue.poll();

            if (head.done()) {
                return head;
            }

            queue.addAll(head.next());
        }

        return null;
    }
}
