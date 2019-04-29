package massim.simulation.game.environment;

public enum Terrain {
    EMPTY(0),
    GOAL(1);

    public final int id;

    Terrain(int id) {
        this.id = id;
    }
}
