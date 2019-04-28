package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.simulation.AbstractSimulation;
import massim.simulation.game.environment.Grid;
import massim.util.RNG;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Simulation extends AbstractSimulation {

    private String name;
    private GameState state;

    @Override
    public Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams) {
        this.state = new GameState(config, matchTeams);
        this.name = System.currentTimeMillis() + "_" + matchTeams.stream()
                .map(TeamConfig::getName)
                .collect(Collectors.joining("_"));
        return state.getInitialPercepts(steps);
    }

    @Override
    public Map<String, RequestActionMessage> preStep(int step) {
        return state.prepare(step);
    }

    @Override
    public void step(int stepNo, Map<String, ActionMessage> actionMap) {
        handleActions(actionMap);
    }

    @Override
    public Map<String, SimEndMessage> finish() {
        // TODO create sim end percepts
        return new HashMap<>();
    }

    @Override
    public JSONObject getResult() {
        return state.getResult();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JSONObject getSnapshot() {
        return state.takeSnapshot();
    }

    @Override
    public JSONObject getStaticData() {
        // TODO
        return new JSONObject();
    }

    @Override
    public void handleCommand(String[] command) {}

    /**
     * Executes all actions in random order.
     */
    private void handleActions(Map<String, ActionMessage> actions) {
        List<Entity> entities = actions.keySet().stream().map(ag -> state.getEntityByName(ag)).collect(Collectors.toList());
        RNG.shuffle(entities);
        for (Entity entity : entities) {
            entity.setNewAction(actions.get(entity.getAgentName()));
            if (RNG.nextInt(100) < state.getRandomFail()) {
                entity.setLastActionResult(ActionMessage.RESULT_F_RANDOM);
            }
        }
        for (Entity entity : entities) {
            if (entity.getLastActionResult().equals(ActionMessage.RESULT_F_RANDOM)) continue;
            List<String> params = entity.getLastActionParams();
            switch(entity.getLastAction()) {
                case "moved":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    String direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    } else {
                        if (state.move(entity, direction)) {
                            entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(ActionMessage.RESULT_F_PATH);
                        }
                    }
                    continue;

                case "attach":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                    } else {
                        if (state.attach(entity, direction)){
                            entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(ActionMessage.RESULT_F);
                        }
                    }
                    continue;

                case "detach":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                    } else {
                        if (state.detach(entity, direction)){
                            entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(ActionMessage.RESULT_F);
                        }
                    }
                    continue;

                case "rotate":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!direction.equalsIgnoreCase("cw") && !direction.equalsIgnoreCase("ccw")) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    boolean clockwise = direction.equals("cw");
                    if (state.rotate(entity, clockwise)) {
                        entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                    }
                    else{
                        entity.setLastActionResult(ActionMessage.RESULT_F);
                    }
                    continue;

                case "connect":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    String entityName = params.get(0);
                    Entity partnerEntity = state.getEntityByName(entityName);
                    if (partnerEntity == null) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    if (!partnerEntity.getLastAction().equals("connect")
                            || partnerEntity.getLastActionResult().equals(ActionMessage.RESULT_F_RANDOM)) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARTNER);
                        continue;
                    }
                    if (partnerEntity.getLastActionResult().equals(ActionMessage.RESULT_PENDING)) {
                        if (state.connectEntities(entity, partnerEntity)) {
                            entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                            partnerEntity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(ActionMessage.RESULT_F_PARTNER);
                        }
                    } else { // handle action when it's the other agent's turn
                        entity.setLastActionResult(ActionMessage.RESULT_PENDING);
                    }
                    continue;

                case "request":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                    } else {
                        if (state.requestBlock(entity, direction)){
                            entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(ActionMessage.RESULT_F);
                        }
                    }
                    continue;

                case "submit":
                    if (params.size() != 1) {
                        entity.setLastActionResult(ActionMessage.RESULT_F_PARAMETER);
                        continue;
                    }
                    String taskName = params.get(0);
                    if (state.submitTask(entity, taskName)){
                        entity.setLastActionResult(ActionMessage.RESULT_SUCCESS);
                    }
                    else {
                        entity.setLastActionResult(ActionMessage.RESULT_F);
                    }
                    continue;
                default:
                    entity.setLastActionResult(ActionMessage.UNKNOWN_ACTION);
            }
        }
    }
}
