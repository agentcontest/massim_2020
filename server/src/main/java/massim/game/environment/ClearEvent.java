package massim.game.environment;

import massim.protocol.data.Position;

public class ClearEvent {

    private Position position;
    private int step;
    private int radius;

    public ClearEvent(Position position, int step, int radius) {
        this.position = position;
        this.step = step;
        this.radius = radius;
    }

    public Position getPosition() {
        return position;
    }

    public int getStep() {
        return step;
    }

    public int getRadius() {
        return radius;
    }
}
