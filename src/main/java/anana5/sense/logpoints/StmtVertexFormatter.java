package anana5.sense.logpoints;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class StmtVertexFormatter {
    static String format(Vertex<Stmt> vertex) {
        var stmt = vertex.value();
        if (stmt == null) {
            return "[root]";
        }

        if (stmt.containsInvokeExpr()) {
            var expr = stmt.getInvokeExpr();
            var mref = expr.getMethodRef();
            return mref.getName() + expr.getArgs().toString();
        } else {
            return vertex.toString();
        }
    }
}
