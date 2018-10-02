package massim.scenario;

/**
 * A controllable entity in the scenario.
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
