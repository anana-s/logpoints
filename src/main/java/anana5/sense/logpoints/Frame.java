package anana5.sense.logpoints;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import anana5.util.LList;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Frame {
    Frame parent;
    ExceptionalUnitGraph cfg;
    Collection<Pointer> rets;

    public Frame(SootMethod entrypoint) {
        this(null, entrypoint, Collections.emptyList());
    }

    public Frame(Frame parent, SootMethod method, Collection<Pointer> rets) {
        this.parent = parent;
        this.cfg = new ExceptionalUnitGraph(method.getActiveBody());
        this.rets = rets;
    }

    public Frame push(SootMethod method, Collection<Pointer> rets) {
        return new Frame(this, method, rets);
    }

    public LList<Pointer> roots() {
        return new LList<>(cfg.getHeads()).map(unit -> new Pointer(this, null));
    }
}