package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

import java.util.UUID;

public abstract class GameObject {

    private String id = UUID.randomUUID().toString();

    public String getID() {
        return id;
    }

    public abstract Thing toPercept(Position relativeTo);
}
