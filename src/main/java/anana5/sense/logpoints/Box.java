package anana5.sense.logpoints;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class Box {
    private static int probe;
    private final byte[] hash;

    public Box() {
        this.hash = ByteBuffer.allocate(4).putInt(probe++).array();
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
            this.hash = ByteBuffer.allocate(4).putInt(probe++).array();
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

        public abstract Stmt value();

        @Override
        public String toString() {
            var v = value();
            if (sentinel()) {
                return String.format("sentinel[%s]@[%d]", v, v.hashCode());
            }
            return String.format("[%s]@[%d]", v, v.hashCode());
        }
    }

    private class SimpleRef extends Ref {
        private final Stmt value;

        private SimpleRef(Stmt value, boolean sentinel) {
            super(sentinel);
            this.value = value;
        }

        @Override
        public Stmt value() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Box.SimpleRef)) {
                return false;
            }
            Box.SimpleRef other = (Box.SimpleRef) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), value);
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Box.CopyRef)) {
                return false;
            }
            Box.CopyRef other = (Box.CopyRef) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(ref, other.ref);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), ref);
        }
    }
}
