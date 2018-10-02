package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.simulation.AbstractSimulation;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public class Simulation extends AbstractSimulation {

    @Override
    public Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams) {
        return null;
    }

    @Override
    public Map<String, RequestActionMessage> preStep(int stepNo) {
        return null;
    }

    @Override
    public void step(int stepNo, Map<String, ActionMessage> actionMap) {

    }

    @Override
    public Map<String, SimEndMessage> finish() {
        return null;
    }

    @Override
    public JSONObject getResult() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public JSONObject getSnapshot() {
        return null;
    }

    @Override
    public JSONObject getStaticData() {
        return null;
    }

    @Override
    public void handleCommand(String[] command) {

    }
}
