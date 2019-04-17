package massim.simulation.game.environment;

import java.util.UUID;

public abstract class GameObject {

    private String id = UUID.randomUUID().toString();

    public String getID() {
        return id;
    }
}
