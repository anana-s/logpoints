package anana5.sense.logpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import soot.jimple.Stmt;

public class SerializedVertex implements Serializable, GrapherVertex {
    private final long id;
    private final ArrayList<String> args;
    private final boolean returns;
    private final boolean sentinel;
    private final SourceMapTag tag;

    public static SerializedVertex serialize(GrapherVertex v) {
        if (v instanceof SerializedVertex) {
            return (SerializedVertex)v;
        }
        return new SerializedVertex(v.id(), v.args(), v.returns(), v.sentinel(), v.tag());
    }

    public SerializedVertex(long id, List<String> args, boolean returns, boolean sentinel, SourceMapTag tag) {
        this.id = id;
        this.args = new ArrayList<>(args);
        this.returns = returns;
        this.sentinel = sentinel;
        this.tag = tag;
    }

    public long id() {
        return id;
    }

    @Override
    public String toString() {
        String name = args.get(0);
        String rest = String.join(",", args.subList(1, args.size()));
        String smap = tag().toString();
        return String.format("%s %s(%s)", name, rest, smap);
    }

    public boolean returns() {
        return returns;
    }

    public boolean sentinel() {
        return sentinel;
    }

    @Override
    public Stmt get() {
        throw new UnsupportedOperationException("Stmt cannot be serialized");
    }

    @Override
    public GrapherVertex copy() {
        return new SerializedVertex(id, args, returns, sentinel, tag);
    }

    @Override
    public SourceMapTag tag() {
        return tag;
    }

    @Override
    public List<String> args() {
        return Collections.unmodifiableList(args);
    }
}
