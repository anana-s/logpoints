package anana5.sense.logpoints;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.text.StringEscapeUtils;

public class GraphVizPrinter extends GraphPrinter implements AutoCloseable {

    PrintStream out;

    GraphVizPrinter(PrintStream out) throws FileNotFoundException {
        this.out = out;
        this.out.println("digraph {");
    }
    
    @Override
    public String discover(Object o) {
        if (discovered.containsKey(o)) {
            return discovered.get(o);
        }
        String repr = "\"" + StringEscapeUtils.escapeJava(o.toString() + " @ " + o.hashCode()) + "\"";
        discovered.put(o, repr);
        return repr;
    }

    @Override
    public void print(Object from, Object to) {
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
