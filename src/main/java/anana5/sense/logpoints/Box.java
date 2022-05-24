package anana5.sense.logpoints;

import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;

public interface Box extends anana5.util.Ref<Stmt> {

    boolean returns();
    Box copy();
    default boolean sentinel() {
        return !check();
    }

    public static Box of(Stmt stmt) {
        if (stmt == null) {
            return of();
        } else if (LogPoints.isReturn(stmt)) {
            return ret();
        } else {
            return new SimpleBox(stmt);
        }
    }

    public static Box of(Box other) {
        return other.copy();
    }

    public static Box of() {
        return SentinelBox.instance;
    }

    public static Box ret() {
        return ReturnBox.instance;
    }

    public static class ReturnBox implements Box {
        private static final Stmt stmt = new JReturnVoidStmt();
        private static final ReturnBox instance = new ReturnBox();

        @Override
        public Stmt get() {
            return stmt;
        }

        @Override
        public boolean returns() {
            return true;
        }

        @Override
        public String toString() {
            return "return";
        }

        @Override
        public Box copy() {
            return this;
        }
    }

    public static class SentinelBox implements Box {
        private static final SentinelBox instance = new SentinelBox();

        @Override
        public Stmt get() {
            return null;
        }

        @Override
        public boolean returns() {
            return false;
        }

        @Override
        public Box copy() {
            return this;
        }

        @Override
        public String toString() {
            return "sentinel";
        }
    }

    public static class SimpleBox implements Box {
        private final Stmt stmt;

        private SimpleBox(Stmt stmt) {
            if (stmt == null) {
                throw new NullPointerException("stmt must not be null");
            }

            if (!stmt.hasTag("SourceMapTag")) {
                throw new IllegalArgumentException("stmt must have SourceMapTag");
            }

            if (!stmt.containsInvokeExpr()) {
                throw new IllegalArgumentException("stmt must invoke");
            }
            this.stmt = stmt;
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        @Override
        public boolean returns() {
            return false;
        }

        @Override
        public String toString() {
            var expr = stmt.getInvokeExpr();
            var mref = expr.getMethodRef();
            return mref.getName() + expr.getArgs().toString() + stmt.getTag("SourceMapTag").toString();
        }

        @Override
        public Box copy() {
            return this;
        }
    }
}
