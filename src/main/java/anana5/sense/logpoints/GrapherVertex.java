package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import anana5.util.Counter;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;

public interface GrapherVertex extends anana5.util.Ref<Stmt> {
    boolean returns();

    long id();

    GrapherVertex copy();

    default boolean sentinel() {
        return !check();
    }

    List<String> args();

    SourceMapTag tag();

    static Counter counter = new Counter();

    public static GrapherVertex of(Stmt stmt) {
        if (stmt == null) {
            return of();
        } else if (Grapher.isReturn(stmt)) {
            return ret();
        } else {
            return new SimpleBox(counter.probe(), stmt);
        }
    }

    public static GrapherVertex of(GrapherVertex other) {
        return other.copy();
    }

    public static GrapherVertex of() {
        return SentinelBox.instance;
    }

    public static GrapherVertex ret() {
        return ReturnBox.instance;
    }

    public static class ReturnBox implements GrapherVertex {
        private static final long id = counter.probe();
        private static final Stmt stmt = new JReturnVoidStmt();
        private static final ReturnBox instance = new ReturnBox();

        @Override
        public long id() {
            return id;
        }

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
        public GrapherVertex copy() {
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

    public static class SentinelBox implements GrapherVertex {
        private static final long id = counter.probe();
        private static final SentinelBox instance = new SentinelBox();

        @Override
        public long id() {
            return id;
        }

        @Override
        public Stmt get() {
            return null;
        }

        @Override
        public boolean returns() {
            return false;
        }

        @Override
        public GrapherVertex copy() {
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

    public static class SimpleBox implements GrapherVertex {
        private final long id;
        private final Stmt stmt;

        private SimpleBox(long id, Stmt stmt) {
            if (stmt == null) {
                throw new NullPointerException("stmt must not be null");
            }

            if (!stmt.hasTag("SourceMapTag")) {
                throw new IllegalArgumentException("stmt must have SourceMapTag");
            }

            if (!stmt.containsInvokeExpr()) {
                throw new IllegalArgumentException("stmt must invoke");
            }
            this.id = id;
            this.stmt = stmt;
        }

        @Override
        public long id() {
            return id;
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
        public GrapherVertex copy() {
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
