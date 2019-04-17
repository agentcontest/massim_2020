package massim.simulation.game.environment;

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
}
