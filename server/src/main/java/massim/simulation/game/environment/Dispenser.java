package massim.simulation.game.environment;

public class Dispenser extends Positionable {

    private String blockType;

    public Dispenser(Position position, String blockType) {
        super(position);
        this.blockType = blockType;
    }

    public String getBlockType() {
        return blockType;
    }
}
