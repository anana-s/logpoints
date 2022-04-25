package anana5.sense.logpoints;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.util.ListF;
import anana5.util.PList;
import anana5.util.Promise;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Match implements Callable<Match.Head> {
    private static final Logger log = LoggerFactory.getLogger(Match.class);
    private static final Pattern pattern = Pattern.compile("^(?<m>[^\\(\\s]*)\\((?<s>[^\\:\\s]*)\\:(?<l>\\d+)\\)");

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
            for (Head h : path(head)) {
                System.out.println(h);
            }

        } catch (EOFException e) {
            log.info("connection closed");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    final Queue<Head> queue;
    final Graph<SerialRef> graph;

    private Match(Graph<SerialRef> graph, Collection<SerialRef> roots, PList<String> lines) throws IOException {
        List<Head> heads = roots.stream().map(serial -> new SerialHead(null, serial, lines)).collect(Collectors.toList());
        this.queue = Collections.asLifoQueue(new ConcurrentLinkedDeque<>(heads));
        this.graph = graph;
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

    static List<Head> path(Head head) {
        var path = Stream.iterate(head, Head::previous).takeWhile(h -> h != null);
        var list = path.collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    interface Head {
        Head previous();
        Collection<Head> next();
        boolean done();
    }

    abstract class BaseSerialHead implements Head {
        private final Head prev;
        private final PList<String> lines;

        public BaseSerialHead(Head prev, PList<String> lines) {
            this.prev = prev;
            this.lines = lines;
        }

        @Override
        public boolean done() {
            return lines.empty().join();
        }

        @Override
        public Head previous() {
            return prev;
        }

        PList<String> lines() {
            return lines;
        }

    }

    static PList<String> eat(PList<String> lines) {
        var line = lines.head().join();
        while (!pattern.matcher(line).matches()) {
            lines = lines.tail();
            line = lines.head().join();
        }
        return lines;
    }

    class RootSerialHead extends BaseSerialHead {
        final Collection<SerialRef> roots;
        RootSerialHead(Collection<SerialRef> roots, PList<String> lines) {
            super(null, lines);
            this.roots = roots;
        }

        @Override
        public Collection<Head> next() {
            final var lines = eat(lines());
            return roots.stream().map(serial -> new SerialHead(this, serial, lines)).collect(Collectors.toList());
        }
    }

    class SerialHead extends BaseSerialHead {
        private final SerialRef serial;

        SerialHead(Head prev, SerialRef serial, PList<String> lines) {
            super(prev, lines);
            this.serial = serial;
        }

        @Override
        public List<Head> next() {
            return lines().unfix().join().match(() -> {
                return Collections.emptyList();
            }, (line, next) -> {
                if (!match(line)) {
                    return Collections.emptyList();
                }

                final var serials = graph.from(serial);
                final var lines = eat(next);

                if (serials.isEmpty()) {
                    return Collections.singletonList(new StopSerialHead(this, lines));
                }
                return serials.stream().map(s -> new SerialHead(this, s, lines)).collect(Collectors.toList());
            });
        }

        @Override
        public boolean done() {
            return false;
        }

        private boolean match(String line) {
            var matcher = pattern.matcher(line);

            if (!matcher.find()) {
                return false;
            }

            var methodName = matcher.group("m");
            var sourceFile = matcher.group("s");
            var lineNumber = Integer.valueOf(matcher.group("l"));

            return serial.method().equals(methodName) && serial.source().equals(sourceFile) && serial.line() == lineNumber;
        }
    }

    class StopSerialHead extends BaseSerialHead {

        public StopSerialHead(Head previous, PList<String> lines) {
            super(previous, lines);
        }

        @Override
        public Collection<Head> next() {
            return Collections.emptyList();
        }

        @Override
        public boolean done() {
            return super.done();
            // return true;
        }
    }
}
