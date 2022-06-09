package anana5.sense.logpoints;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.util.ListF;
import anana5.util.Maybe;
import anana5.util.PList;
import anana5.util.Promise;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Matcher implements Callable<Collection<State>> {
    private static final Logger log = LoggerFactory.getLogger(Match.class);

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints match").build().defaultHelp(true)
                .description("match logs against logpoints");

        parser.addArgument("address").type(String.class);

        parser.addArgument("input").nargs("?").type(FileInputStream.class).setDefault(System.in);

        // parser.addArgument("output")
        //     .nargs("?")
        //     .type(PrintStream.class)
        //     .setDefault(System.out);

        parser.addArgument("--pattern")
                .setDefault("^(?<m>[\\w<>$.]+)\\((?<s>[\\/\\w.]*)\\:(?<l>\\d+)\\)");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.printHelp();
            return;
        }

        // traverse graph
        try (var in = ns.<InputStream>get("input");
                var graph = RemoteGraph.connect(ns.get("address"))) {
            // create a plist of all logging statements
            var reader = new BufferedReader(new InputStreamReader(in));
            final var lines = PList.<Integer, Line>unfold(0, i -> {
                return Promise.lazy(() -> {
                    try {
                        var line = reader.readLine();
                        if (line == null) {
                            return ListF.nil();
                        }
                        return ListF.cons(new Line(i, line), i + 1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            });

            var matcher =
                    new Matcher(graph, graph.roots(), lines, Pattern.compile(ns.get("pattern")));

            matcher.call();

            // var paths = heads.stream().map(Match::path).collect(Collectors.toList());

            // for (var path : paths) {
            //     System.out.println(path);
            // }

        } catch (EOFException e) {
            log.info("connection closed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    final Graph<SerializedVertex> graph;
    final Collection<SerializedVertex> roots;
    final Pattern pattern;

    final PList<Line> lines;
    final int MAX_RECURSION = 0;
    final int MAX_PATHS = 10;
    final int MAX_QUEUE_SIZE = 10_000;
    final int MAX_HEADS = 3000;
    final int LOOK_AHEAD = 10;

    private Matcher(Graph<SerializedVertex> graph, Collection<SerializedVertex> roots, PList<Line> lines,
            Pattern pattern) throws IOException {
        this.graph = graph;
        this.roots = roots;
        this.lines = lines;
        this.pattern = pattern;
    }

    public Collection<State> call() throws IOException {
        // main loop
        final Collection<State> out = new ArrayList<>();

        List<Head> heads = roots.stream().map(v -> new Head(this, v)).collect(Collectors.toCollection(ArrayList::new));

        SearchTree search = new SearchTree(new State(heads, lines));

        //TODO: run the search

        // final Queue<StateUpdate> queue = MinMaxPriorityQueue.maximumSize(MAX_QUEUE_SIZE).create();

        // MultiHeadState root = new MultiSerialHeadState();

        // queue.add(new StateUpdate(root, lines));

        // while (!queue.isEmpty() && out.size() < MAX_PATHS) {
        //     StateUpdate update = queue.poll();
        //     if (update.done()) {
        //         MultiHeadState state = update.state();
        //         out.add(state);
        //         for (PList<Match> path : state.paths()) {
        //             var list = path.map(Match::serial)
        //                     .collect(Collectors.toCollection(ArrayList::new)).join();
        //             Collections.reverse(list);
        //             System.out.println(list);
        //         }
        //         System.out.println();
        //         continue;
        //     }

        //     var succs = update.succs();

        //     queue.addAll(succs);
        // }

        return out;
    }

    private Set<SerializedVertex> flatten(Collection<SerializedVertex> matchers, int rec) {
        Set<SerializedVertex> out = new HashSet<>();
        for (SerializedVertex matcher : matchers) {
            if (matcher.sentinel()) {
                if (rec == 0) {
                    continue;
                }
                out.addAll(flatten(graph.from(matcher), rec - 1));
            } else {
                out.add(matcher);
            }
        }

        return out;
    }

    Maybe<Match> match(SerializedVertex serial, Line line) {
        SourceMapTag smap = serial.tag();
        var matcher = pattern.matcher(line.get());

        if (!matcher.find()) {
            return Maybe.nothing();
        }

        var methodName = matcher.group("m");
        var sourceFile = matcher.group("s");
        var lineNumber = Integer.valueOf(matcher.group("l")).intValue();

        if (smap.method().equals(methodName) && smap.source().equals(sourceFile)
                && smap.line() == lineNumber) {
            return Maybe.just(new Match(serial, line));
        } else {
            return Maybe.nothing();
        }
    }

    PList<Line> eat(PList<Line> lines) {
        Line line;
        while (!lines.empty().join()
                && !pattern.matcher((line = lines.head().join()).get()).find()) {
            log.warn("discarding line {}", line.index() + 1);
            lines = lines.tail();
        }
        return lines;
    }

    // class RootSerialHead implements Head {
    //     final Collection<SerialRef> roots;

    //     RootSerialHead(Collection<SerialRef> roots) {
    //         this.roots = roots;
    //     }

    //     public State accept(String line) {
    //         final var lines = eat(lines());
    //         if (lines.empty().join()) {
    //             return Collections.emptyList();
    //         }
    //         return roots().stream().map(serial -> new SerialHead(roots(), this, recursion, seq(), serial, lines)).collect(Collectors.toList());
    //     }

    //     @Override
    //     public String toString() {
    //         return String.format("root ~ %d", seq());
    //     }

    //     @Override
    //     public boolean equals(Object obj) {
    //         if (obj == null) {
    //             return false;
    //         }
    //         if (this == obj) {
    //             return true;
    //         }
    //         if (getClass() != obj.getClass()) {
    //             return false;
    //         }
    //         RootSerialHead other = (RootSerialHead) obj;
    //         return roots.equals(other.roots);
    //     }

    //     @Override
    //     public int hashCode() {
    //         return Objects.hash(roots);
    //     }
    // }

    // class SerialHead extends BaseSerialHead {
    //     private final SerialRef serial;
    //     private final int recursion;

    //     SerialHead(Collection<SerialRef> roots, Head prev, int recursion, int sequence, SerialRef serial, PList<String> lines) {
    //         super(roots, prev, sequence, lines);
    //         this.serial = serial;
    //         this.recursion = recursion;
    //     }

    //     public List<Head> next() {
    //         var lines = lines();
    //         if (lines.empty().join()) {
    //             return Collections.emptyList();
    //         }

    //         if (serial.returns()) {
    //             return Collections.singletonList(new RootSerialHead(roots(), recursion, seq(), lines));
    //         }

    //         var line = lines.head().join();

    //         if (!serial.sentinel() && !match(line)) {
    //             return Collections.emptyList();
    //         }

    //         if (recursion == 0 && serial.sentinel()) {
    //             return Collections.emptyList();
    //         }

    //         final var future = CompletableFuture.supplyAsync(() -> graph.from(serial));
    //         try {
    //             final var serials = future.get(10, TimeUnit.MINUTES);
    //             var next = eat(lines.tail());

    //             if (serials.isEmpty()) {
    //                 // dunno what happened here
    //                 return Collections.singletonList(new StopSerialHead(roots(), this, seq() + 1, next));
    //             } else if (serial.sentinel()) {
    //                 // match with the neighbours of the sentinel
    //                 return serials.stream().map(s -> new SerialHead(roots(), this, recursion - 1, seq(), s, lines)).collect(Collectors.toList());
    //             } else {
    //                 // match next with neighbours
    //                 return serials.stream().map(s -> new SerialHead(roots(), this, recursion, seq() + 1, s, next)).collect(Collectors.toList());
    //             }
    //         } catch (TimeoutException e) {
    //             log.error("timeout while getting neighbors of {}", serial);
    //             future.cancel(true);
    //             return Collections.emptyList();
    //         } catch (InterruptedException | ExecutionException e) {
    //             log.error("error while getting neighbors of {}", serial);
    //             e.printStackTrace();
    //             return Collections.emptyList();
    //         }
    //     }

    //     @Override
    //     public boolean done() {
    //         return serial.returns();
    //     }

    //     @Override
    //     public boolean sentinel() {
    //         return this.serial.sentinel();
    //     }

    //     @Override
    //     public String toString() {
    //         return String.format("%s ~ %d", serial, seq());
    //     }

    //     @Override
    //     public boolean equals(Object obj) {
    //         if (obj == null) {
    //             return false;
    //         }
    //         if (this == obj) {
    //             return true;
    //         }
    //         if (getClass() != obj.getClass()) {
    //             return false;
    //         }
    //         SerialHead other = (SerialHead) obj;
    //         return serial.equals(other.serial) && roots().equals(other.roots()) && seq() == other.seq() && previous().equals(other.previous());
    //     }

    //     @Override
    //     public int hashCode() {
    //         return Objects.hash(serial, roots(), seq(), previous());
    //     }
    // }

    // class StopSerialHead extends BaseSerialHead {

    //     public StopSerialHead(Collection<SerialRef> roots, Head previous, int sequence, PList<String> lines) {
    //         super(roots, previous, sequence, lines);
    //     }

    //     public Collection<Head> next() {
    //         return Collections.emptyList();
    //     }

    //     @Override
    //     public boolean done() {
    //         return true;
    //         // return true;
    //     }

    //     @Override
    //     public boolean sentinel() {
    //         return false;
    //     }

    //     @Override
    //     public String toString() {
    //         return String.format("nil ~ %d", seq());
    //     }

    //     @Override
    //     public boolean equals(Object obj) {
    //         if (obj == null) {
    //             return false;
    //         }
    //         if (this == obj) {
    //             return true;
    //         }
    //         if (getClass() != obj.getClass()) {
    //             return false;
    //         }
    //         StopSerialHead other = (StopSerialHead) obj;
    //         return roots().equals(other.roots()) && seq() == other.seq() && previous().equals(other.previous());
    //     }

    //     @Override
    //     public int hashCode() {
    //         return Objects.hash(roots(), seq(), previous());
    //     }

    // }

    // final class StateUpdate implements Comparable<StateUpdate> {
    //     private final MultiHeadState state;
    //     private final PList<Line> lines;
    //     // private final int seq;
    //     // private final int rec;

    //     StateUpdate(MultiHeadState state, PList<Line> lines) {
    //         this.state = state;
    //         this.lines = eat(lines);
    //         // this.seq = 0;
    //         // this.rec = 0;
    //     }

    //     // StateUpdate(int seq, State state, PList<Line> lines) {
    //     //     this.state = state;
    //     //     this.lines = lines;
    //     //     this.seq = seq;
    //     //     // this.rec = rec;
    //     // }

    //     public boolean done() {
    //         return this.lines.empty().join();
    //     }

    //     public Collection<StateUpdate> succs() {
    //         if (done()) {
    //             return Collections.emptyList();
    //         }

    //         var line = lines.head().join();
    //         var tail = lines.tail();

    //         Collection<StateUpdate> out = new ArrayList<>();

    //         // apply and queue next state updates
    //         for (MultiHeadState state : this.state.apply(line)) {
    //             out.add(new StateUpdate(state, tail));
    //         }

    //         return out;
    //     }

    //     public MultiHeadState state() {
    //         return state;
    //     }

    //     @Override
    //     public int compareTo(StateUpdate o) {
    //         if (done()) {
    //             if (o.done()) {
    //                 return 0;
    //             } else {
    //                 return -1;
    //             }
    //         }
    //         if (o.done()) {
    //             return 1;
    //         }

    //         int c = 0;

    //         // c = Integer.compare(o.lines.head().join().index() - o.state.skips().size(), this.lines.head().join().index() - this.state.skips().size());
    //         // if (c != 0) {
    //         //     return c;
    //         // }

    //         c = Integer.compare(this.state.skips().size() / LOOK_AHEAD,
    //                 o.state.skips().size() / LOOK_AHEAD);
    //         if (c != 0) {
    //             return c;
    //         }

    //         c = Integer.compare(this.state.paths().size(), o.state.paths().size());
    //         if (c != 0) {
    //             return c;
    //         }

    //         // final var thisMaxLength = this.state.paths().stream().map(matches -> matches.collect(Collectors.counting()).join()).collect(Collectors.maxBy(Long::compare));
    //         // if (thisMaxLength.isPresent()) {
    //         //     final var oMaxLength = o.state.paths().stream().map(matches -> matches.collect(Collectors.counting()).join()).collect(Collectors.maxBy(Long::compare));
    //         //     if (oMaxLength.isPresent()) {
    //         //         c = Long.compare(oMaxLength.get(), thisMaxLength.get());
    //         //         if (c != 0) {
    //         //             return c;
    //         //         }
    //         //     }
    //         // }

    //         c = Integer.compare(o.lines.head().join().index(), this.lines.head().join().index());
    //         if (c != 0) {
    //             return c;
    //         }

    //         // c = Integer.compare(this.rec, o.rec);
    //         // if (c != 0) {
    //         //     return c;
    //         // }

    //         // c = Integer.compare(this.seq, o.seq);
    //         // if (c != 0) {
    //         //     return c;
    //         // }

    //         return c;
    //     }
    // }

    // private Promise<Collection<State>> run(State state, PList<Line> lines, int rec) {
    //     Line line = lines.head().join();

    //     Collection<State> out = new ArrayList<>();

    //     for (State nextState : state.apply(line)) {
    //         out.addAll(run(state, lines.tail(), rec + 1));
    //     }
    // }
}
