package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

public class DotPrinter implements AutoCloseable {
    private final Map<Box.SerialRef, String> discovered;
    private final PrintStream out;

    DotPrinter(PrintStream out) {
        this.discovered = new HashMap<>();
        this.out = out;
        this.out.println("digraph {");
        this.out.println("    edge [style=bold]");
        this.out.println("    node [shape=box, style=\"rounded,bold\", fontname=\"helvetica\"]");
    }
    
    public String discover(Box.SerialRef vertex) {
        if (vertex == null) {
            return "root";
        }
        if (discovered.containsKey(vertex)) {
            return discovered.get(vertex);
        }
        String id = "\"" + new String(Base64.getEncoder().encode(vertex.hash())) + "\"";
        discovered.put(vertex, id);
        out.println("    " + String.format("%s [label=\"%s\"]", id, StringEscapeUtils.escapeJava(vertex.toString())));
        return id;
    }

    public void print(Box.SerialRef from, Box.SerialRef to) {
        StringBuilder s = new StringBuilder("    ");
        s.append(discover(from));
        s.append(" -> ");
        s.append(discover(to));
        out.println(s);
    }

    @Override
    public void close() {
        out.println("}");
    }
}
