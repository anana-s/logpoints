package anana5.sense.logpoints;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import anana5.util.Promise;

public class PrintNodes {

    public static void main(String[] args) {
        OnlineGraphCLI.parse(args);
        var graph = LogPoints.v().build().join();

        Set<Box.Ref> discovered = new HashSet<>();
        var task = graph.traverse(($, ref) -> {
            if (!discovered.contains(ref)) {
                discovered.add(ref);
                var sj = new StringJoiner("\t");
                sj.add(String.format("nx%08x", ref.hashCode()));
                sj.add(StmtVertexFormatter.format(ref));
                sj.add(ref.get().getTag("SourceMapTag").toString());
                System.out.println(sj.toString());
            }
            return Promise.lazy();
        });

        task.join();
    }

}
