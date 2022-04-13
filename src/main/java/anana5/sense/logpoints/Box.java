package anana5.sense.logpoints;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

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

    public static Ref returns() {
        return new ReturnRef();
    }

    public interface Ref extends anana5.util.Ref<Stmt> {
        boolean returns();
        boolean recursive();
        Ref copy(Box box);
        default boolean sentinel() {
            return recursive() || returns();
        }
    }

    private class CopyRef implements Ref {
        private final byte[] hash;
        private final Box base;
        private final Stmt stmt;
        private final boolean recursive;

        public CopyRef(Box base, Stmt stmt, boolean recursive) {
            this.base = base;
            this.stmt = stmt;
            this.recursive = recursive;
            this.hash = probe();
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
        public Ref copy(Box box) {
            return box.new CopyRef(base, get(), recursive() || base.equals(box));
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        @Override
        public byte[] hash() {
            return hash;
        }

        @Override
        public String toString() {
            return format(stmt);
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
            CopyRef other = (CopyRef) obj;
            return Arrays.equals(hash(), other.hash());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash());
        }
    }

    private class SimpleRef implements Ref {
        private final byte[] hash;
        private final Stmt stmt;

        private SimpleRef(Stmt stmt) {
            this.stmt = stmt;
            this.hash = probe();
        }

        @Override
        public byte[] hash() {
            return this.hash;
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        public boolean returns() {
            return LogPoints.isReturn(stmt);
        }

        public boolean recursive() {
            return false;
        }

        @Override
        public String toString() {
            return format(stmt);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            SimpleRef other = (SimpleRef) obj;
            return Arrays.equals(hash(), other.hash());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash());
        }

        @Override
        public Ref copy(Box box) {
            return box.new CopyRef(Box.this, get(), Box.this.equals(box));
        }
    }

    private static class ReturnRef implements Ref {
        private final byte[] hash;

        private ReturnRef() {
            this.hash = new byte[]{};
        }

        @Override
        public byte[] hash() {
            return this.hash;
        }

        @Override
        public Stmt get() {
            return new JReturnVoidStmt();
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
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != this.getClass()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash();
        }

        @Override
        public Ref copy(Box box) {
            return this;
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
