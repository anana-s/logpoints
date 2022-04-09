package anana5.sense.logpoints;

public class StmtVertexFormatter {
    static String format(Box.Ref ref) {
        var stmt = ref.get();

        if (stmt == null || !stmt.containsInvokeExpr()) {
            return ref.toString();
        }
        var expr = stmt.getInvokeExpr();
        var mref = expr.getMethodRef();
        return mref.getName() + expr.getArgs().toString() + stmt.getTag("SourceMapTag").toString();
    }
}
