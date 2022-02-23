package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class DotPrinter implements AutoCloseable {
    Map<Vertex<Stmt>, String> discovered;
    PrintStream out;
    Function<Vertex<Stmt>, String> formatter;

    DotPrinter(PrintStream out, Function<Vertex<Stmt>, String> formatter) {
        this.discovered = new HashMap<>();
        this.out = out;
        this.out.println("digraph {");
        this.out.println("    edge [style=bold]");
        this.out.println("    node [shape=box, style=\"rounded,bold\", fontname=\"helvetica\"]");
        this.formatter = formatter;
    }
    
    public String discover(Vertex<Stmt> o) {
        if (discovered.containsKey(o)) {
            return discovered.get(o);
        }
        String repr = "\"" + StringEscapeUtils.escapeJava(formatter.apply(o)) + "\"";
        discovered.put(o, repr);
        out.println("    " + repr);
        return repr;
    }

    public void print(Vertex<Stmt> from, Vertex<Stmt> to) {
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
