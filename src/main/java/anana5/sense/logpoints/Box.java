package anana5.sense.logpoints;

import java.util.Objects;

import anana5.graph.Vertex;

public class Box<T> {

    public abstract class Ref implements Vertex<T> {
        private final boolean sentinel;

        private Ref(boolean sentinel) {
            this.sentinel = sentinel;
        }

        public Box<T> box() {
            return Box.this;
        }

        public boolean sentinel() {
            return sentinel;
        }

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

        private SimpleRef(T value, boolean sentinel) {
            super(sentinel);
            this.value = value;
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

    private class CopyRef extends Ref {
        private final Ref ref;

        private CopyRef(Ref ref) {
            super(ref.sentinel());
            this.ref = ref;
        }

        @Override
        public T value() {
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
            if (!(obj instanceof Box<?>.CopyRef)) {
                return false;
            }
            Box<?>.CopyRef other = (Box<?>.CopyRef) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(ref, other.ref);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), ref);
        }
    }

    public Ref of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new SimpleRef(value, false);
    }

    public Ref of(boolean sentinel, T value) {
        if (sentinel == false && value == null) {
            throw new NullPointerException();
        }
        return new SimpleRef(value, sentinel);
    }

    public Ref copy(Ref other) {
        if (other.box() == this) {
            return other;
        }
        return new CopyRef(other);
    }
}
