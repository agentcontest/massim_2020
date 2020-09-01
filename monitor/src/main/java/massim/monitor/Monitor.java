package massim.monitor;

import org.json.JSONObject;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.handler.EmbeddedResourceHandler;
import org.webbitserver.handler.HttpToWebSocketHandler;
import org.webbitserver.handler.StaticFileHandler;
import org.webbitserver.handler.StringHttpHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The web monitor for the MASSim server.
 */
public class Monitor {

    private final EventSink monitorSink = new EventSink("monitor");

    private final EventSink statusSink = new EventSink("status");

    /**
     * Constructor.
     * Used by the massim server to create the "live" monitor.
     */
    public Monitor(int port) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        InetSocketAddress bind = new InetSocketAddress(port);
        String publicUri = "http://localhost:" + port + "/";

        WebServer server = WebServers.createWebServer(executor, bind, URI.create(publicUri))
            .add("/live/monitor", monitorSink)
            .add("/live/status", statusSink)
            .add(new EmbeddedResourceHandler("www"))
            .start()
            .get();

        System.out.println(String.format("[ MONITOR ] Live monitor: %s", publicUri));
        System.out.println(String.format("[ MONITOR ] Live status:  %sstatus.html", publicUri));
    }

    /**
     * Creates a new monitor to watch replays with.
     * @param replayPath the path to a replay file
     */
    Monitor(int port, String replayPath) throws ExecutionException, InterruptedException {
        // read index.html from resources
        String html = new Scanner(Monitor.class.getClassLoader().getResourceAsStream("www/index.html"), "UTF-8")
            .useDelimiter("\\A")
            .next();

        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        InetSocketAddress bind = new InetSocketAddress(port);
        String publicUri = "http://localhost:" + port + "/";

        WebServer server = WebServers.createWebServer(executor, bind, URI.create(publicUri))
            .add(new EmbeddedResourceHandler("www"))
            .add("/?/", new StringHttpHandler("text/html", html))
            .add(new StaticFileHandler(replayPath))
            .start()
            .get();

        System.out.println(String.format("[ MONITOR ] Viewing replay %s on %s?/", replayPath, publicUri));
    }

    /**
     * Updates the current state of the monitor.
     * Called by the massim server after each step.
     */
    public void updateState(JSONObject state) {
        monitorSink.broadcast(state.toString(), !state.has("grid"));
    }

    public void updateStatus(JSONObject status) {
        statusSink.broadcast(status.toString(), true);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int port = 8000;
        String path = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                default:
                    path = args[i];
                    break;
            }
        }

        if (path == null) {
            System.out.println("Usage: java -jar monitor.jar [--port PORT] <path to replay>");
            return;
        }

        if (!Paths.get(path, "static.json").toFile().exists()) {
            System.out.println("Not a replay. static.json does not seem to exist in this directory.");
            return;
        }

        new Monitor(port, path);
    }
}
