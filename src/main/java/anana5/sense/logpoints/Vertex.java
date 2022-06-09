package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;

public interface Vertex extends anana5.util.Ref<Stmt> {
    boolean returns();

    Vertex copy();

    default boolean sentinel() {
        return !check();
    }

    List<String> args();

    SourceMapTag tag();

    public static Vertex of(Stmt stmt) {
        if (stmt == null) {
            return of();
        } else if (Grapher.isReturn(stmt)) {
            return ret();
        } else {
            return new SimpleBox(stmt);
        }
    }

    public static Vertex of(Vertex other) {
        return other.copy();
    }

    public static Vertex of() {
        return SentinelBox.instance;
    }

    public static Vertex ret() {
        return ReturnBox.instance;
    }

    public static class ReturnBox implements Vertex {
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
        public Vertex copy() {
            return this;
        }

        @Override
        public SourceMapTag tag() {
            return new SourceMapTag("", "", -1);
        }

        @Override
        public List<String> args() {
            return Collections.singletonList("return");
        }
    }

    public static class SentinelBox implements Vertex {
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
        public Vertex copy() {
            return this;
        }

        @Override
        public String toString() {
            return "sentinel";
        }

        @Override
        public SourceMapTag tag() {
            return new SourceMapTag("", "", -1);
        }

        @Override
        public List<String> args() {
            return Collections.singletonList("sentinel");
        }
    }

    public static class SimpleBox implements Vertex {
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
        public SourceMapTag tag() {
            SourceMapTag tag = (SourceMapTag)stmt.getTag("SourceMapTag");
            return tag;
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
        public Vertex copy() {
            return this;
        }

        @Override
        public List<String> args() {
            var out = new ArrayList<String>();
            out.add(stmt.getInvokeExpr().getMethodRef().getName());
            for (var arg : stmt.getInvokeExpr().getArgs()) {
                out.add(arg.toString());
            }
            return out;
        }
    }
}
