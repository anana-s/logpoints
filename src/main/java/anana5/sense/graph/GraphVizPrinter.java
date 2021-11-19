package anana5.sense.graph;

import org.apache.commons.text.StringEscapeUtils;

public class GraphVizPrinter extends GraphPrinter {
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
        System.out.println(s);
    }
}
