package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.Thing;

/**
 * A simple marker marking a position.
 */
public class Marker extends Positionable {

    private Type type;

    public Marker(Position pos, Type type) {
        super(pos);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Thing toPercept(Position relativeTo) {
        var pos = getPosition().relativeTo(relativeTo);
        return new Thing(pos.x, pos.y, Thing.TYPE_MARKER, type.name);
    }

    @Override
    public String toString() {
        return "Marker("  + getPosition()+"," + type+")";
    }

    public enum Type {
        CLEAR("clear"),
        CLEAR_PERIMETER("cp"),
        CLEAR_IMMEDIATE("ci");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }
}
