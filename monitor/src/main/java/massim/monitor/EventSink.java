package massim.monitor;

import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventSink extends BaseWebSocketHandler {
    private final String name;
    private String latestStatic;
    private String latestDynamic;
    private final ReentrantReadWriteLock poolLock = new ReentrantReadWriteLock();
    private final HashSet<WebSocketConnection> pool = new HashSet<WebSocketConnection>();

    public EventSink(String name) {
        this.name = name;
    }

    @Override
    public void onOpen(WebSocketConnection client) {
        Lock lock = poolLock.writeLock();
        lock.lock();
        try {
            pool.add(client);
            if (latestStatic != null) client.send(latestStatic);
            if (latestDynamic != null) client.send(latestDynamic);
            System.out.println(String.format("[ MONITOR ] %s: %d connection(s)", name, pool.size()));
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
            System.out.println(String.format("[ MONITOR ] %s: %d connection(s)", name, pool.size()));
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(String message, boolean dynamic) {
        if (dynamic) this.latestDynamic = message;
        else this.latestStatic = message;

        Lock lock = poolLock.readLock();
        lock.lock();
        try {
            for (WebSocketConnection client: pool) {
                client.send(message);
            }
        } finally {
            lock.unlock();
        }
    }
}
