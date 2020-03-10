package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

public class Block extends Attachable {

    private String blockType;

    public Block(Position xy, String blockType) {
        super(xy);
        this.blockType = blockType;
    }

    public String getBlockType(){
        return this.blockType;
    }

    @Override
    public Thing toPercept(Position entityPosition) {
        Position relativePosition = getPosition().relativeTo(entityPosition);
        return new Thing(relativePosition.x, relativePosition.y, Thing.TYPE_BLOCK, blockType);
    }
}