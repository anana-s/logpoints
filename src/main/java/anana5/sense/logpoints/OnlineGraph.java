package anana5.sense.logpoints;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.argparse4j.inf.Namespace;

public class OnlineGraph {
    public static void main(String[] args) {
        Namespace ns = Cmd.parse(args);

        try (Socket socket = new Socket("localhost", 7000)) {

         } catch (UnknownHostException e) {

        } catch (IOException e) {

        }
    }
}
