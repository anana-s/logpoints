package anana5.sense.logpoints;

import java.nio.ByteBuffer;
import java.util.Arrays;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;

public class Box implements anana5.util.Ref<SootMethod> {
    private static int probe = 0;

    private final SootMethod method;

    private static byte[] probe() {
        return ByteBuffer.allocate(4).putInt(probe++).array();
    }

    public Box(SootMethod method) {
        this.method = method;
    }

    @Override
    public SootMethod get() {
        return method;
    }

    public Ref of(Stmt value) {
        return new SimpleRef(this, value, false);
    }

    public Ref of(Stmt value, boolean recursive) {
        return new SimpleRef(this, value, recursive);
    }

    public Ref of(Ref other) {
        return other.copy(this);
    }


    private static Stmt retStmt = new JReturnVoidStmt();
    private static Ref retRef = new ReturnRef();
    public static Ref returns() {
        return retRef;
    }

    @Deprecated
    public static Ref sentinel(Stmt stmt, boolean recursive) {
        return new SentinelRef(recursive);
    }

    public static Ref sentinel(boolean recursive) {
        return new SentinelRef(recursive);
    }

    public interface Ref extends anana5.util.Ref<Stmt> {
        byte[] hash();
        boolean returns();
        boolean recursive();
        Ref copy(Box box);
        default boolean sentinel() {
            return !check();
        }
    }

    private static abstract class BoxRef implements Ref {
        private final byte[] hash;

        private BoxRef() {
            this.hash = probe();
        }

        @Override
        public byte[] hash() {
            return this.hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            BoxRef other = (BoxRef)obj;
            return Arrays.equals(hash, other.hash);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash);
        }
    }

    private static class SentinelRef extends BoxRef {
        private final boolean recursive;

        private SentinelRef(boolean recursive) {
            this.recursive = recursive;
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
        public boolean recursive() {
            return recursive;
        }

        @Override
        public Ref copy(Box box) {
            return new SentinelRef(recursive);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static class SimpleRef extends BoxRef {
        private final Stmt stmt;
        private final boolean recursive;
        private final Box box;

        private SimpleRef(Box box, Stmt stmt, boolean recursive) {
            if (stmt == null) {
                throw new NullPointerException("stmt must not be null");
            }

            if (!stmt.hasTag("SourceMapTag")) {
                throw new IllegalArgumentException("stmt must have SourceMapTag");
            }
            if (!stmt.containsInvokeExpr()) {
                throw new IllegalArgumentException("stmt must contain InvokeExpr");
            }
            this.box = box;
            this.stmt = stmt;
            this.recursive = recursive;
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        @Override
        public boolean returns() {
            return LogPoints.isReturn(stmt);
        }

        @Override
        public boolean recursive() {
            return recursive;
        }

        @Override
        public String toString() {
            return format(stmt);
        }

        @Override
        public Ref copy(Box box) {
            return new SimpleRef(this.box, stmt, recursive || this.box.equals(box));
        }
    }

    private static class ReturnRef extends BoxRef {
        @Override
        public Stmt get() {
            return retStmt;
        }

        @Override
        public boolean returns() {
            return true;
        }

        @Override
        public boolean recursive() {
            return false;
        }

        @Override
        public String toString() {
            return "return";
        }

        @Override
        public Ref copy(Box box) {
            return Box.returns();
        }
    }

    protected static String format(Stmt stmt) {
        if (stmt == null || !stmt.containsInvokeExpr()) {
            return stmt.toString();
        }
        var expr = stmt.getInvokeExpr();
        var mref = expr.getMethodRef();
        return mref.getName() + expr.getArgs().toString() + stmt.getTag("SourceMapTag").toString();
    }
}
