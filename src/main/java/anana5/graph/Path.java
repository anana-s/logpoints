package anana5.graph;

import java.util.function.Function;

public class Path<A, B> {
    Path<A, B> prev;
    A ref;
    B tgt;
    public Path() {
        this(null, null, null);
    }
    public Path(Path<A, B> prev, A ref, B target) {
        this.prev = prev;
        this.ref = ref;
        this.tgt = target;
    }
    public Path<A, B> push(A ref, B target) {
        return new Path<>(this, ref, target);
    }
    public Path<A, B> push(A ref) {
        return new Path<>(this, ref, null);
    }
    public B push(A ref, Function<Path<A,B>, B> f) {
        Path<A, B> out = new Path<>(this, ref, null);
        return out.tgt = f.apply(out);
    }
    public B set(B target) {
        return tgt = target;
    }
    public boolean contains(Object ref) {
        if (this.ref != null && this.ref.equals(ref)) {
            return true;
        } else if (prev == null) {
            return false;
        } else {
            return prev.contains(ref);
        }
    }
    public B find(A ref) {
        if (this.ref != null && this.ref.equals(ref)) {
            return tgt;
        } else if (prev == null) {
            return null;
        } else {
            return prev.find(ref);
        }
    }
    public B map(A ref, Function<Path<A, B>, B> f) {
        B out = find(ref);
        if (out == null) {
            return push(ref, f);
        }
        return out;
    }
}
