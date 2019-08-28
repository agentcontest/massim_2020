package massim.game.environment;

import massim.protocol.data.Position;

import java.util.ArrayList;

public class Area extends ArrayList<Position> {
    /**
     * Creates a new list containing all positions belonging to the
     * area around a given center within the given radius.
     */
    public Area(Position center, int radius) {
        if (center == null) return;
        for (var dx = -radius; dx <= radius; dx++) {
            var x = center.x + dx;
            var dy = radius - Math.abs(dx);
            for (var y = center.y - dy; y <= center.y + dy; y++) {
                this.add(Position.of(x, y));
            }
        }
    }
}
