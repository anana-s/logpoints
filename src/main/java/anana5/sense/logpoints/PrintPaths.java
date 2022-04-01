package anana5.sense.logpoints;

import java.util.HashMap;
import java.util.StringJoiner;

import anana5.graph.Vertex;
import anana5.util.PList;
import anana5.util.LList;
import anana5.util.Promise;
import soot.jimple.Stmt;

public class PrintPaths {
    public static void main(String[] args) {
        LogPoints.v().configure(args);

        var graph = LogPoints.v().graph();

        var seen = new HashMap<Box.Ref, PList<LList<Box.Ref>>>();
        PList<LList<Box.Ref>> paths = graph.rain().fold(droplets -> droplets.flatmap(droplet -> {
            var vertex = droplet.get();

            if (seen.containsKey(vertex)) {
                return seen.get(vertex);
            }

            var paths$ = droplet.next();
            return PList.bind(paths$.empty().map(e -> {
                if (e) {
                    var path$ = PList.cons(LList.<Box.Ref>nil(), PList.of());
                    seen.put(vertex, path$);
                    return path$;
                } else {
                    var path$ = paths$.map(path -> path.push(vertex));
                    seen.put(vertex, path$);
                    return path$;
                }
            }));
        }));

        paths.traverse(path -> {
            if (path.empty()) {
                return Promise.lazy();
            }

            var sj = new StringJoiner("\t");
            path.traverse(vertex -> {
                sj.add(String.format("nx%08x %s", vertex.hashCode(), StmtVertexFormatter.format(vertex)));
            });
            System.out.println(sj.toString());
            return Promise.lazy();
        }).join();
    }

}
