package massim.simulation.game.environment;

import massim.protocol.messages.scenario.data.Thing;

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
    public Thing toPercept() {
        return new Thing(getPosition().x, getPosition().y, Thing.TYPE_BLOCK, blockType);
    }
}
