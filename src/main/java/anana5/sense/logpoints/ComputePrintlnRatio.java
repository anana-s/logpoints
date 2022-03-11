package anana5.sense.logpoints;

import java.util.HashSet;
import java.util.Set;

import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Tuple;
import soot.jimple.Stmt;

public class ComputePrintlnRatio {

    public static void main(String[] args) {
        Factory.v().configure(args);
        var graph = Factory.v().graph();

        // stats
        Set<Vertex<Stmt>> seen = new HashSet<>();
        LList<Tuple<Double, Double>> ratios = graph.fold(droplets -> {
            return droplets.flatmap(droplet -> {
                if (seen.contains(droplet.get())) {
                    return LList.cons(new Tuple<>(.0, .0), LList.nil());
                }
                seen.add(droplet.get());
                return LList.bind(droplet.next().isEmpty().map(isEmpty -> {
                    if (droplet.get().value().getInvokeExpr().getMethodRef().getName().equals("println")) {
                        if (isEmpty) {
                            return LList.cons(new Tuple<>(1., 1.), LList.nil());
                        } else {
                            return droplet.next().map(tuple -> new Tuple<>(tuple.fst() + 1., tuple.snd() + 1.));
                        }
                    } else {
                        if (isEmpty) {
                            return LList.cons(new Tuple<>(0., 1.), LList.nil());
                        } else  {
                            return droplet.next().map(tuple -> new Tuple<>(tuple.fst(), tuple.snd() + 1.));
                        }
                    }
                }));
            });
        });

        Double average = ratios.foldr(new Tuple<>(.0, .0), (cur, acc) -> {
            return new Tuple<>((cur.fst() / cur.snd()) + acc.fst(), acc.snd() + 1);
        }).map(tuple -> {
            return tuple.fst() / tuple.snd();
        }).join();

        System.out.println(average);
    }
}
