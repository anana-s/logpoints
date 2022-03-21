package anana5.sense.logpoints;

import java.util.Objects;

import anana5.graph.Vertex;

public class Box<T> {

    public abstract class Ref implements Vertex<T> {
        abstract Box<T> box();
        abstract boolean sentinel();

        @Override
        public String toString() {
            if (sentinel()) {
                return String.format("sentinel[%s]", value());
            }
            return String.format("[%s]", value());
        }
    }

    private class SimpleRef extends Ref {
        private final T value;
        private final boolean sentinel;

        private SimpleRef(T value, boolean sentinel) {
            this.value = value;
            this.sentinel = sentinel;
        }

        public Box<T> box() {
            return Box.this;
        }

        public boolean sentinel() {
            return sentinel;
        }

        @Override
        public T value() {
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
            if (!(obj instanceof Box<?>.SimpleRef)) {
                return false;
            }
            Box<?>.SimpleRef other = (Box<?>.SimpleRef) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), value);
        }
    }

    private class CopyRef extends SimpleRef {
        private final Ref ref;
        private final T context;

        private CopyRef(Ref ref, T context) {
            super(ref.value(), ref.sentinel());
            this.ref = ref;
            this.context = context;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Box<?>.CopyRef)) {
                return false;
            }
            Box<?>.CopyRef other = (Box<?>.CopyRef) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(context, other.context) && Objects.equals(ref, other.ref);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), ref, context);
        }
    }

    public Ref of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new SimpleRef(value, false);
    }

    public Ref sentinel(T value) {
        return new SimpleRef(value, true);
    }

    public Ref sentinel() {
        return new SimpleRef(null, true);
    }

    public Ref copy(Ref other, T context) {
        return new CopyRef(other, context);
    }
}
