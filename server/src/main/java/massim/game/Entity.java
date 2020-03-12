package massim.game;

import massim.game.environment.Attachable;
import massim.game.environment.Task;
import massim.protocol.data.Position;
import massim.protocol.data.Thing;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.scenario.Actions;

import java.util.Collections;
import java.util.List;


/**
 * A controllable entity in the simulation.
 */
public class Entity extends Attachable {

    static int maxEnergy = 0;
    static int clearEnergyCost = 0;
    static int disableDuration = 0;

    private String agentName;
    private String teamName;
    private String lastAction = "";
    private List<String> lastActionParams = Collections.emptyList();
    private String lastActionResult = "";

    private int vision = 5;
    private int energy;

    private int previousClearStep = -1;
    private Position previousClearPosition = Position.of(-1, -1);
    private int clearCounter = 0;

    private int disabled = 0;

    private Task acceptedTask;

    public Entity(Position xy, String agentName, String teamName) {
        super(xy);
        this.agentName = agentName;
        this.teamName = teamName;
        this.energy = maxEnergy;
    }

    @Override
    public Thing toPercept(Position origin) {
        Position localPosition = getPosition().relativeTo(origin);
        return new Thing(localPosition.x, localPosition.y, Thing.TYPE_ENTITY, teamName);
    }

    /**
     * recharge and repair
     */
    void preStep() {
        disabled--;
        energy = Math.min(energy + 1, maxEnergy);
    }

    String getTeamName() {
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
        this.lastActionResult = Actions.RESULT_UNPROCESSED;
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

    void recordClearAction(int step, Position position) {
        previousClearPosition = position;
        previousClearStep = step;
    }

    int getPreviousClearStep() {
        return previousClearStep;
    }

    Position getPreviousClearPosition() {
        return previousClearPosition;
    }

    int incrementClearCounter() {
        return ++clearCounter;
    }

    void resetClearCounter() {
        clearCounter = 0;
    }

    void disable() {
        disabled = disableDuration;
        detachAll();
    }

    boolean isDisabled() {
        return disabled > 0;
    }

    int getEnergy() {
        return energy;
    }

    void consumeClearEnergy() {
        energy -= clearEnergyCost;
    }

    void acceptTask(Task t) {
        this.acceptedTask = t;
    }

    String getTask() {
        if (acceptedTask == null) return "";
        return acceptedTask.getName();
    }
}
