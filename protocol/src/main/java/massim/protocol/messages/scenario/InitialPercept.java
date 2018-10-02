package massim.protocol.messages.scenario;

import massim.protocol.messages.SimStartMessage;
import org.json.JSONObject;

public class InitialPercept extends SimStartMessage {

    public InitialPercept(long time, JSONObject content) {
        super(time);
    }
}
