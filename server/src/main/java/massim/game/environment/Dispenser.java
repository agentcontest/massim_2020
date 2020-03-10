package massim.game.environment;

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
        Position local = getPosition().relativeTo(entityPosition);
        return new Thing(local.x, local.y, Thing.TYPE_DISPENSER, blockType);
    }

    @Override
    public String toString() {
        return "dispenser(" + getPosition().x + "," + getPosition().y + "," + blockType + ")";
    }
}
