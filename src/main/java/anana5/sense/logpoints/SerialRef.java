package anana5.sense.logpoints;

import java.io.Serializable;
import java.util.Arrays;

import anana5.sense.logpoints.Box.Ref;
import soot.jimple.Stmt;

public class SerialRef implements anana5.sense.logpoints.Box.Ref, Serializable {
    private final byte[] hash;
    private final String serial;
    private final boolean recursive;
    private final boolean returns;

    public SerialRef(Box.Ref ref) {
        this.hash = ref.hash();
        this.serial = ref.toString();
        this.recursive = ref.recursive();
        this.returns = ref.returns();
    }

    public byte[] hash() {
        return hash;
    }

    @Override
    public String toString() {
        return serial;
    }

    public boolean recursive() {
        return recursive;
    }

    public boolean returns() {
        return returns;
    }

    public boolean sentinel() {
        return recursive() || returns();
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
        SerialRef other = (SerialRef) obj;
        return Arrays.equals(hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }

    @Override
    public Stmt get() {
        throw new UnsupportedOperationException("serial ref does contain the actual stmt");
    }

    @Override
    public Ref copy(Box box) {
        // TODO Auto-generated method stub
        return null;
    }
}
