package anana5.sense.logpoints;

import java.util.Arrays;

import anana5.graph.rainfall.Rain;
import anana5.util.PList;
import anana5.util.Promise;
import net.sourceforge.argparse4j.inf.Namespace;
import soot.jimple.Stmt;

public class PrintGraph {

    public static void main(String[] args) {
        Namespace ns = Cmd.parse(args);
        LogPoints.v().configure(ns);
        Rain<Box<Stmt>.Ref> graph = LogPoints.v().graph();

        // traverse graph
        try (var printer = new DotPrinter(ns.get("output"), StmtVertexFormatter::format)) {
            graph.traverse((src, tgt) -> {
                printer.print(src, tgt);
                return Promise.lazy();
            }).join();
        } catch (StackOverflowError e) {
            var trace = e.getStackTrace();
            trace = Arrays.copyOfRange(trace, trace.length - 50, trace.length);
            PList.of(trace).traverse(element -> {
                System.err.println(element);
                return Promise.nil();
            }).join();
        }
    }
}
