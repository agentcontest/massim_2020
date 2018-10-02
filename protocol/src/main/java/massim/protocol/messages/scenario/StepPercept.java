package massim.protocol.messages.scenario;

import massim.protocol.messages.RequestActionMessage;
import org.json.JSONObject;

public class StepPercept extends RequestActionMessage {

    public StepPercept(long time, JSONObject content) {
        super(time, content);
    }

    @Override
    public JSONObject makePercept() {
        // TODO
        return new JSONObject();
    }
}
