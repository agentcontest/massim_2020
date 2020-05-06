package massim.protocol.messages.scenario;

import massim.protocol.messages.SimStartMessage;
import org.json.JSONObject;

public class InitialPercept extends SimStartMessage {

    public String agentName;
    public String teamName;
    public int teamSize;
    public int steps;
    public int vision;

    public InitialPercept(JSONObject content) {
        super(content);
        parsePercept(content.getJSONObject("percept"));
    }

    public InitialPercept(String agentName, String teamName, int teamSize, int steps, int vision) {
        super(System.currentTimeMillis());
        this.agentName = agentName;
        this.teamName = teamName;
        this.teamSize = teamSize;
        this.steps = steps;
        this.vision = vision;
    }

    @Override
    public JSONObject makePercept() {
        JSONObject percept = new JSONObject();
        percept.put("name", agentName);
        percept.put("team", teamName);
        percept.put("teamSize", teamSize);
        percept.put("steps", steps);
        percept.put("vision", vision);
        return percept;
    }

    private void parsePercept(JSONObject percept) {
        agentName = percept.getString("name");
        teamName = percept.getString("team");
        steps = percept.getInt("steps");
        teamSize = percept.getInt("teamSize");
        vision = percept.getInt("vision");
    }
}
