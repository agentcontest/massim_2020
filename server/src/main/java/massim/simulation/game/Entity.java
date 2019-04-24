package massim.simulation.game;

import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.scenario.data.Thing;
import massim.simulation.game.environment.Attachable;
import massim.simulation.game.environment.Position;

import java.util.Collections;
import java.util.List;


/**
 * A controllable entity in the simulation.
 */
public class Entity extends Attachable {

    private String agentName;
    private String teamName;
    private String lastAction = "";
    private List<String> lastActionParams = Collections.emptyList();
    private String lastActionResult = "";

    private int vision = 5;

    public Entity(Position xy, String agentName, String teamName) {
        super(xy);
        this.agentName = agentName;
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }

    void setLastActionResult(String result) {
        this.lastActionResult = result;
    }

    String getAgentName() {
        return agentName;
    }

    void setNewAction(ActionMessage action) {
        this.lastAction = action.getActionType();
        this.lastActionResult = ActionMessage.RESULT_UNPROCESSED;
        this.lastActionParams = action.getParams();
    }

    String getLastAction() {
        return lastAction;
    }

    List<String> getLastActionParams() {
        return lastActionParams;
    }

    String getLastActionResult() {
        return lastActionResult;
    }

    int getVision() {
        return vision;
    }

    @Override
    public Thing toPercept() {
        return new Thing(getPosition().x, getPosition().y, Thing.TYPE_ENTITY, "");
    }
}
