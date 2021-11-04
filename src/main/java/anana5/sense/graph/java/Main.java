package anana5.sense.graph.java;

import java.util.ArrayList;
import java.util.Arrays;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Main {
    public static void main(String[] args) {
        Options.v().parse(args);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_exclude(Arrays.asList("jdk.*"));;

        Scene.v().loadNecessaryClasses();

        for(SootClass c : new ArrayList<>(Scene.v().getClasses())) {
            for (SootMethod m : new ArrayList<>(c.getMethods())) {
                if (m.isJavaLibraryMethod()) {
                    continue;
                }

                if (m.isPhantom()) {
                    continue;
                }

                JimpleBody b;
                try {
                     b = (JimpleBody) m.retrieveActiveBody();
                } catch (RuntimeException e) {
                    continue;
                }

                ExceptionalGraph<Unit> g = new ExceptionalUnitGraph(b);
                for (Unit u : g) {
                    System.out.println(u);
                }
            }
        }

    }
}
