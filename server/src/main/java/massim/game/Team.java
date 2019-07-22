package massim.game;

/**
 * Stores some team info.
 */
public class Team {

    private long score = 0;

    private String name;

    Team(String name){
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
