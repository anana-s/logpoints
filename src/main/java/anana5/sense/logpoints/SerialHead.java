package anana5.sense.logpoints;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import anana5.graph.Graph;
import anana5.util.PList;

class SerialHead extends BaseHead {
    private static final Pattern pattern = Pattern.compile("^(?<m>[^\\(\\s]*)\\((?<s>[^\\:\\s]*)\\:(?<l>\\d+)\\)");
    private final Graph<SerialRef> graph;
    private final SerialRef serial;

    SerialHead(Head prev, Graph<SerialRef> graph, SerialRef serial, PList<String> lines) {
        super(prev, lines);
        this.graph = graph;
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
            var serials = graph.from(serial);
            if (serials.isEmpty()) {
                return Collections.singletonList(new StopSerialHead(this, next));
            }
            return serials.stream().map(s -> new SerialHead(this, graph, s, next)).collect(Collectors.toList());
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