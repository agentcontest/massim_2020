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

    private String latestStatic;
    private String latestDynamic;
    private String latestStatus;

    private final ReentrantReadWriteLock poolLock = new ReentrantReadWriteLock();
    private final HashSet<WebSocketConnection> pool = new HashSet<WebSocketConnection>();

    private final BaseWebSocketHandler socketHandler = new BaseWebSocketHandler() {

        @Override
        public void onOpen(WebSocketConnection client) {
            Lock lock = poolLock.writeLock();
            lock.lock();
            try {
                pool.add(client);
                if (latestStatic != null) client.send(latestStatic);
                if (latestDynamic != null) client.send(latestDynamic);
                System.out.println(String.format("[ MONITOR ] %d viewer(s) connected", pool.size()));
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void onClose(WebSocketConnection client) {
            Lock lock = poolLock.writeLock();
            lock.lock();
            try {
                pool.remove(client);
                System.out.println(String.format("[ MONITOR ] %d viewer(s) connected", pool.size()));
            } finally {
                lock.unlock();
            }
        }
    };

    /**
     * Constructor.
     * Used by the massim server to create the "live" monitor.
     */
    public Monitor(int port) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        InetSocketAddress bind = new InetSocketAddress(port);
        String publicUri = "http://127.0.0.1:" + port + "/";

        WebServer server = WebServers.createWebServer(executor, bind, URI.create(publicUri))
            .add("/socket", new HttpToWebSocketHandler(this.socketHandler))
            .add(new EmbeddedResourceHandler("www"))
            .start()
            .get();

        System.out.println(String.format("[ MONITOR ] Webmonitor listening on %s", publicUri));
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
        String publicUri = "http://127.0.0.1:" + port + "/";

        WebServer server = WebServers.createWebServer(executor, bind, URI.create(publicUri))
            .add(new EmbeddedResourceHandler("www"))
            .add("/?/", new StringHttpHandler("text/html", html))
            .add(new StaticFileHandler(replayPath))
            .start()
            .get();

        System.out.println(String.format("[ MONITOR ] Viewing replay %s on %s?/", replayPath, publicUri));
    }

    private void broadcast(String message) {
        Lock lock = poolLock.readLock();
        lock.lock();
        try {
            for (WebSocketConnection client: this.pool) {
                client.send(message);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the current state of the monitor.
     * Called by the massim server after each step.
     */
    public void updateState(JSONObject worldData){
        if (isStatic(worldData)) {
            this.latestStatic = worldData.toString();
            this.broadcast(this.latestStatic);
        } else {
            this.latestDynamic = worldData.toString();
            this.broadcast(this.latestDynamic);
        }
    }

    public void updateStatus(JSONObject status) {
        this.latestDynamic = status.toString();
    }

    private boolean isStatic(JSONObject worldData) {
        return worldData.has("grid");
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
