package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class DotPrinter implements AutoCloseable {
    Map<Box.Ref, String> discovered;
    PrintStream out;
    Function<Box.Ref, String> formatter;

    DotPrinter(PrintStream out, Function<Box.Ref, String> formatter) {
        this.discovered = new HashMap<>();
        this.out = out;
        this.out.println("digraph {");
        this.out.println("    edge [style=bold]");
        this.out.println("    node [shape=box, style=\"rounded,bold\", fontname=\"helvetica\"]");
        this.formatter = formatter;
    }
    
    public String discover(Box.Ref vertex) {
        if (vertex == null) {
            return "root";
        }
        if (discovered.containsKey(vertex)) {
            return discovered.get(vertex);
        }
        String id = "\"" + new String(Base64.getEncoder().encode(vertex.hash())) + "\"";
        discovered.put(vertex, id);
        out.println("    " + String.format("%s [label=\"%s\"]", id, StringEscapeUtils.escapeJava(formatter.apply(vertex))));
        return id;
    }

    public void print(Box.Ref from, Box.Ref to) {
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
