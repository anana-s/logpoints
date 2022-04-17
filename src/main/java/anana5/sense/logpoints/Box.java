package anana5.sense.logpoints;

import java.nio.ByteBuffer;
import java.util.Arrays;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;

public class Box implements anana5.util.Ref<SootMethod> {
    private static int probe = 0;

    private final byte[] hash;
    private final SootMethod method;

    private static byte[] probe() {
        return ByteBuffer.allocate(4).putInt(probe++).array();
    }

    public Box(SootMethod method) {
        this.hash = probe();
        this.method = method;
    }

    @Override
    public byte[] hash() {
        return hash;
    }

    @Override
    public SootMethod get() {
        return method;
    }

    public Ref of(Stmt value) {
        return new SimpleRef(value);
    }

    public Ref of(Ref other) {
        return other.copy(this);
    }


    private static Stmt retStmt = new JReturnVoidStmt();
    private static Ref retRef = new ReturnRef();
    public static Ref returns() {
        return retRef;
    }

    public static Ref sentinel(boolean recursive) {
        return new SentinelRef(recursive);
    }

    public interface Ref extends anana5.util.Ref<Stmt> {
        boolean returns();
        boolean recursive();
        Ref copy(Box box);
        boolean sentinel();
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

        public SentinelRef(boolean recursive) {
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
        public boolean sentinel() {
            return true;
        }

        @Override
        public Ref copy(Box box) {
            return Box.sentinel(recursive);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static class CopyRef extends BoxRef {
        private final Box base;
        private final Stmt stmt;
        private final boolean recursive;

        public CopyRef(Box base, Stmt stmt, boolean recursive) {
            this.base = base;
            this.stmt = stmt;
            this.recursive = recursive;
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
        public boolean sentinel() {
            return false;
        }

        @Override
        public Ref copy(Box box) {
            return new CopyRef(base, stmt, recursive() || base.equals(box));
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        @Override
        public String toString() {
            return format(stmt);
        }
    }

    private class SimpleRef extends BoxRef {
        private final Stmt stmt;

        private SimpleRef(Stmt stmt) {
            this.stmt = stmt;
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
            return false;
        }

        @Override
        public boolean sentinel() {
            return false;
        }

        @Override
        public String toString() {
            return format(stmt);
        }

        @Override
        public Ref copy(Box box) {
            return new CopyRef(Box.this, stmt, Box.this.equals(box));
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
        public boolean sentinel() {
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
