package massim.protocol.data;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Objects;

public final class Position {

    private static int dimX = 0;
    private static int dimY = 0;

    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Position)) return false;
        return ((Position) other).x == x && ((Position) other).y == y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public static void setGridDimensions(int dimX, int dimY) {
        Position.dimX = dimX;
        Position.dimY = dimY;
    }

    /**
     * @return the same position but wrapped back into the bounds
     */
    public static Position wrapped(int someX, int someY) {
        // handle negative values correctly
        return Position.of(Math.floorMod(someX, dimX), Math.floorMod(someY, dimY));
    }

    public Position wrapped() {
        return Position.wrapped(x, y);
    }

    public int distanceTo(Position other) {
            int dx = Math.abs(x - other.x);
            if (dx > dimX/2.0) dx = dimX - dx;
            int dy = Math.abs(y - other.y);
            if (dy > dimY/2.0) dy = dimY - dy;
            return dx + dy;
    }

    public Position moved(String direction, int distance) {
        switch (direction) {
            case "n": return Position.wrapped(x, y - distance);
            case "s": return Position.wrapped(x, y + distance);
            case "w": return Position.wrapped(x - distance, y);
            case "e": return Position.wrapped(x + distance, y);
        }
        return Position.of(x, y);
    }

    public static Position of(int x, int y) {
        return new Position(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public Position translate(int x, int y) {
        return Position.wrapped(this.x + x, this.y + y);
    }

    public Position translate(Position other) {
        return translate(other.x, other.y);
    }

    public Position relativeTo(Position origin) {
        var dx = x - origin.x;
        if (dx < -(dimX / 2.0)) dx += dimX;
        else if (dx > dimX / 2.0) dx -= dimX;
        var dy = y - origin.y;
        if (dy < -(dimY / 2.0)) dy += dimY;
        else if (dy > dimY / 2.0) dy -= dimY;
        return Position.of(dx, dy);
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.put(x);
        result.put(y);
        return result;
    }

    public static Position fromJSON(JSONArray json) {
        return Position.of(json.getInt(0), json.getInt(1));
    }

    /**
     * @return list containing all positions belonging to the area around this position within the given radius.
     */
    public ArrayList<Position> spanArea(int radius) {
        var area = new ArrayList<Position>();
        for (var dx = -radius; dx <= radius; dx++) {
            var cx = x + dx;
            var dy = radius - Math.abs(dx);
            for (var cy = y - dy; cy <= y + dy; cy++) {
                area.add(Position.wrapped(cx, cy));
            }
        }
        return area;
    }

    /**
     * @return this position rotated 90 degrees in the given direction
     */
    public Position rotated90(Position center, boolean clockwise) {
        var pos = this.relativeTo(center);
        // the rotation is calculated relative to the rotation center
        //var pos = Position.of(center.x + relative.x, center.y + relative.y);
        var dx = clockwise? -pos.y : pos.y;
        var dy = clockwise? pos.x : -pos.x;
        return Position.wrapped(center.x + dx, center.y + dy);
    }
}
