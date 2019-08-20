package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class GameObject {

    private static AtomicInteger nextId = new AtomicInteger(1);

    private final String id = Integer.toString(nextId.getAndIncrement());

    public final String getID() {
        return id;
    }

    public abstract Thing toPercept(Position relativeTo);
}
