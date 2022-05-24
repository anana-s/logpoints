package anana5.sense.logpoints;

import java.io.Serializable;

import soot.jimple.Stmt;

public class StmtMatcher implements Serializable {
    private final long id;

    private final boolean returns;
    private final boolean sentinel;

    private final String methodName;
    private final String sourceFile;
    private final int  lineNumber;

    private final String name;
    private final String[] args;

    public StmtMatcher(long id, Box ref) {
        this.id = id;
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

    public StmtMatcher(long id, String name, String[] args, boolean returns, boolean sentinel, String methodName, String sourceFile, int lineNumber) {
        this.id = id;
        this.name = name;
        this.args = args;
        this.returns = returns;
        this.sentinel = sentinel;
        this.methodName = methodName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public long id() {
        return id;
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

    public boolean returns() {
        return returns;
    }

    public boolean sentinel() {
        return sentinel;
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
}
