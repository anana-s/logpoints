package anana5.sense.logpoints;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anana5.graph.rainfall.Rain;
import anana5.util.Promise;
import net.sourceforge.argparse4j.inf.Namespace;

public class OnlineGraph implements Callable<Void>, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OnlineGraph.class);
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final SerialRefGraph graph;

    public OnlineGraph(Promise<SerialRefGraph> promise, ServerSocket server) throws IOException {
        this.socket = server.accept();
        this.graph = promise.join();
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public static void main(String[] args) {
        Namespace ns = OnlineGraphCLI.parse(args);
        ExecutorService executor = Executors.newCachedThreadPool();

        // try (var printer = new DotPrinter(System.out)) {
        //     Rain.bind(LogPoints.v().build()).traverse((a, b) -> {
        //         printer.print(a == null ? null : new SerialRef(a), new SerialRef(b));
        //         return Promise.nil();
        //     }).join();
        // }

        executor.submit(() -> {
            try {
                LogPoints.v().graph().resolve();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        });

        var promise = Promise.lazy(() -> new SerialRefGraph(LogPoints.v().graph()));

        try (var server = new ServerSocket(ns.getInt("port"))) {
            while (true) {
                try {
                    var task = new OnlineGraph(promise, server);
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
                var res = req.accept(graph);
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
