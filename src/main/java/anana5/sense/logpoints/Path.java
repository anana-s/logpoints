package anana5.sense.logpoints;

import anana5.util.ListF;
import anana5.util.Tuple;
import soot.SootMethod;
import soot.jimple.Stmt;

public class Path {
    private final ListF<Tuple<SootMethod, Box<Stmt>>, Path> unfix;
    private final int length;

    private Path(ListF<Tuple<SootMethod, Box<Stmt>>, Path> unfix) {
        this.unfix = unfix;
        this.length = unfix.match(() -> 0, (x, xs) -> 1 + xs.length());
    }

    public Path() {
        this(ListF.nil());
    }

    public Path push(SootMethod method) {
        return new Path(ListF.cons(Tuple.of(method, box(method)), this));
    }

    public boolean contains(SootMethod method) {
        return unfix.match(() -> false, (a, f) -> a.fst().equals(method) || f.contains(method));
    }

    public Box<Stmt> box() {
        return unfix.match(() -> new Box<>(), (a, f) -> a.snd());
    }

    public Box<Stmt> box(SootMethod method) {
        return unfix.match(() -> new Box<>(), (a, f) -> {
            if (a.fst().equals(method)) {
                return a.snd();
            } else {
                return f.box(method);
            }
        });
    }

    public int length() {
        return length;
    }

    public SootMethod head() {
        return unfix.match(() -> null, (a, f) -> a.fst());
    }
}
