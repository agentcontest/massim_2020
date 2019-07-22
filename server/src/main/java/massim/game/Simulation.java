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

    public Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams) {
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

    public JSONObject getStaticData() {
        var cells = new JSONArray();
        for (int y = 0; y < this.state.getGrid().getDimY(); y++) {
            var row = new JSONArray();
            for (int x = 0; x < this.state.getGrid().getDimX(); x++) {
                row.put(this.state.getGrid().getTerrain(Position.of(x, y)).id);
            }
            cells.put(row);
        }

        var grid = new JSONObject();
        grid.put("width", this.state.getGrid().getDimX());
        grid.put("height", this.state.getGrid().getDimY());
        grid.put("cells", cells);

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
        world.put("grid", grid);
        world.put("teams", teams);
        world.put("blockTypes", this.state.getBlockTypes());
        return world;
    }

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
                entity.setLastActionResult(RESULT_F_RANDOM);
            }
        }
        for (Entity entity : entities) {
            if (!entity.getLastActionResult().equals(RESULT_UNPROCESSED)) continue;
            List<String> params = entity.getLastActionParams();
            switch(entity.getLastAction()) {

                case NO_ACTION:
                    entity.setLastActionResult(RESULT_SUCCESS);
                    continue;

                case MOVE:
                    String direction = getStringParam(params, 0);
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
                    String result = state.handleConnectAction(entity, Position.of(x, y), partnerEntity, Position.of(px, py));
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
                    String taskName = getStringParam(params, 0);
                    entity.setLastActionResult(state.handleSubmitAction(entity, taskName));
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

    private String getStringParam(List<String> params, int index) {
        if (index >= params.size()) return null;
        try {
            return params.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
