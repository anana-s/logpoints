package anana5.sense.logpoints;

import java.io.PrintStream;
import java.util.ArrayList;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Cmd {
    public static Namespace parse(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("logpoints").build()
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

        parser.addArgument("-o", "--output")
            .nargs("?")
            .type(PrintStream.class)
            .setDefault(System.out);

        parser.addArgument("--trace")
            .setDefault(false)
            .action(Arguments.storeTrue());

        parser.addArgument("classes").nargs("+");

        Namespace ns;

        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return null;
        }

        return ns;
    }
}
