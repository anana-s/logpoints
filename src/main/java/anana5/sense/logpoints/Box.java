package anana5.sense.logpoints;

import java.util.Objects;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class Box implements Vertex<Stmt> {
    private final Stmt stmt;
    private final int id;
    private static int probe = 0;

    public Box(Stmt stmt) {
        this.stmt = stmt;
        this.id = probe++;
    }

    @Override
    public Stmt value() {
        return stmt;
    }

    @Override
    final public int id() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof Box && ((Box) obj).id == this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
