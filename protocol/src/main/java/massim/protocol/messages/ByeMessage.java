package massim.protocol.messages;

import org.json.JSONObject;

public class ByeMessage extends Message {

    public ByeMessage(long time) {
        super(time);
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_BYE;
    }

    @Override
    public JSONObject makeContent() {
        return new JSONObject();
    }
}
