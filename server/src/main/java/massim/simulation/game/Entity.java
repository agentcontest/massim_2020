package massim.simulation.game;

import massim.protocol.messages.ActionMessage;
import massim.simulation.game.environment.Attachable;
import massim.simulation.game.environment.Position;

import java.util.Collections;
import java.util.List;


/**
 * A controllable entity in the simulation.
 */
public class Entity extends Attachable {

    private String agentName;
    private String lastAction = "";
    private List<String> lastActionParams = Collections.emptyList();
    private String lastActionResult = "";

    public Entity(Position xy, String agentName) {
        super(xy);
        this.agentName = agentName;
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
}
