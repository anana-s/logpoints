package anana5.sense.logpoints;

import java.util.ArrayList;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class OnlineGraphCLI {
    public static Namespace parse(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints serve").build()
            .defaultHelp(true)
            .description("Constructs the interprocedural flow graph of logging statements.");

        parser.addArgument("-p", "--prepend")
            .setDefault(false)
            .action(Arguments.storeTrue());

        parser.addArgument("-c", "--classpath")
            .setDefault("");
        
        parser.addArgument("-m", "--modulepath")
            .setDefault("");

        parser.addArgument("-i", "--include")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("-x", "--exclude")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("-t", "--tag")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("--trace")
            .setDefault(false)
            .action(Arguments.storeTrue());

        parser.addArgument("--port")
            .setDefault(7000)
            .type(Integer.class);

        parser.addArgument("classes").nargs("+");

        Namespace ns;

        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return null;
        }

        LogPoints.v().prepend(ns.getBoolean("prepend"));
        LogPoints.v().classpath(ns.getString("classpath"));
        LogPoints.v().modulepath(ns.getString("modulepath"));
        LogPoints.v().include(ns.<String>getList("include"));
        LogPoints.v().exclude(ns.<String>getList("exclude"));
        LogPoints.v().classes(ns.<String>getList("classes"));
        LogPoints.v().trace(ns.getBoolean("trace"));
        for (String tag : ns.<String>getList("tag")) {
            LogPoints.v().tag(tag);
        }

        return ns;
    }
}
