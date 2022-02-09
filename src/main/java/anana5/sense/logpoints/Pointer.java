package anana5.sense.logpoints;

import soot.jimple.Stmt;

public class Pointer {
    Frame ctx;
    Stmt stmt;

    Pointer(Frame context, Stmt stmt) {
        this.ctx = context;
        this.stmt = stmt;
    }
}
