package massim.protocol.messages.scenario;

import massim.protocol.messages.SimStartMessage;
import org.json.JSONObject;

public class InitialPercept extends SimStartMessage {

    public InitialPercept(JSONObject content) {
        super(content);
        parsePercept(content.getJSONObject("percept"));
    }

    @Override
    public JSONObject makePercept() {
        JSONObject percept = new JSONObject();
        // TODO
        return percept;
    }

    private void parsePercept(JSONObject percept) {
        //TODO
    }
}
