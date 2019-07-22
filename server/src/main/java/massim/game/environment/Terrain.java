package massim.game.environment;

public enum Terrain {
    EMPTY(0, "empty"),
    GOAL(1, "goal"),
    OBSTACLE(2, "obstacle");

    public final int id;
    public final String name;

    Terrain(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
