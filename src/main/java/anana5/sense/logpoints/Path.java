package anana5.sense.logpoints;

import anana5.util.ListF;
import soot.SootMethod;
import soot.jimple.Stmt;

public class Path {
    private final ListF<Frame, Path> unfix;
    private final int length;

    private Path(ListF<Frame, Path> unfix) {
        this.unfix = unfix;
        this.length = unfix.match(() -> 0, (x, xs) -> 1 + xs.length());
    }

    public Path() {
        this(ListF.nil());
    }

    public Path(SootMethod method) {
        this(ListF.cons(new Frame(null, method, new Box()), new Path()));
    }

    public Path push(Stmt invoker, SootMethod method) {
        return new Path(ListF.cons(frame(invoker, method), this));
    }

    public Frame frame(Stmt invoker, SootMethod method) {
        return unfix.match(() -> new Frame(invoker, method, new Box()), (frame, path) -> {
            if (method.equals(frame.method())) {
                return new Frame(invoker, method, frame.box());
            }
            return path.frame(invoker, method);
        });
    }

    public Box box() {
        return unfix.match(() -> null, (a, f) -> a.box());
    }

    public int length() {
        return length;
    }

    public Frame head() {
        return unfix.match(() -> null, (a, f) -> a);
    }

    public Path tail() {
        return unfix.match(() -> null, (a, f) -> f);
    }

    public boolean contains(SootMethod method) {
        return unfix.match(() -> false, (a, f) -> a.method().equals(method) || f.contains(method));
    }

    public boolean contains(Stmt invoker, SootMethod method) {
        return unfix.match(() -> false, (a, f) -> (invoker.equals(a.invoker()) && method.equals(a.method())) || f.contains(invoker, method));
    }
}
