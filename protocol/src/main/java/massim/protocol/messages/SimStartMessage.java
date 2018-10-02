package massim.protocol.messages;

import org.json.JSONObject;

public class SimStartMessage extends Message {

    public SimStartMessage(long time) {
        super(time);
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_SIM_START;
    }

    @Override
    public JSONObject makeContent() {
        return new JSONObject();
    }
}
