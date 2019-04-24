package massim.simulation.game.environment;

import java.util.Objects;

public class Position {

    public int x;
    public int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Position copy() {
        return new Position(x, y);
    }

    public int distanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
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

    public Position moved(String direction, int distance) {
        switch (direction) {
            case "n": return Position.of(x, y - distance);
            case "s": return Position.of(x, y + distance);
            case "w": return Position.of(x - distance, y);
            case "e": return Position.of(x + distance, y);
        }
        return null;
    }

    public static Position of(int x, int y) {
        return new Position(x, y);
    }

    public static String oppositeDirection(String direction) {
        switch(direction) {
            case "n": return "s";
            case "s": return "n";
            case "e": return "w";
            case "w": return "e";
            default: return null;
        }
    }

    static Position rotate90(Position anchor, Position object, boolean clockwise) {
        int nx = object.y - anchor.y; // local
        int ny = object.x - anchor.x; // coordinates
        if (clockwise) nx *= -1; else ny *= -1;
        return Position.of(anchor.x + nx, anchor.y + ny);
    }

    /**
     * @return the direction (n,s,e,w) to another position (only works if either x or y is shared)
     */
    public String directionTo(Position other) {
        if (other.x == x) {
            if (other.y > y) return "s";
            if (other.y < y) return "n";
            return null;
        }
        if (other.y == y) {
            if (other.x > x) return "e";
            return "w";
        }
        return null;
    }

    public Position rotatedOneStep(Position anchor, boolean clockwise) {
        int localX = x - anchor.x;
        int localY = y - anchor.y;
        if (clockwise) {
            if (localX >= 0 && localY < 0) return Position.of(x + 1, y + 1);
            else if (localX > 0) return Position.of(x - 1, y + 1);
            else if (localY > 0) return Position.of(x - 1, y - 1);
            else return Position.of(x + 1, y - 1);
        }
        else {
            if (localX > 0 && localY <= 0) return Position.of(x - 1, y - 1);
            else if (localX <= 0 && localY < 0) return Position.of(x - 1, y + 1);
            else if (localX < 0) return Position.of(x + 1, y + 1);
            else return Position.of(x + 1, y - 1);


        }
    }

    @Override
    public String toString() {
        return "P(" + x + "," + y + ")";
    }

    public Position translate(Position other) {
        return Position.of(other.x + x, other.y + y);
    }
}
