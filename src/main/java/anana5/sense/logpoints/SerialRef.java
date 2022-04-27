package anana5.sense.logpoints;

import java.io.Serializable;
import java.util.Arrays;

import anana5.sense.logpoints.Box.Ref;
import soot.jimple.Stmt;

public class SerialRef implements anana5.sense.logpoints.Box.Ref, Serializable {
    private final byte[] hash;

    private final boolean recursive;
    private final boolean returns;
    private final boolean sentinel;

    private final String methodName;
    private final String sourceFile;
    private final int  lineNumber;

    private final String name;
    private final String[] args;

    public SerialRef(Box.Ref ref) {
        this.hash = ref.hash();

        this.recursive = ref.recursive();
        this.returns = ref.returns();
        this.sentinel = ref.sentinel();

        if (returns || sentinel) {
            this.methodName = "";
            this.sourceFile = "";
            this.lineNumber = 0;
            this.name = ref.toString();
            this.args = new String[0];
        } else if (ref.check()) {
            Stmt stmt = ref.get();

            SourceMapTag tag = (SourceMapTag)stmt.getTag("SourceMapTag");

            this.methodName = tag.method();
            this.sourceFile = tag.source();
            this.lineNumber = tag.line();

            var expr = stmt.getInvokeExpr();
            var mref = expr.getMethodRef();

            this.name = mref.getName();
            this.args = expr.getArgs().stream().map(arg -> arg.toString()).toArray(String[]::new);
        } else {
            throw new IllegalStateException("null ref does not return and is not a sentinel");
        }
    }

    public SerialRef(byte[] hash, String name, String[] args, boolean recursive, boolean returns, boolean sentinel, String methodName, String sourceFile, int lineNumber) {
        this.hash = hash;
        this.name = name;
        this.args = args;
        this.recursive = recursive;
        this.returns = returns;
        this.sentinel = sentinel;
        this.methodName = methodName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public byte[] hash() {
        return hash;
    }

    @Override
    public String toString() {
        if (returns) {
            return "return";
        }
        if (sentinel) {
            return "$";
        }
        return String.format("%s[%s]%s(%s:%d)", name, String.join(",", args), methodName, sourceFile, lineNumber);
    }

    public boolean recursive() {
        return recursive;
    }

    public boolean returns() {
        return returns;
    }

    public boolean sentinel() {
        return sentinel;
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
        return null;
    }

    public String method() {
        return methodName;
    }

    public String source() {
        return sourceFile;
    }

    public int line() {
        return lineNumber;
    }

    @Override
    public Ref copy(Box box) {
        return new SerialRef(hash, name, args, recursive, returns, sentinel, methodName, sourceFile, lineNumber);
    }
}
