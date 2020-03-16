package massim.game.environment;

import massim.game.Entity;
import massim.protocol.data.Position;
import massim.util.Log;
import massim.util.RNG;
import org.json.JSONObject;


import javax.imageio.ImageIO;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Grid {

    public static final Set<String> DIRECTIONS = Set.of("n", "s", "e", "w");
    public static final Set<String> ROTATION_DIRECTIONS = Set.of("cw", "ccw");
    private static Map<Integer, Terrain> terrainColors =
            Map.of(-16777216, Terrain.OBSTACLE, -1, Terrain.EMPTY, -65536, Terrain.GOAL);

    private int dimX;
    private int dimY;
    private int attachLimit;
    private Map<Position, Set<Positionable>> thingsMap;
    private Terrain[][] terrainMap;
    private List<Marker> markers = new ArrayList<>();
    private Map<String,Boolean> blockedForTaskBoards = new HashMap<>();

    public Grid(JSONObject gridConf, int attachLimit, int distanceToTaskboards) {
        this.attachLimit = attachLimit;
        dimX = gridConf.getInt("width");
        dimY = gridConf.getInt("height");
        Position.setGridDimensions(dimX, dimY);
        thingsMap = new HashMap<>();
        terrainMap = new Terrain[dimX][dimY];
        for (Terrain[] col : terrainMap) Arrays.fill(col, Terrain.EMPTY);

        // terrain from bitmap
        String mapFilePath = gridConf.optString("file");
        if (!mapFilePath.isBlank()){
            var mapFile = new File(mapFilePath);
            if (mapFile.exists()) {
                try {
                    BufferedImage img = ImageIO.read(mapFile);
                    var width = Math.min(dimX, img.getWidth());
                    var height = Math.min(dimY, img.getHeight());
                    for (int x = 0; x < width; x++) { for (int y = 0; y < height; y++) {
                        setTerrain(Position.of(x, y), terrainColors.getOrDefault(img.getRGB(x, y), Terrain.EMPTY));
                    }}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else Log.log(Log.Level.ERROR, "File " + mapFile.getAbsolutePath() + " not found.");
        }

        // terrain from other instructions
        var instructions = gridConf.getJSONArray("instructions");
        for (var i = 0; i < instructions.length(); i++) {
            var instruction = instructions.optJSONArray(i);
            if (instruction == null) continue;
            switch (instruction.getString(0)) {
                case "line-border":
                    var width = instruction.getInt(1);
                    for (var j = 0; j < width; j++) createLineBorder(j);
                    break;
                case "ragged-border":
                    width = instruction.getInt(1);
                    createRaggedBorder(width);
                    break;
                case "cave":
                    var chanceAlive = instruction.getDouble(1);
                    for (int x = 0; x < dimX; x++) { for (int y = 0; y < dimY; y++) {
                        if (RNG.nextDouble() < chanceAlive) terrainMap[x][y] = Terrain.OBSTACLE;
                    }}
                    var iterations = instruction.getInt(2);
                    var createLimit = instruction.getInt(3);
                    var destroyLimit = instruction.getInt(4);
                    for (var it = 0; it < iterations; it++) {
                        doCaveIteration(createLimit, destroyLimit);
                    }
                    break;
            }
        }

        // goal terrain
        var goalConf = gridConf.getJSONObject("goals");
        var goalNumber = goalConf.getInt("number");
        var goalSize = goalConf.getJSONArray("size");
        var goalSizeMin = goalSize.getInt(0);
        var goalSizeMax = goalSize.getInt(1);
        for (var i = 0; i < goalNumber; i++) {
            var centerPos = findRandomFreePosition();
            var size = RNG.betweenClosed(goalSizeMin, goalSizeMax);
            for (var pos : centerPos.spanArea(size)) setTerrain(pos, Terrain.GOAL);

            for (var pos : centerPos.spanArea(size + distanceToTaskboards))
                blockedForTaskBoards.put(pos.toString(), true);
        }
    }

    public Position findNewTaskboardPosition() {
        var start = findRandomFreePosition();
        var pos = start;
        while (blockedForTaskBoards.getOrDefault(pos.toString(), false)) {
            var x = pos.x + 1;
            var y = pos.y;
            if (x >= dimX) {
                x = 0;
                y += 1;
                if (y >= dimY) {
                    y = 0;
                }
            }
            pos = Position.of(x,y);
            if (pos.equals(start)) {
                Log.log(Log.Level.ERROR, "Grid too small to place all things.");
                return null;
            }
        }
        return pos;
    }

    private void doCaveIteration(int createLimit, int destroyLimit) {
        var newTerrain = new Terrain[dimX][dimY];
        for (var x = 0; x < dimX; x++) { for (var y = 0; y < dimY; y++) {
            var n = countObstacleNeighbours(x,y);
            if (terrainMap[x][y] == Terrain.OBSTACLE) {
                if (n < destroyLimit) newTerrain[x][y] = Terrain.EMPTY;
                else newTerrain[x][y] = Terrain.OBSTACLE;
            }
            else if (terrainMap[x][y] == Terrain.EMPTY) {
                if (n > createLimit) newTerrain[x][y] = Terrain.OBSTACLE;
                else newTerrain[x][y] = Terrain.EMPTY;
            }
            else {
                newTerrain[x][y] = terrainMap[x][y];
            }
        }}
        terrainMap = newTerrain;
    }

    private int countObstacleNeighbours(int cx, int cy) {
        var count = 0;
        for (var x = cx - 1; x <= cx + 1; x++) { for (var y = cy - 1; y <= cy + 1; y++) {
            if (x != cx || y != cy) {
                var pos = Position.wrapped(x, y);
                if (terrainMap[pos.x][pos.y] == Terrain.OBSTACLE) count++;
            }
        }}
        return count;
    }

    /**
     * @param offset distance to the outer map boundaries
     */
    private void createLineBorder(int offset) {
        for (int x = offset; x < dimX - offset; x++) {
            terrainMap[x][offset] = Terrain.OBSTACLE;
            terrainMap[x][dimY - (offset + 1)] = Terrain.OBSTACLE;
        }
        for (int y = offset; y < dimY - offset; y++) {
            terrainMap[offset][y] = Terrain.OBSTACLE;
            terrainMap[dimX - (offset + 1)][y] = Terrain.OBSTACLE;
        }
    }

    private void createRaggedBorder(int width) {
        var currentWidth = width;
        for (var x = 0; x < dimX; x++) {
            currentWidth = Math.max(currentWidth - 1 + RNG.nextInt(3), 1);
            for (var i = 0; i < currentWidth; i++) terrainMap[x][i] = Terrain.OBSTACLE;
        }
        currentWidth = width;
        for (var x = 0; x < dimX; x++) {
            currentWidth = Math.max(currentWidth - 1 + RNG.nextInt(3), 1);
            for (var i = 0; i < currentWidth; i++) terrainMap[x][dimY - (i + 1)] = Terrain.OBSTACLE;
        }
        currentWidth = width;
        for (var y = 0; y < dimY; y++) {
            currentWidth = Math.max(currentWidth - 1 + RNG.nextInt(3), 1);
            for (var i = 0; i < currentWidth; i++) terrainMap[i][y] = Terrain.OBSTACLE;
        }
        currentWidth = width;
        for (var y = 0; y < dimY; y++) {
            currentWidth = Math.max(currentWidth - 1 + RNG.nextInt(3), 1);
            for (var i = 0; i < currentWidth; i++) terrainMap[dimX - (i + 1)][y] = Terrain.OBSTACLE;
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
        insertThing(e);
        return e;
    }

    public Block createBlock(Position xy, String type) {
        if(!isUnblocked(xy)) return null;
        var b = new Block(xy, type);
        insertThing(b);
        return b;
    }

    public void destroyThing(Positionable a) {
        if (a == null) return;
        if (a instanceof Attachable) ((Attachable) a).detachAll();
        var things = thingsMap.get(a.getPosition());
        if (things != null) things.remove(a);
    }

    /**
     * @return a copy of the set of things at the given position
     */
    public Set<Positionable> getThings(Position pos) {
        return new HashSet<>(thingsMap.computeIfAbsent(pos, kPos -> new HashSet<>()));
    }

    private boolean insertThing(Positionable thing) {
        if (outOfBounds(thing.getPosition())) return false;
        thingsMap.computeIfAbsent(thing.getPosition(), pos -> new HashSet<>()).add(thing);
        return true;
    }

    /**
     * @return true if a position is out of the grid's bounds (it could be wrapped back in though).
     */
    public boolean outOfBounds(Position pos) {
        return pos == null || pos.x < 0 || pos.y < 0 || pos.x >= dimX || pos.y >= dimY;
    }

    private void move(Set<Positionable> things, Map<Positionable, Position> newPositions) {
        things.forEach(t -> thingsMap.getOrDefault(t.getPosition(), Collections.emptySet()).remove(t));
        for (Positionable thing : things) {
            var newPos = newPositions.get(thing);
            thing.setPosition(newPos);
            insertThing(thing);
        }
    }

    /**
     * Moves an Attachable to a given position.
     * Only works if target is free and attachable has nothing attached.
     */
    public void moveWithoutAttachments(Attachable a, Position pos) {
        if(isUnblocked(pos) && a.getAttachments().isEmpty()) {
            destroyThing(a);
            a.setPosition(pos);
            insertThing(a);
        }
    }

    public boolean attach(Attachable a1, Attachable a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getPosition().distanceTo(a2.getPosition()) != 1) return false;

        var attachments = a1.collectAllAttachments();
        attachments.addAll(a2.collectAllAttachments());
        if (attachments.size() > attachLimit) return false;

        a1.attach(a2);
        return true;
    }

    public boolean detachNeighbors(Attachable a1, Attachable a2) {
        if (a1 == null || a2 ==  null) return false;
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
        var things = new HashSet<Positionable>(anchor.collectAllAttachments());
        var newPositions = canMove(things, direction, distance);
        if (newPositions == null) return false;
        move(things, newPositions);
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
    private Map<Positionable, Position> canRotate(Attachable anchor, boolean clockwise) {
        var attachments = new HashSet<Positionable>(anchor.collectAllAttachments());
        if(attachments.stream().anyMatch(a -> a != anchor && a instanceof Entity)) return null;
        var newPositions = new HashMap<Positionable, Position>();
        for (Positionable a : attachments) {
            var rotatedPos = a.getPosition().rotated90(anchor.getPosition(), clockwise);
            if(!isUnblocked(rotatedPos, attachments)) return null;
            newPositions.put(a, rotatedPos);
        }
        return newPositions;
    }

    private Map<Positionable, Position> canMove(Set<Positionable> things, String direction, int distance) {
        var newPositions = new HashMap<Positionable, Position>();
        for (Positionable thing : things) {
            for (int i = 1; i <= distance; i++) {
                var newPos = thing.getPosition().moved(direction, i);
                if(!isUnblocked(newPos, things)) return null;
            }
            newPositions.put(thing, thing.getPosition().moved(direction, distance));
        }
        return newPositions;
    }

    public Position findRandomFreePosition() {
        int x = RNG.nextInt(dimX);
        int y = RNG.nextInt(dimY);
        final int startX = x;
        final int startY = y;
        while (!isUnblocked(Position.of(x,y)) || terrainMap[x][y] != Terrain.EMPTY) {
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
    
    public ArrayList<Position> findRandomFreeClusterPosition(int clusterSize) {
        ArrayList<Position> cluster = new ArrayList<Position>();
        int x = RNG.nextInt(dimX);
        int y = RNG.nextInt(dimY);
        final int radius = (int) (Math.log(clusterSize)/Math.log(2)); 
        final int startX = x;
        final int startY = y;
        
        while (!isUnblocked(Position.of(x,y)) || terrainMap[x][y] != Terrain.EMPTY || !hasEnoughFreeSpots(Position.of(x,y),radius,clusterSize)) {
            if (++x >= dimX) {
                x = 0;
                if (++y >= dimY) y = 0;
            }
            if (x == startX && y == startY) {
                Log.log(Log.Level.ERROR, "No free position");
                return null;
            }
        }

        Position.of(x, y).spanArea(radius).forEach((p) -> {if(cluster.size() == clusterSize) return;  if(getTerrain(p) == Terrain.EMPTY) cluster.add(p);});

        return cluster;
    }
    private boolean hasEnoughFreeSpots(Position origin, int radius, int numberPositionNeeded){
        int freeSpots = 0;
        for (Position p : origin.spanArea(radius)) 
            if (getTerrain(p) == Terrain.EMPTY)
                freeSpots++;
        return freeSpots >= numberPositionNeeded;
    }

    public Position findRandomFreePosition(Position center, int maxDistance) {
        for (var i = 0; i < 50; i++) {
            int x = center.x;
            int y = center.y;
            int dx = RNG.nextInt(maxDistance + 1);
            int dy = RNG.nextInt(maxDistance + 1);
            x += RNG.nextDouble() < .5? dx : -dx;
            y += RNG.nextDouble() < .5? dy : -dy;
            var target = Position.of(x, y);
            if (isUnblocked(target)) return target;
        }
        return null;
    }

    /**
     * @return true if the cell is in the grid and there is no other collidable and the terrain is not an obstacle.
     */
    public boolean isUnblocked(Position xy) {
        return isUnblocked(xy, Collections.emptySet());
    }

    private boolean isUnblocked(Position xy, Set<Positionable> excludedObjects) {
        if (outOfBounds(xy)) xy = xy.wrapped();
        if (terrainMap[xy.x][xy.y] == Terrain.OBSTACLE) return false;

        return getThings(xy).stream().noneMatch(t -> t instanceof Attachable && !excludedObjects.contains(t));
    }

    public void setTerrain(Position pos, Terrain terrainType) {
        if (outOfBounds(pos)) pos = pos.wrapped();
        terrainMap[pos.x][pos.y] = terrainType;
    }

    public Terrain getTerrain(Position pos) {
        if (outOfBounds(pos)) pos = pos.wrapped();
        return terrainMap[pos.x][pos.y];
    }

    public void createMarker(Position position, Marker.Type type) {
        if (outOfBounds(position)) position = position.wrapped();
        var marker = new Marker(position, type);
        markers.add(marker);
        insertThing(marker);
    }

    public void deleteMarkers() {
        markers.forEach(this::destroyThing);
        markers.clear();
    }

    public Position getRandomPosition() {
        return Position.of(RNG.nextInt(dimX), RNG.nextInt(dimY));
    }
}
