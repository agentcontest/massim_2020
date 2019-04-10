package massim.simulation.game;

/**
 * A controllable entity in the simulation.
 */
public class Entity {

    private int rotation = 0;

    private boolean rotate(boolean clockwise) {
        if (clockwise) rotation = (rotation + 1) & 3;
        else rotation = (rotation - 1) & 3;

        return true; // TODO check if rotation possible with attachments
    }
}
