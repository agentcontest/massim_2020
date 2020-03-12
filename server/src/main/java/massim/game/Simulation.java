package massim.game;

import massim.config.TeamConfig;
import massim.protocol.data.Position;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.game.environment.Grid;
import massim.util.RNG;
import massim.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static massim.protocol.messages.scenario.Actions.*;

public class Simulation {

    private String name;
    private GameState state;
    private int steps;

    public Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams) {
        this.steps = steps;
        this.state = new GameState(config, matchTeams);
        this.name = System.currentTimeMillis() + "_" + matchTeams.stream()
                .map(TeamConfig::getName)
                .collect(Collectors.joining("_"));
        return state.getInitialPercepts(steps);
    }

    public Map<String, RequestActionMessage> preStep(int step) {
        return state.prepareStep(step);
    }

    public void step(int stepNo, Map<String, ActionMessage> actionMap) {
        handleActions(actionMap);
    }

    public Map<String, SimEndMessage> finish() {
        return state.getFinalPercepts();
    }

    public JSONObject getResult() {
        return state.getResult();
    }

    public String getName() {
        return name;
    }

    public JSONObject getSnapshot() {
        return state.takeSnapshot();
    }

    public JSONObject getStatusSnapshot() {
        JSONObject snapshot = state.takeStatusSnapshot();
        snapshot.put("sim", name);
        snapshot.put("steps", steps);
        return snapshot;
    }

    public JSONObject getStaticData() {
        var grid = new JSONObject();
        grid.put("width", this.state.getGrid().getDimX());
        grid.put("height", this.state.getGrid().getDimY());

        var teams = new JSONObject();
        for (var entry: this.state.getTeams().entrySet()) {
            var team = new JSONObject();
            team.put("name", entry.getValue().getName());
            teams.put(entry.getKey(), team);
        }

        var blockTypes = new JSONArray();
        for (var type: this.state.getBlockTypes()) {
            blockTypes.put(type);
        }

        var world = new JSONObject();
        world.put("sim", name);
        world.put("grid", grid);
        world.put("teams", teams);
        world.put("blockTypes", this.state.getBlockTypes());
        world.put("steps", steps);
        return world;
    }

    public void handleCommand(String[] command) {}

    /**
     * Executes all actions in random order.
     */
    private void handleActions(Map<String, ActionMessage> actions) {
        var entities = actions.keySet().stream().map(ag -> state.getEntityByName(ag)).collect(Collectors.toList());
        RNG.shuffle(entities);
        for (Entity entity : entities) {
            var action = actions.get(entity.getAgentName());
            entity.setNewAction(action);
            if (entity.isDisabled()) {
                entity.setLastActionResult(RESULT_F_STATUS);
            }
            else if (RNG.nextInt(100) < state.getRandomFail()) {
                entity.setLastActionResult(RESULT_F_RANDOM);
            }
        }
        for (Entity entity : entities) {
            if (!entity.getLastActionResult().equals(RESULT_UNPROCESSED)) continue;
            var params = entity.getLastActionParams();
            switch(entity.getLastAction()) {

                case NO_ACTION:
                case SKIP:
                    entity.setLastActionResult(RESULT_SUCCESS);
                    continue;

                case MOVE:
                    var direction = getStringParam(params, 0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        entity.setLastActionResult(state.handleMoveAction(entity, direction));
                    }
                    continue;

                case ATTACH:
                    direction = getStringParam(params, 0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        entity.setLastActionResult(state.handleAttachAction(entity, direction));
                    }
                    continue;

                case DETACH:
                    direction = getStringParam(params, 0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        entity.setLastActionResult(state.handleDetachAction(entity, direction));
                    }
                    continue;

                case ROTATE:
                    direction = getStringParam(params, 0);
                    if (!Grid.ROTATION_DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    entity.setLastActionResult(state.handleRotateAction(entity, "cw".equals(direction)));
                    continue;

                case CONNECT:
                    var partnerEntityName = getStringParam(params, 0);
                    var partnerEntity = state.getEntityByName(partnerEntityName);
                    var x = getIntParam(params, 1);
                    var y = getIntParam(params, 2);
                    if (partnerEntity == null || x == null || y == null) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    var partnerAction = actions.get(partnerEntityName);
                    if (partnerAction == null) {
                        entity.setLastActionResult(RESULT_F_PARTNER);
                        continue;
                    }
                    var partnerParams = partnerAction.getParams();
                    var px = getIntParam(partnerParams, 1);
                    var py = getIntParam(partnerParams, 2);
                    if (!partnerEntity.getLastAction().equals(CONNECT)
                            || !partnerEntity.getLastActionResult().equals(RESULT_UNPROCESSED)
                            || !entity.getAgentName().equals(getStringParam(partnerParams, 0))) {
                        entity.setLastActionResult(RESULT_F_PARTNER);
                        continue;
                    }
                    if (px == null || py == null) {
                        entity.setLastActionResult(RESULT_F_PARTNER);
                        partnerEntity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    var result = state.handleConnectAction(entity, Position.of(x, y), partnerEntity, Position.of(px, py));
                    entity.setLastActionResult(result);
                    partnerEntity.setLastActionResult(result);
                    continue;

                case REQUEST:
                    direction = getStringParam(params, 0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        entity.setLastActionResult(state.handleRequestAction(entity, direction));
                    }
                    continue;

                case SUBMIT:
                    var taskName = getStringParam(params, 0);
                    entity.setLastActionResult(state.handleSubmitAction(entity, taskName));
                    continue;

                case CLEAR:
                    x = getIntParam(params, 0);
                    y = getIntParam(params, 1);
                    if (x == null || y == null) entity.setLastActionResult(RESULT_F_PARAMETER);
                    else {
                        entity.setLastActionResult(state.handleClearAction(entity, Position.of(x, y)));
                    }
                    continue;

                case DISCONNECT:
                    var x1 = getIntParam(params, 0);
                    var y1 = getIntParam(params, 1);
                    var x2 = getIntParam(params, 2);
                    var y2 = getIntParam(params, 3);
                    if (x1 == null || y1 == null || x2 == null || y2 == null) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    entity.setLastActionResult(
                            state.handleDisconnectAction(entity, Position.of(x1, y1), Position.of(x2, y2)));
                    continue;

                case ACCEPT:
                    var task = getStringParam(params, 0);
                    entity.setLastActionResult(state.handleAcceptAction(entity, task));
                    continue;

                default:
                    entity.setLastActionResult(UNKNOWN_ACTION);
            }
        }
    }

    private Integer getIntParam(List<String> params, int index) {
        if (index >= params.size()) return null;
        return Util.tryParseInt(params.get(index));
    }

    /**
     * @return the string parameter at the given index or null if there is no such parameter
     */
    private String getStringParam(List<String> params, int index) {
        if (index >= params.size()) return null;
        try {
            return params.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
