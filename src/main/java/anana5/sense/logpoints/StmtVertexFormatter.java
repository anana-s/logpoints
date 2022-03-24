package anana5.sense.logpoints;

import java.util.UUID;

import anana5.graph.Vertex;
import soot.jimple.Stmt;

public class StmtVertexFormatter {
    static String format(Vertex<Stmt> vertex) {
        var stmt = vertex.value();

        if (stmt == null || !stmt.containsInvokeExpr()) {
            return vertex.toString();
        }
        var expr = stmt.getInvokeExpr();
        var mref = expr.getMethodRef();
        return mref.getName() + expr.getArgs().toString() + stmt.getTag("SourceMapTag").toString();
    }
}
