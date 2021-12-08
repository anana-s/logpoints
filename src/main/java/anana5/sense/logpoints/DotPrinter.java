package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

public class DotPrinter<T> implements AutoCloseable {
    Map<T, String> discovered;
    PrintStream out;

    DotPrinter(PrintStream out) {
        this.discovered = new HashMap<>();
        this.out = out;
        this.out.println("digraph {");
    }
    
    public <O extends T> String discover(O o) {
        if (discovered.containsKey(o)) {
            return discovered.get(o);
        }
        String repr = "\"" + StringEscapeUtils.escapeJava(o.toString() + " at " + Integer.toHexString(o.hashCode())) + "\"";
        discovered.put(o, repr);
        return repr;
    }

    public <O extends T> void print(O from, O to) {
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
