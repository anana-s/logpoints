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

import anana5.sense.logpoints.LogPoints.EntrypointNotFoundException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Server implements Callable<Void>, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Server(ServerSocket server) throws IOException {
        this.socket = server.accept();
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public static void main(String[] args) {
        Namespace ns;
        try {
            ns = OnlineGraphCLI.parse(args);
        } catch (EntrypointNotFoundException e) {
            log.error("entrypoint not found: {}", e.name());
            return;
        }

        // TODO: implement threadsafe promises to use thread pool
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // try (var printer = new DotPrinter(System.out)) {
        //     Rain.bind(LogPoints.v().build()).traverse((a, b) -> {
        //         printer.print(a == null ? null : new SerialRef(a), new SerialRef(b));
        //         return Promise.nil();
        //     }).join();
        // }

        executor.submit(() -> {
            try {
                LogPoints.v().graph();
            } catch (Throwable e) {
                e.printStackTrace();
                executor.shutdownNow();
            }
        });

        try (var socket = new ServerSocket(ns.getInt("port"))) {
            while (true) {
                try {
                    var task = new Server(socket);
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
        try {
            while (true) {
                var req = (LogPointsRequest<?>)in.readObject();
                log.debug("received {}", req);
                var res = req.accept(LogPoints.v());
                out.writeObject(res);
            }
        } catch (EOFException e) {
            log.debug("connection closed");
        } catch (Throwable e) {
            log.error("exited on error");
            e.printStackTrace();
        } finally {
            close();
        }
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
