package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

public class TaskBoard extends Positionable {

    public TaskBoard(Position position) {
        super(position);
    }

    @Override
    public Thing toPercept(Position entity) {
        var local = getPosition().relativeTo(entity);
        return new Thing(local.x, local.y, Thing.TYPE_TASKBOARD, "");
    }

    @Override
    public String toString() {
        return "Taskboard" + getPosition();
    }
}