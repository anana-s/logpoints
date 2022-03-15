package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class DotPrinter implements AutoCloseable {
    Set<Vertex<Stmt>> discovered;
    PrintStream out;
    Function<Vertex<Stmt>, String> formatter;

    DotPrinter(PrintStream out, Function<Vertex<Stmt>, String> formatter) {
        this.discovered = new HashSet<>();
        this.out = out;
        this.out.println("digraph {");
        this.out.println("    edge [style=bold]");
        this.out.println("    node [shape=box, style=\"rounded,bold\", fontname=\"helvetica\"]");
        this.formatter = formatter;
    }
    
    public String discover(Vertex<Stmt> vertex) {
        if (vertex == null) {
            return "root";
        }
        String id = String.format("nx%08x", vertex.hashCode());
        if (discovered.contains(vertex)) {
            return String.format("nx%08x", vertex.hashCode());
        }
        discovered.add(vertex);
        out.println("    " + String.format("%s [label=\"%s\"]", id, StringEscapeUtils.escapeJava(formatter.apply(vertex))));
        return id;
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
