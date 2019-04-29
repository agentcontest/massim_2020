package massim.simulation.game.environment;

import massim.protocol.data.Position;
import massim.simulation.game.Entity;
import massim.util.Log;
import massim.util.RNG;

import java.util.*;
import java.util.stream.Collectors;

public class Grid {

    public static final Set<String> DIRECTIONS = Set.of("n", "s", "e", "w");

    private int dimX;
    private int dimY;
    private int attachLimit;
    private String[][] collisionMap;
    private Terrain[][] terrainMap;

    public Grid(int dimX, int dimY, int attachLimit) {
        this.dimX = dimX;
        this.dimY = dimY;
        this.attachLimit = attachLimit;
        collisionMap = new String[dimX][dimY];
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
        Entity e = new Entity(xy, agentName, teamName);
        insert(e.getID(), e.getPosition());
        return e;
    }

    public Block createBlock(Position xy, String type) {
        Block b = new Block(xy, type);
        insert(b.getID(), b.getPosition());
        return b;
    }

    public void removeAttachable(Attachable a) {
        if (a == null) return;
        a.detachAll();
        Position pos = a.getPosition();
        if (outOfBounds(pos)) return;
        collisionMap[pos.x][pos.y] = null;
    }

    public boolean insert(String id, Position pos) {
        if (outOfBounds(pos)) return false;
        if (!isFree(pos)) return false;
        collisionMap[pos.x][pos.y] = id;
        return true;
    }

    private boolean outOfBounds(Position pos) {
        return pos.x < 0 || pos.y < 0 || pos.x >= dimX || pos.y >= dimY;
    }

    private void move(Set<Attachable> attachables, Map<Attachable, Position> newPositions) {
        attachables.forEach(a -> collisionMap[a.getPosition().x][a.getPosition().y] = null);
        for (Attachable a : attachables) {
            Position newPos = newPositions.get(a);
            collisionMap[newPos.x][newPos.y] = a.getID();
            a.setPosition(newPos);
        }
    }

    public boolean attach(Attachable a1, Attachable a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getPosition().distanceTo(a2.getPosition()) != 1) return false;

        Set<Attachable> attachments = getAllAttached(a1);
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
        for (int row = 0; row < dimY; row++){
            StringBuilder rowString = new StringBuilder();
            for (int col = 0; col < dimX; col++){
                rowString.append(collisionMap[col][row] != null ? "[O]" : "[ ]");
            }
            System.out.println(rowString.toString());
        }
        System.out.println();
    }

    /**
     * @return whether the movement succeeded
     */
    public boolean moveWithAttached(Attachable anchor, String direction, int distance) {
        Set<Attachable> attachables = getAllAttached(anchor);
        Map<Attachable, Position> newPositions = canMove(attachables, direction, distance);
        if (newPositions == null) return false;
        move(attachables, newPositions);
        return true;
    }

    /**
     * @return whether the rotation succeeded
     */
    public boolean rotateWithAttached(Attachable anchor, boolean clockwise) {
        Map<Attachable, Position> newPositions = canRotate(anchor, clockwise);
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
        Set<Attachable> attachments = getAllAttached(anchor);
        if(attachments.stream().anyMatch(a -> a != anchor && a instanceof Entity)) return null;
        Set<String> attachableIDs = attachments.stream().map(GameObject::getID).collect(Collectors.toSet());
        Map<Attachable, Position> newPositions = new HashMap<>();
        for (Attachable a : attachments) {
            Position rotatedPosition = a.getPosition();
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
        Set<String> attachableIDs = attachables.stream().map(GameObject::getID).collect(Collectors.toSet());
        Map<Attachable, Position> newPositions = new HashMap<>();
        for (Attachable a : attachables) {
            for (int i = 1; i <= distance; i++) {
                Position newPos = a.getPosition().moved(direction, i);
                if(!isFree(newPos, attachableIDs)) return null;
            }
            newPositions.put(a, a.getPosition().moved(direction, distance));
        }
        return newPositions;
    }

    public Set<Attachable> getAllAttached(Attachable anchor) {
        Set<Attachable> attachables = new HashSet<>();
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

    /**
     * @param position the position to check
     * @return A game object on the collision layer at that position or null if there is none.
     */
    public String getCollidable(Position position) {
        if (outOfBounds(position)) return null;
        return collisionMap[position.x][position.y];
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
        return !outOfBounds(xy)
                && (collisionMap[xy.x][xy.y] == null || excludedObjects.contains(collisionMap[xy.x][xy.y]))
                && terrainMap[xy.x][xy.y] != Terrain.OBSTACLE;
    }

    public void setTerrain(Position pos, Terrain terrainType) {
        if (!outOfBounds(pos)) {
            terrainMap[pos.x][pos.y] = terrainType;
        }
    }

    public Terrain getTerrain(Position pos) {
        if (outOfBounds(pos)) return Terrain.EMPTY;
        return terrainMap[pos.x][pos.y];
    }
}
