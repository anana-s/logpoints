package anana5.sense.logpoints;

import java.util.ArrayList;
import java.util.Arrays;

import anana5.sense.logpoints.LogPoints.EntrypointNotFoundException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class OnlineGraphCLI {
    public static Namespace parse(String[] args) throws EntrypointNotFoundException {
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
            .setDefault(new ArrayList<>(Arrays.asList("jdk.*")))
            .action(Arguments.append());

        parser.addArgument("-t", "--tag")
            .setDefault(new ArrayList<>())
            .action(Arguments.append());

        parser.addArgument("--trace")
            .setDefault(false)
            .action(Arguments.storeTrue());

        parser.addArgument("--disable-clinit")
            .setDefault(false)
            .action(Arguments.storeTrue());

        parser.addArgument("--port")
            .setDefault(7000)
            .type(Integer.class);

        parser.addArgument("entrypoints").nargs("+");

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
        for (String entrypoint : ns.<String>getList("entrypoints")) {
            LogPoints.v().entrypoint(entrypoint);
        }
        LogPoints.v().trace(ns.getBoolean("trace"));
        if (ns.getBoolean("disable_clinit")) {
            LogPoints.v().clinit(false);
        }
        for (String tag : ns.<String>getList("tag")) {
            LogPoints.v().tag(tag);
        }

        return ns;
    }
}
