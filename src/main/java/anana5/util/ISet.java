package anana5.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ISet<T> {
    private final Set<T> set;
    private ISet(Set<T> set) {
        this.set = set;
    }
    @SafeVarargs
    public static <T> ISet<T> of(T... ts) {
        return new ISet<>(new HashSet<>(Arrays.asList(ts)));
    }
    public ISet<T> add(T t) {
        var set = new HashSet<>(this.set);
        set.add(t);
        return new ISet<>(set);
    }
    public ISet<T> remove(T t) {
        var set = new HashSet<>(this.set);
        set.remove(t);
        return new ISet<>(set);
    }
    public boolean contains(T t) {
        return set.contains(t);
    }
    public static <T> ISet<T> intersect(ISet<T> a, ISet<T> b) {
        var set = new HashSet<>(a.set);
        set.retainAll(b.set);
        return new ISet<>(set);
    }
    public static <T> ISet<T> union(ISet<T> a, ISet<T> b) {
        var set = new HashSet<>(a.set);
        set.addAll(b.set);
        return new ISet<>(set);
    }
}
