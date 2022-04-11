package anana5.sense.logpoints;

import java.util.Arrays;

public class SerialRef implements anana5.util.Ref<String> {
    private final byte[] hash;
    private final String serial;
    private final boolean recursive;
    private final boolean isReturn;

    public SerialRef(Box.Ref ref) {
        this.hash = ref.hash();
        this.serial = ref.toString();
        this.recursive = ref.recursive();
        this.isReturn = LogPoints.isReturn(ref.get());
    }

    public byte[] hash() {
        return hash;
    }

    @Override
    public String get() {
        return serial;
    }

    @Override
    public String toString() {
        return serial;
    }

    public boolean recursive() {
        return recursive;
    }

    public boolean returns() {
        return isReturn;
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
}
