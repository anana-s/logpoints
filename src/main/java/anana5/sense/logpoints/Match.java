package anana5.sense.logpoints;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

public class Match implements Callable<Collection<Match.Head>> {
    private static final Logger log = LoggerFactory.getLogger(Match.class);

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
            .setDefault("(?<m>[\\w.]+)\\((?<s>[\\\\/\\w.]*)\\:(?<l>\\d+)\\)");

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

            var matcher = new Match(graph, graph.roots(), lines, Pattern.compile(ns.get("pattern")));

            var heads = matcher.call();

            // var paths = heads.stream().map(Match::path).collect(Collectors.toList());

            // for (var path : paths) {
            //     System.out.println(path);
            // }

        } catch (EOFException e) {
            log.info("connection closed");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    final Queue<Head> queue;
    final Queue<Head> sentinels;
    final Graph<SerialRef> graph;
    final Pattern pattern;

    private Match(Graph<SerialRef> graph, Collection<SerialRef> roots, PList<String> lines, Pattern pattern) throws IOException {

        roots = roots.stream().filter(s -> !(s.recursive() || s.returns())).collect(Collectors.toList());

        List<Head> heads = Collections.singletonList(new RootSerialHead(roots, 8, 0, lines));
        // this.queue = new ConcurrentLinkedDeque<>(heads);
        this.queue = Collections.asLifoQueue(new ConcurrentLinkedDeque<>());
        this.sentinels = Collections.asLifoQueue(new ConcurrentLinkedDeque<>(heads));
        this.graph = graph;
        this.pattern = pattern;
    }

    public Collection<Head> call() throws IOException {
        // main loop
        var out = new ArrayList<Head>();
        var seen = new HashSet<Head>();

        while (!sentinels.isEmpty()) {
            var sentinel = sentinels.poll();

            queue.add(sentinel);

            while (!queue.isEmpty()) {
                var head = queue.poll();

                seen.add(head);

                if (head.done()) {
                    out.add(head);
                    System.out.println(path(head));
                    if (out.size() > 100) {
                        return out;
                    }
                }

                for (var h : head.next()) {
                    if (seen.contains(h)) {
                        continue;
                    }
                    if (h.sentinel()) {
                        sentinels.add(h);
                    } else {
                        queue.add(h);
                    }
                }
            }
        }

        return out;
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
        boolean sentinel();
    }

    abstract class BaseSerialHead implements Head {
        private final Head prev;
        private final PList<String> lines;
        private final Collection<SerialRef> roots;
        private final int sequence;

        public BaseSerialHead(Collection<SerialRef> roots, Head prev, int sequence, PList<String> lines) {
            this.prev = prev;
            this.lines = lines;
            this.roots = roots;
            this.sequence = sequence;
        }

        @Override
        public boolean done() {
            return lines.empty().join();
        }

        @Override
        public Head previous() {
            return prev;
        }

        Collection<SerialRef> roots() {
            return this.roots;
        }

        PList<String> lines() {
            return lines;
        }

        int seq() {
            return sequence;
        }
    }

    PList<String> eat(PList<String> lines) {
        var line = lines.head().join();
        while (!pattern.matcher(line).find()) {
            lines = lines.tail();
            line = lines.head().join();
        }
        return lines;
    }

    class RootSerialHead extends BaseSerialHead {
        final int recursion;

        RootSerialHead(Collection<SerialRef> roots, int recursion, int sequence, PList<String> lines) {
            super(roots, null, sequence, lines);
            this.recursion = recursion;
        }

        @Override
        public Collection<Head> next() {
            final var lines = eat(lines());
            if (lines.empty().join()) {
                return Collections.emptyList();
            }
            return roots().stream().map(serial -> new SerialHead(roots(), this, recursion, seq(), serial, lines)).collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return String.format("root ~ %d", seq());
        }

        @Override
        public boolean sentinel() {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RootSerialHead other = (RootSerialHead) obj;
            return roots().equals(other.roots()) && seq() == other.seq();
        }

        @Override
        public int hashCode() {
            return Objects.hash(roots(), seq());
        }
    }

    class SerialHead extends BaseSerialHead {
        private final SerialRef serial;
        private final int recursion;

        SerialHead(Collection<SerialRef> roots, Head prev, int recursion, int sequence, SerialRef serial, PList<String> lines) {
            super(roots, prev, sequence, lines);
            this.serial = serial;
            this.recursion = recursion;
        }

        @Override
        public List<Head> next() {
            var lines = lines();
            if (lines.empty().join()) {
                return Collections.emptyList();
            }

            if (serial.returns()) {
                return Collections.singletonList(new RootSerialHead(roots(), recursion, seq(), lines));
            }

            var line = lines.head().join();

            if (!serial.sentinel() && !match(line)) {
                return Collections.emptyList();
            }

            if (recursion == 0 && serial.sentinel()) {
                return Collections.emptyList();
            }

            final var future = CompletableFuture.supplyAsync(() -> graph.from(serial));
            try {
                final var serials = future.get(10, TimeUnit.MINUTES);
                var next = eat(lines.tail());

                if (serials.isEmpty()) {
                    // dunno what happened here
                    return Collections.singletonList(new StopSerialHead(roots(), this, seq() + 1, next));
                } else if (serial.sentinel()) {
                    // match with the neighbours of the sentinel
                    return serials.stream().map(s -> new SerialHead(roots(), this, recursion - 1, seq(), s, lines)).collect(Collectors.toList());
                } else {
                    // match next with neighbours
                    return serials.stream().map(s -> new SerialHead(roots(), this, recursion, seq() + 1, s, next)).collect(Collectors.toList());
                }
            } catch (TimeoutException e) {
                log.error("timeout while getting neighbors of {}", serial);
                future.cancel(true);
                return Collections.emptyList();
            } catch (InterruptedException | ExecutionException e) {
                log.error("error while getting neighbors of {}", serial);
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        @Override
        public boolean done() {
            return serial.returns();
        }

        @Override
        public boolean sentinel() {
            return this.serial.sentinel();
        }

        private boolean match(String line) {
            var matcher = pattern.matcher(line);

            if (!matcher.find()) {
                return false;
            }

            var methodName = matcher.group("m");
            var sourceFile = matcher.group("s");
            var lineNumber = Integer.valueOf(matcher.group("l")).intValue();

            return serial.method().equals(methodName) && serial.source().equals(sourceFile) && serial.line() == lineNumber;
        }

        @Override
        public String toString() {
            return String.format("%s ~ %d", serial, seq());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SerialHead other = (SerialHead) obj;
            return serial.equals(other.serial) && roots().equals(other.roots()) && seq() == other.seq() && previous().equals(other.previous());
        }

        @Override
        public int hashCode() {
            return Objects.hash(serial, roots(), seq(), previous());
        }
    }

    class StopSerialHead extends BaseSerialHead {

        public StopSerialHead(Collection<SerialRef> roots, Head previous, int sequence, PList<String> lines) {
            super(roots, previous, sequence, lines);
        }

        @Override
        public Collection<Head> next() {
            return Collections.emptyList();
        }

        @Override
        public boolean done() {
            return true;
            // return true;
        }

        @Override
        public boolean sentinel() {
            return false;
        }

        @Override
        public String toString() {
            return String.format("nil ~ %d", seq());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            StopSerialHead other = (StopSerialHead) obj;
            return roots().equals(other.roots()) && seq() == other.seq() && previous().equals(other.previous());
        }

        @Override
        public int hashCode() {
            return Objects.hash(roots(), seq(), previous());
        }

    }
}
