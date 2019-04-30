package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.data.Position;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.simulation.AbstractSimulation;
import massim.simulation.game.environment.Grid;
import massim.util.RNG;
import massim.util.Util;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static massim.protocol.messages.scenario.Actions.*;

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
        return state.prepareStep(step);
    }

    @Override
    public void step(int stepNo, Map<String, ActionMessage> actionMap) {
        handleActions(actionMap);
    }

    @Override
    public Map<String, SimEndMessage> finish() {
        return state.getFinalPercepts();
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
        var cells = new JSONArray();
        for (int y = 0; y < state.getGrid().getDimY(); y++) {
            var row = new JSONArray();
            for (int x = 0; x < state.getGrid().getDimX(); x++) {
                row.put(state.getGrid().getTerrain(Position.of(x, y)).id);
            }
            cells.put(row);
        }

        var grid = new JSONObject();
        grid.put("width", state.getGrid().getDimX());
        grid.put("height", state.getGrid().getDimY());
        grid.put("cells", cells);

        var world = new JSONObject();
        world.put("grid", grid);
        return world;
    }

    @Override
    public void handleCommand(String[] command) {}

    /**
     * Executes all actions in random order.
     */
    private void handleActions(Map<String, ActionMessage> actions) {
        List<Entity> entities = actions.keySet().stream().map(ag -> state.getEntityByName(ag)).collect(Collectors.toList());
        Map<Entity, Position> connections = new HashMap<>();
        RNG.shuffle(entities);
        for (Entity entity : entities) {
            entity.setNewAction(actions.get(entity.getAgentName()));
            if (RNG.nextInt(100) < state.getRandomFail()) {
                entity.setLastActionResult(RESULT_F_RANDOM);
            }
        }
        for (Entity entity : entities) {
            if (entity.getLastActionResult().equals(RESULT_F_RANDOM)) continue;
            List<String> params = entity.getLastActionParams();
            switch(entity.getLastAction()) {

                case NO_ACTION:
                    entity.setLastActionResult(RESULT_SUCCESS);
                    continue;

                case MOVE:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    String direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    } else {
                        if (state.move(entity, direction)) {
                            entity.setLastActionResult(RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(RESULT_F_PATH);
                        }
                    }
                    continue;

                case ATTACH:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        if (state.attach(entity, direction)){
                            entity.setLastActionResult(RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(RESULT_F);
                        }
                    }
                    continue;

                case DETACH:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        if (state.detach(entity, direction)){
                            entity.setLastActionResult(RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(RESULT_F);
                        }
                    }
                    continue;

                case ROTATE:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!direction.equalsIgnoreCase("cw") && !direction.equalsIgnoreCase("ccw")) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    boolean clockwise = direction.equals("cw");
                    if (state.rotate(entity, clockwise)) {
                        entity.setLastActionResult(RESULT_SUCCESS);
                    }
                    else{
                        entity.setLastActionResult(RESULT_F);
                    }
                    continue;

                case CONNECT:
                    if (params.size() != 3) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    var entityName = params.get(0);
                    var x = getIntParam(params, 1);
                    var y = getIntParam(params, 2);
                    var partnerEntity = state.getEntityByName(entityName);
                    if (partnerEntity == null || x == null || y == null) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    if (!partnerEntity.getLastAction().equals(CONNECT)
                            || partnerEntity.getLastActionResult().equals(RESULT_F_RANDOM)) {
                        entity.setLastActionResult(RESULT_F_PARTNER);
                        continue;
                    }
                    if (partnerEntity.getLastActionResult().equals(RESULT_PENDING)) {
                        if (state.connectEntities(entity, Position.of(x, y), partnerEntity, connections.get(partnerEntity))) {
                            entity.setLastActionResult(RESULT_SUCCESS);
                            partnerEntity.setLastActionResult(RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(RESULT_F);
                            partnerEntity.setLastActionResult(RESULT_F);
                        }
                    } else { // handle action when it's the other agent's turn
                        connections.put(entity, Position.of(x,y));
                        entity.setLastActionResult(RESULT_PENDING);
                    }
                    continue;

                case REQUEST:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    direction = params.get(0);
                    if (!Grid.DIRECTIONS.contains(direction)) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                    } else {
                        if (state.requestBlock(entity, direction)){
                            entity.setLastActionResult(RESULT_SUCCESS);
                        }
                        else {
                            entity.setLastActionResult(RESULT_F);
                        }
                    }
                    continue;

                case SUBMIT:
                    if (params.size() != 1) {
                        entity.setLastActionResult(RESULT_F_PARAMETER);
                        continue;
                    }
                    String taskName = params.get(0);
                    if (state.submitTask(entity, taskName)){
                        entity.setLastActionResult(RESULT_SUCCESS);
                    }
                    else {
                        entity.setLastActionResult(RESULT_F);
                    }
                    continue;
                default:
                    entity.setLastActionResult(UNKNOWN_ACTION);
            }
        }
    }

    private Integer getIntParam(List<String> params, int index) {
        return Util.tryParseInt(params.get(index));
    }
}
