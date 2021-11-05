package anana5.sense.graph.java;

import soot.SootMethod;
import soot.Unit;

public interface ICFGBuilder {
    public void add(Unit from, Unit to);
    public void add(SootMethod from, Unit to);
    public void add(Unit from, SootMethod to);
}
