package massim.simulation.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

public class Block extends Attachable {

    public final static String BLOCK_TYPE_NORMAL = "normal";

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
        Position relativePosition = getPosition().toLocal(entityPosition);
        return new Thing(relativePosition.x, relativePosition.y, Thing.TYPE_BLOCK, blockType);
    }
}