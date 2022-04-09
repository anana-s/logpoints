package anana5.sense.logpoints;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.util.Promise;
import net.sourceforge.argparse4j.inf.Namespace;
import polyglot.lex.EOF;

public class OnlineGraph implements Callable<Void>, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OnlineGraph.class);
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Promise<BoxGraph> graph;

    public OnlineGraph(ServerSocket server) throws IOException {
        graph = Promise.lazy(() -> new BoxGraph(LogPoints.v().graph()));
        socket = server.accept();
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public static void main(String[] args) {
        Namespace ns = OnlineGraphCLI.parse(args);
        ExecutorService executor = Executors.newCachedThreadPool();

        var future = executor.submit(() -> LogPoints.v().graph().roots());

        try {
            Thread.sleep(1000);
            if (future.isDone()) {
                future.get();
            }
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        try (var server = new ServerSocket(ns.getInt("port"))) {
            while (true) {
                try {
                    var task = new OnlineGraph(server);
                    log.debug("new connection");
                    executor.submit(task);
                } catch (IOException e) {
                    log.error("{}", e);
                    continue;
                }
            }
        } catch (IOException e) {
            log.error("could not listen on port {}", ns.getInt("port"));
            e.printStackTrace();
            executor.shutdownNow();
        }
    }

    @Override
    public Void call() {
        log.debug("started");
        while (true) {
            try {
                var req = (GraphRequest<?>)in.readObject();
                log.debug("received {}", req);
                var res = req.accept(graph.join());
                out.writeObject(res);
            } catch (EOFException e) {
                log.debug("connection closed");
                break;
            } catch (ClassNotFoundException | IOException e) {
                log.error("{}", e);
                e.printStackTrace();
                break;
            }
        }
        close();
        return null;
    }

    @Override
    public void close() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            return;
        }
    }
}
