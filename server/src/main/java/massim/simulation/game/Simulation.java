package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.simulation.AbstractSimulation;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Simulation extends AbstractSimulation {

    @Override
    public Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams) {
        return new HashMap<>();
    }

    @Override
    public Map<String, RequestActionMessage> preStep(int stepNo) {
        return new HashMap<>();
    }

    @Override
    public void step(int stepNo, Map<String, ActionMessage> actionMap) {

    }

    @Override
    public Map<String, SimEndMessage> finish() {
        return new HashMap<>();
    }

    @Override
    public JSONObject getResult() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public JSONObject getSnapshot() {
        return new JSONObject();
    }

    @Override
    public JSONObject getStaticData() {
        return new JSONObject();
    }

    @Override
    public void handleCommand(String[] command) {

    }
}
