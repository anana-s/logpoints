package anana5.sense.graph.java;

import java.util.HashMap;

public class GraphPrinter {
    HashMap<Object, String> discovered = new HashMap<>();

    public String discover(Object o) {
        if (discovered.containsKey(o)) {
            return discovered.get(o);
        }
        String repr = Integer.toString(o.hashCode());
        discovered.put(o, repr);
        StringBuilder s = new StringBuilder("+ ");
        s.append(repr);
        s.append(" -> ");
        s.append(o.toString());
        System.out.println(s);
        return repr;
    }

    public void print (Object from, Object to) {
        StringBuilder s = new StringBuilder("- ");
        s.append(discover(from));
        s.append(" -> ");
        s.append(discover(to));
        System.out.println(s);
    }
}
