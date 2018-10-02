package massim.protocol.messages.scenario;

import massim.protocol.messages.SimStartMessage;
import org.json.JSONObject;

public class InitialPercept extends SimStartMessage {

    public InitialPercept(JSONObject content) {
        super(content);
    }

    @Override
    public JSONObject makePercept() {
        //TODO
        return new JSONObject();
    }
}
