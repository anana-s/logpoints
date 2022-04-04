package anana5.sense.logpoints;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

import soot.jimple.Stmt;

public class Box implements Serializable {
    private static int probe;
    private final byte[] hash;

    private static byte[] probe() {
        return ByteBuffer.allocate(4).putInt(probe++).array();
    }

    public Box() {
        this.hash = probe();
    }

    public Ref of(Stmt value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new SimpleRef(value, false);
    }

    public Ref of(boolean sentinel, Stmt value) {
        if (sentinel == false && value == null) {
            throw new NullPointerException();
        }
        return new SimpleRef(value, sentinel);
    }

    public Ref of(Ref other) {
        if (other.box() == this) {
            return other;
        }
        return new CopyRef(other);
    }

    public abstract class Ref {
        private final boolean sentinel;
        private final byte[] hash;

        private Ref(boolean sentinel) {
            this.sentinel = sentinel;
            this.hash = probe();
        }

        public Box box() {
            return Box.this;
        }

        public boolean sentinel() {
            return sentinel;
        }

        public byte[] hash() {
            return ArrayUtils.addAll(Box.this.hash, this.hash);
        }

        protected abstract Stmt value();

        @Override
        public String toString() {
            if (sentinel()) {
                return String.format("sentinel %s", format(value()));
            }
            return format(value());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Box.Ref)) {
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

    private class SimpleRef extends Ref {
        private final Stmt value;

        private SimpleRef(Stmt value, boolean sentinel) {
            super(sentinel);

            if (!value.hasTag("SourceMapTag")) {
                throw new IllegalArgumentException("value must have SourceMapTag");
            }

            this.value = value;
        }

        @Override
        public Stmt value() {
            return value;
        }
    }

    private class CopyRef extends Ref {
        private final Ref ref;

        private CopyRef(Ref ref) {
            super(ref.sentinel());
            this.ref = ref;
        }

        @Override
        public Stmt value() {
            return ref.value();
        }
    }
}
