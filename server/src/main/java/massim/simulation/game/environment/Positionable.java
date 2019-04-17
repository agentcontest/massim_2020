package massim.simulation.game.environment;

public abstract class Positionable  extends GameObject {

    private Position position;

    public Positionable(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    void setPosition(Position position){
        if (position == null) return;
        this.position = position;
    }
}
