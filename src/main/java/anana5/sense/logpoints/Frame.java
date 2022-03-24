package anana5.sense.logpoints;

import java.util.Objects;

import soot.SootMethod;
import soot.jimple.Stmt;

public class Frame {
    private final Stmt invoker;
    private final SootMethod method;
    private final Box box;

    public Frame(Stmt invoker, SootMethod method, Box box) {
        this.invoker = invoker;
        this.method = method;
        this.box = box;
    }

    public Stmt invoker() {
        return invoker;
    }

    public SootMethod method() {
        return method;
    }

    public Box box() {
        return box;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Frame)) {
            return false;
        }
        Frame other = (Frame)obj;
        return Objects.equals(invoker, other.invoker) && Objects.equals(method, other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoker, method);
    }

    @Override
    public String toString() {
        return "[" + method.getDeclaringClass().getName() + "." + method.getName() + "]@[" + hashCode() + "]";
    }
}
