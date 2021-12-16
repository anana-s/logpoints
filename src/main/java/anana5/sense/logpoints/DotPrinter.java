package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

import anana5.sense.graph.Rainfall.Droplet;
import soot.jimple.Stmt;

public class DotPrinter implements AutoCloseable {
    Map<Droplet<Stmt, ?>.SnowFlake, String> discovered;
    PrintStream out;
    Function<Droplet<Stmt, ?>.SnowFlake, String> formatter;

    DotPrinter(PrintStream out, Function<Droplet<Stmt, ?>.SnowFlake, String> formatter) {
        this.discovered = new HashMap<>();
        this.out = out;
        this.out.println("digraph {");
        this.formatter = formatter;
    }
    
    public String discover(Droplet<Stmt, ?>.SnowFlake o) {
        if (o == null) {
            return "root";
        }
        if (discovered.containsKey(o)) {
            return discovered.get(o);
        }
        String repr = "\"" + StringEscapeUtils.escapeJava(formatter.apply(o)) + "\"";
        discovered.put(o, repr);
        return repr;
    }

    public void print(Droplet<Stmt, ?>.SnowFlake from, Droplet<Stmt, ?>.SnowFlake to) {
        StringBuilder s = new StringBuilder();
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
