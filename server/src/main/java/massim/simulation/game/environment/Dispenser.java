package massim.simulation.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

public class Dispenser extends Positionable {

    private String blockType;

    public Dispenser(Position position, String blockType) {
        super(position);
        this.blockType = blockType;
    }

    public String getBlockType() {
        return blockType;
    }

    @Override
    public Thing toPercept(Position entityPosition) {

        return new Thing(getPosition().x, getPosition().y, Thing.TYPE_DISPENSER, blockType);
    }
}
