package anana5.sense.graph.java;

import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;

public class PrintICFGBuilder implements ICFGBuilder {
    Set<Object> discovered = new HashSet<>();

    public int discover(SootMethod m) {
        if (discovered.contains(m)) {
            return m.hashCode();
        }
        discovered.add(m);
        StringBuilder s = new StringBuilder("+ ");
        s.append(m.hashCode());
        s.append(" -> ");
        s.append(m.toString());
        System.out.println(s);
        return m.hashCode();
    }

    public int discover(Unit u) {
        if (discovered.contains(u)) {
            return u.hashCode();
        }
        discovered.add(u);
        StringBuilder s = new StringBuilder("+ ");
        s.append(u.hashCode());
        s.append(" -> ");
        s.append(u.toString());
        System.out.println(s);
        return u.hashCode();
    }

    public void add (Unit from, Unit to) {
        StringBuilder s = new StringBuilder("- ");
        s.append(discover(from));
        s.append(" -> ");
        s.append(discover(to));
        System.out.println(s);
    }

    public void add (SootMethod from, Unit to) {
        StringBuilder s = new StringBuilder("- ");
        s.append(discover(from));
        s.append(" -> ");
        s.append(discover(to));
        System.out.println(s);

    }

    public void add (Unit from, SootMethod to) {
        StringBuilder s = new StringBuilder("- ");
        s.append(discover(from));
        s.append(" -> ");
        s.append(discover(to));
        System.out.println(s);

    }
}
