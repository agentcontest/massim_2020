package massim.monitor;

import org.webbitserver.EventSourceHandler;
import org.webbitserver.EventSourceConnection;
import org.webbitserver.EventSourceMessage;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventSink implements EventSourceHandler {
    private final String name;
    private String latestStatic;
    private String latestDynamic;
    private final ReentrantReadWriteLock poolLock = new ReentrantReadWriteLock();
    private final HashSet<EventSourceConnection> pool = new HashSet<EventSourceConnection>();

    public EventSink(String name) {
        this.name = name;
    }

    @Override
    public void onOpen(EventSourceConnection client) {
        Lock lock = poolLock.writeLock();
        lock.lock();
        try {
            pool.add(client);
            if (latestStatic != null) client.send(new EventSourceMessage(latestStatic));
            if (latestDynamic != null) client.send(new EventSourceMessage(latestDynamic));
            System.out.println(String.format("[ MONITOR ] %s: %d connection(s)", name, pool.size()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onClose(EventSourceConnection client) {
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

        EventSourceMessage msg = new EventSourceMessage(message);

        Lock lock = poolLock.readLock();
        lock.lock();
        try {
            for (EventSourceConnection client: pool) {
                client.send(msg);
            }
        } finally {
            lock.unlock();
        }
    }
}
