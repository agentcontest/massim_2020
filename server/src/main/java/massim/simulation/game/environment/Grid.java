package massim.simulation.game.environment;

import massim.protocol.data.Position;
import massim.simulation.game.Entity;
import massim.util.Log;
import massim.util.RNG;

import java.util.*;
import java.util.stream.Collectors;

public class Grid {

    public static final Set<String> DIRECTIONS = Set.of("n", "s", "e", "w");
    public static final Set<String> ROTATION_DIRECTIONS = Set.of("cw", "ccw");

    private int dimX;
    private int dimY;
    private int attachLimit;
    private Map<Position, Set<String>> thingsMap;
    private Terrain[][] terrainMap;

    public Grid(int dimX, int dimY, int attachLimit) {
        this.dimX = dimX;
        this.dimY = dimY;
        this.attachLimit = attachLimit;
        thingsMap = new HashMap<>();
        terrainMap = new Terrain[dimX][dimY];
        for (Terrain[] col : terrainMap) Arrays.fill(col, Terrain.EMPTY);
        for (int x = 0; x < dimX; x++) {
            terrainMap[x][0] = Terrain.OBSTACLE;
            terrainMap[x][dimY - 1] = Terrain.OBSTACLE;
        }
        for (int y = 0; y < dimY; y++) {
            terrainMap[0][y] = Terrain.OBSTACLE;
            terrainMap[dimX - 1][y] = Terrain.OBSTACLE;
        }
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public Entity createEntity(Position xy, String agentName, String teamName) {
        var e = new Entity(xy, agentName, teamName);
        insert(e.getID(), e.getPosition());
        return e;
    }

    public Block createBlock(Position xy, String type) {
        if(!isFree(xy)) return null;
        var b = new Block(xy, type);
        insert(b.getID(), b.getPosition());
        return b;
    }

    public void removeAttachable(Attachable a) {
        if (a == null) return;
        a.detachAll();
        getThings(a.getPosition()).remove(a.getID());
    }

    public Set<String> getThings(Position pos) {
        return thingsMap.computeIfAbsent(pos, kPos -> new HashSet<>());
    }

    public boolean insert(String id, Position pos) {
        if (outOfBounds(pos)) return false;
        getThings(pos).add(id);
        return true;
    }

    private boolean outOfBounds(Position pos) {
        return pos.x < 0 || pos.y < 0 || pos.x >= dimX || pos.y >= dimY;
    }

    private void move(Set<Attachable> attachables, Map<Attachable, Position> newPositions) {
        attachables.forEach(a -> getThings(a.getPosition()).remove(a.getID()));
        for (Attachable a : attachables) {
            var newPos = newPositions.get(a);
            insert(a.getID(), newPos);
            a.setPosition(newPos);
        }
    }

    /**
     * Moves an Attachable to a given position.
     * Only works if target is free and attachable has nothing attached.
     */
    public void moveWithoutAttachments(Attachable a, Position pos) {
        if(isFree(pos) && a.getAttachments().isEmpty()) {
            removeAttachable(a);
            insert(a.getID(), a.getPosition());
        }
    }

    public boolean attach(Attachable a1, Attachable a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getPosition().distanceTo(a2.getPosition()) != 1) return false;

        var attachments = getAllAttached(a1);
        attachments.addAll(getAllAttached(a2));
        if (attachments.size() > attachLimit) return false;

        a1.attach(a2);
        return true;
    }

    public boolean detach(Attachable a1, Attachable a2) {
        if (a1.getPosition().distanceTo(a2.getPosition()) != 1) return false;
        if (!a1.getAttachments().contains(a2)) return false;
        a1.detach(a2);
        return true;
    }

    public void print(){
        var sb = new StringBuilder(dimX * dimY * 3 + dimY);
        for (int row = 0; row < dimY; row++){
            for (int col = 0; col < dimX; col++){
                sb.append("[").append(getThings(Position.of(col, row)).size()).append("]");
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    /**
     * @return whether the movement succeeded
     */
    public boolean moveWithAttached(Attachable anchor, String direction, int distance) {
        var attachables = getAllAttached(anchor);
        var newPositions = canMove(attachables, direction, distance);
        if (newPositions == null) return false;
        move(attachables, newPositions);
        return true;
    }

    /**
     * @return whether the rotation succeeded
     */
    public boolean rotateWithAttached(Attachable anchor, boolean clockwise) {
        var newPositions = canRotate(anchor, clockwise);
        if (newPositions == null) return false;
        move(newPositions.keySet(), newPositions);
        return true;
    }

    /**
     * Checks if the anchor element and all attachments can rotate 90deg in the given direction.
     * Intermediate positions (the "diagonals") are also checked for all attachments.
     * @return a map from the element and all attachments to their new positions after rotation or null if anything is blocked
     */
    private Map<Attachable, Position> canRotate(Attachable anchor, boolean clockwise) {
        var attachments = getAllAttached(anchor);
        if(attachments.stream().anyMatch(a -> a != anchor && a instanceof Entity)) return null;
        var attachableIDs = attachments.stream().map(GameObject::getID).collect(Collectors.toSet());
        var newPositions = new HashMap<Attachable, Position>();
        for (Attachable a : attachments) {
            var rotatedPosition = a.getPosition();
            int distance = rotatedPosition.distanceTo(anchor.getPosition());
            for (int rotations = 0; rotations < distance; rotations++) {
                rotatedPosition = rotatedPosition.rotatedOneStep(anchor.getPosition(), clockwise);
                if(!isFree(rotatedPosition, attachableIDs)) return null;
            }
            newPositions.put(a, rotatedPosition);
        }
        return newPositions;
    }

    private Map<Attachable, Position> canMove(Set<Attachable> attachables, String direction, int distance) {
        var attachableIDs = attachables.stream().map(GameObject::getID).collect(Collectors.toSet());
        var newPositions = new HashMap<Attachable, Position>();
        for (Attachable a : attachables) {
            for (int i = 1; i <= distance; i++) {
                var newPos = a.getPosition().moved(direction, i);
                if(!isFree(newPos, attachableIDs)) return null;
            }
            newPositions.put(a, a.getPosition().moved(direction, distance));
        }
        return newPositions;
    }

    public Set<Attachable> getAllAttached(Attachable anchor) {
        var attachables = new HashSet<Attachable>();
        attachables.add(anchor);
        Set<Attachable> newAttachables = new HashSet<>(attachables);
        while (!newAttachables.isEmpty()) {
            Set<Attachable> tempAttachables = new HashSet<>();
            for (Attachable a : newAttachables) {
                for (Attachable a2 : a.getAttachments()) {
                    if (!attachables.contains(a2)) {
                        attachables.add(a2);
                        tempAttachables.add(a2);
                    }
                }
            }
            newAttachables = tempAttachables;
        }
        return attachables;
    }

    public Position findRandomFreePosition() {
        int x = RNG.nextInt(dimX);
        int y = RNG.nextInt(dimY);
        final int startX = x;
        final int startY = y;
        while (!isFree(Position.of(x,y))) {
            if (++x >= dimX) {
                x = 0;
                if (++y >= dimY) y = 0;
            }
            if (x == startX && y == startY) {
                Log.log(Log.Level.ERROR, "No free position");
                return null;
            }
        }
        return Position.of(x, y);
    }

    /**
     * @return true if the cell is in the grid and there is no other collidable and the terrain is not an obstacle.
     */
    public boolean isFree(Position xy) {
        return isFree(xy, Collections.emptySet());
    }

    private boolean isFree(Position xy, Set<String> excludedObjects) {
        if (outOfBounds(xy)) return false;

        var blockingThings = getThings(xy);
        blockingThings.removeAll(excludedObjects);

        return blockingThings.isEmpty() && terrainMap[xy.x][xy.y] != Terrain.OBSTACLE;
    }

    public void setTerrain(int x, int y, Terrain terrainType) {
        terrainMap[x][y] = terrainType;
    }

    public Terrain getTerrain(Position pos) {
        if (outOfBounds(pos)) return Terrain.EMPTY;
        return terrainMap[pos.x][pos.y];
    }
}
