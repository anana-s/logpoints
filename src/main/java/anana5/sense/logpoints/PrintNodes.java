package anana5.sense.logpoints;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import anana5.graph.Vertex;
import anana5.util.Promise;
import soot.jimple.Stmt;

public class PrintNodes {

    public static void main(String[] args) {
        OnlineGraphCLI.parse(args);
        var graph = LogPoints.v().build().join();

        Set<Box.Ref> discovered = new HashSet<>();
        var task = graph.traverse(($, vertex) -> {
            if (!discovered.contains(vertex)) {
                discovered.add(vertex);
                var sj = new StringJoiner("\t");
                sj.add(String.format("nx%08x", vertex.hashCode()));
                sj.add(StmtVertexFormatter.format(vertex));
                sj.add(vertex.value().getTag("SourceMapTag").toString());
                System.out.println(sj.toString());
            }
            return Promise.lazy();
        });

        task.join();
    }

}
