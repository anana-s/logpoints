package anana5.sense.logpoints;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import soot.jimple.Stmt;

public class Box implements Serializable {
    private static int probe;

    private static byte[] probe() {
        return ByteBuffer.allocate(4).putInt(probe++).array();
    }

    public Ref of(Stmt value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new Ref(value, false);
    }

    public Ref of(Ref other) {
        if (this.equals(other.box())) {
            return new Ref(other.get(), true);
        }
        return new Ref(other.get(), other.recursive());
    }

    public class Ref implements anana5.util.Ref<Stmt> {
        private final byte[] hash;
        private final Stmt stmt;
        private final boolean recursive;

        private Ref(Stmt stmt, boolean recursive) {
            this.stmt = stmt;
            this.recursive = recursive;
            this.hash = probe();
        }

        public Box box() {
            return Box.this;
        }

        @Override
        public byte[] hash() {
            return this.hash;
        }

        @Override
        public Stmt get() {
            return stmt;
        }

        public boolean recursive() {
            return recursive;
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
            if (obj.getClass() != Box.Ref.class) {
                return false;
            }
            Box.Ref other = (Box.Ref) obj;
            return Arrays.equals(hash(), other.hash());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash());
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
