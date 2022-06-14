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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.Graph;
import anana5.sense.logpoints.SearchTree.Action;
import anana5.util.ListF;
import anana5.util.Maybe;
import anana5.util.PList;
import anana5.util.Promise;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import static java.lang.Math.*;

public class Matcher implements Callable<Collection<Matcher.State>> {
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
        try (var in = ns.<InputStream>get("input"); var graph = RemoteGraph.connect(ns.get("address"))) {
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

            var matcher = new Matcher(graph, graph.roots(), lines, Pattern.compile(ns.get("pattern")));

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

    final Graph<GrapherVertex> graph;
    final List<GrapherVertex> roots;
    final Pattern pattern;

    final SearchTree tree;

    final PList<Line> lines;
    final int MAX_RECURSION = 0;
    final int MAX_PATHS = 10;
    final int MAX_QUEUE_SIZE = 10_000;
    final int MAX_HEADS = 3000;
    final int LOOK_AHEAD = 10;

    private Matcher(Graph<GrapherVertex> graph, List<GrapherVertex> roots, PList<Line> lines,
            Pattern pattern) throws IOException {
        this.graph = graph;
        this.roots = roots;
        this.lines = lines;
        this.pattern = pattern;

        State root = new State(lines);

        tree = new SearchTree(root, 5, sqrt(2));
    }

    @Override
    public Collection<State> call() throws IOException {
        // main loop
        final Collection<State> out = new ArrayList<>();

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

    class State implements SearchTree.State {
        private PList<Line> lines;
        private final List<Group> groups;
        private final List<Line> skips;

        State(PList<Line> lines) {
            this.lines = lines;
            this.groups = new ArrayList<>();
            groups.add(new Group(roots));
            this.skips = new ArrayList<>();
        }

        private State(List<Group> groups, List<Line> skips, PList<Line> lines) {
            this.lines = lines;
            this.groups = new ArrayList<>(groups);
            this.skips = new ArrayList<>(skips);
        }

        @Override
        public List<Action> actions() {
            List<SearchTree.Action> actions = new ArrayList<>();
            OneshotAction.Context context = new OneshotAction.Context();
            actions.add(context.oneshot(new SkipAction()));

            for (int g = 0; g < groups.size(); g++) {
                Group group = groups.get(g);
                for (int h = 0; h < group.heads.size(); h++) {
                    Action action = context.oneshot(new NextStateAction(g, h));
                    actions.add(action);
                }
            }

            for (int r = 0; r < roots.size(); r++) {
                Action action = context.oneshot(new NewGroupAction(r));
                actions.add(action);
            }

            return actions;
        }

        class SkipAction implements SearchTree.Action {
            @Override
            public State apply() {
                skips.add(advance());
                return State.this;
            }

            @Override
            public double evaluate() {
                return -1;
            }

            @Override
            public SkipAction clone() {
                return State.this.clone().new SkipAction();
            }
        }

        class NextStateAction implements SearchTree.Action {
            private final int g, h;

            NextStateAction(int g, int h) {
                this.g = g;
                this.h = h;
            }

            @Override
            public State apply() {
                Line line = advance();

                Group group = groups.get(g);
                GrapherVertex head = group.heads.get(h);

                group.add(new Match(head, line));
                group.heads.clear();
                for (GrapherVertex v : graph.from(head)) {
                    group.heads.add(v);
                }

                return State.this;
            }

            @Override
            public double evaluate() {
                return 0;
            }

            @Override
            public NextStateAction clone() {
                return State.this.clone().new NextStateAction(g, h);
            }
        }

        class NewGroupAction implements SearchTree.Action {
            private final int r;

            NewGroupAction(int r) {
                this.r = r;
            }

            @Override
            public State apply() {
                Line line = advance();

                GrapherVertex head = roots.get(r);
                Collection<GrapherVertex> heads = graph.from(head);
                Group group = new Group(heads);
                group.add(new Match(head, line));
                groups.add(group);

                return State.this;
            }

            @Override
            public double evaluate() {
                return 0;
            }

            @Override
            public NewGroupAction clone() {
                return State.this.clone().new NewGroupAction(r);
            }
        }

        @Override
        public double evaluate() {
            return 0;
        }

        Line line() {
            return lines.head().join();
        }

        Line advance() {
            var line = lines.head().join();
            lines = lines.tail();
            return line;
        }

        public Collection<Group> groups() {
            return groups();
        }

        class Group {
            final List<Match> matches;
            final List<GrapherVertex> heads;

            Group(Collection<GrapherVertex> vertices) {
                this(Collections.emptyList(), vertices);
            }

            Group(List<Match> matches, Collection<GrapherVertex> heads) {
                this.matches = new ArrayList<>(matches);
                this.heads = new ArrayList<>(heads);
            }

            void add(Match match) {
                this.matches.add(match);
            }

            @Override
            public String toString() {
                return matches.toString();
            }

            @Override
            protected Group clone() {
                return new Group(matches, heads);
            }
        }

        public State clone() {
            return new State(groups.stream().map(Group::clone).collect(Collectors.toList()), skips, lines);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            State other = (State) obj;
            return Objects.equals(groups, other.groups) && Objects.equals(skips, other.skips) && Objects.equals(lines, other.lines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groups, skips, lines);
        }

    }

    // private Set<GrapherVertex> flatten(Collection<? extends GrapherVertex> matchers, int rec) {
    //     Set<GrapherVertex> out = new HashSet<>();
    //     for (GrapherVertex matcher : matchers) {
    //         if (matcher.sentinel()) {
    //             if (rec == 0) {
    //                 continue;
    //             }
    //             out.addAll(flatten(graph.from(matcher), rec - 1));
    //         } else {
    //             out.add(matcher);
    //         }
    //     }

    //     return out;
    // }

    Maybe<Match> match(GrapherVertex vertex, Line line) {
        SourceMapTag smap = vertex.tag();
        var matcher = pattern.matcher(line.get());

        if (!matcher.find()) {
            return Maybe.nothing();
        }

        var methodName = matcher.group("m");
        var sourceFile = matcher.group("s");
        var lineNumber = Integer.valueOf(matcher.group("l")).intValue();

        if (smap.method().equals(methodName) && smap.source().equals(sourceFile)
                && smap.line() == lineNumber) {
            return Maybe.just(new Match(vertex, line));
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
}
