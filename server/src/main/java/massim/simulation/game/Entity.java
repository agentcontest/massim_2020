package massim.simulation.game;

/**
 * A controllable entity in the simulation.
 */
public class Entity {

    private Role role;

    public Entity(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }
}
