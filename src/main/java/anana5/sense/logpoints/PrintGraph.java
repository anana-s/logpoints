package anana5.sense.logpoints;

import net.sourceforge.argparse4j.inf.Namespace;

public class PrintGraph {

    public static void main(String[] args) {
        Namespace ns = Cmd.parse(args);
        LogPoints.v().configure(ns);
        
        var graph = LogPoints.v().graph();

        // traverse graph
        try (var printer = new DotPrinter(ns.get("output"))) {
            graph.traverse((src, tgt) -> {
                printer.print(src == null ? null : src.value(), tgt.value());
            });
        }
    }
}
