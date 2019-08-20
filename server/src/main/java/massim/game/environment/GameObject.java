package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class GameObject {

    private static AtomicInteger lastId = new AtomicInteger();

    private final int id = lastId.incrementAndGet();

    public final int getID() {
        return id;
    }

    public abstract Thing toPercept(Position relativeTo);
}
