package anana5.sense.logpoints;

import java.util.HashSet;
import java.util.Set;

import anana5.graph.Vertex;
import anana5.util.LList;
import anana5.util.Promise;
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
                    return LList.cons(Tuple.of(.0, .0), LList.of());
                }
                seen.add(droplet.get());
                return LList.bind(droplet.next().empty().fmap(isEmpty -> {
                    if (droplet.get().value().getInvokeExpr().getMethodRef().getName().equals("println")) {
                        if (isEmpty) {
                            return LList.cons(Tuple.of(1., 1.), LList.of());
                        } else {
                            return droplet.next().map(tuple -> Tuple.of(tuple.fst() + 1., tuple.snd() + 1.));
                        }
                    } else {
                        if (isEmpty) {
                            return LList.cons(Tuple.of(0., 1.), LList.of());
                        } else  {
                            return droplet.next().map(tuple -> Tuple.of(tuple.fst(), tuple.snd() + 1.));
                        }
                    }
                }));
            });
        });

        Double average = ratios.foldr(Tuple.of(.0, .0), (cur, acc) -> {
            return Promise.just(Tuple.of((cur.fst() / cur.snd()) + acc.fst(), acc.snd() + 1));
        }).fmap(tuple -> {
            return tuple.fst() / tuple.snd();
        }).join();

        System.out.println(average);
    }
}
