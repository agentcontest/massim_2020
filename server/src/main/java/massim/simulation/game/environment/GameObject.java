package massim.simulation.game.environment;

import massim.protocol.messages.scenario.data.Thing;

import java.util.UUID;

public abstract class GameObject {

    private String id = UUID.randomUUID().toString();

    public String getID() {
        return id;
    }

    public abstract Thing toPercept();
}
