package massim.scenario;

/**
 * Stores some team info.
 */
public class TeamState {

    private long score = 0;

    private String name;

    TeamState(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void addScore(int points) {
        score += points;
    }

    public long getScore() {
        return score;
    }
}
