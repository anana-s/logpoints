package anana5.sense.logpoints;

import java.util.Objects;

import anana5.graph.Vertex;

public class Box<T> {
    public class Ref implements Vertex<T> {
        private final T value;
        private final boolean sentinel;

        private Ref(T value, boolean sentinel) {
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
            if (obj.getClass() != getClass()) {
                return false;
            }
            Box<?>.Ref other = (Box<?>.Ref) obj;
            return Objects.equals(box(), other.box()) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box(), value);
        }

        @Override
        public String toString() {
            if (sentinel) {
                return String.format("sentinel[%s]", value);
            }
            return String.format("[%s]", value);
        }

    }

    public Ref of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new Ref(value, false);
    }

    public Ref sentinel(T value) {
        return new Ref(value, true);
    }

    public Ref sentinel() {
        return new Ref(null, true);
    }

    public Ref copy(Ref other) {
        return new Ref(other.value(), other.sentinel());
    }
}
