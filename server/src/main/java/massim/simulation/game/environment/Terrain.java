package massim.simulation.game.environment;

public enum Terrain {
    EMPTY("empty"),
    GOAL("goal"),
    OBSTACLE("obstacle");

    private String name;

    Terrain(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
