package massim.protocol.messages;

import org.json.JSONObject;

public class StatusRequestMessage extends Message {

    public StatusRequestMessage() {}

    @Override
    public String getMessageType() {
        return Message.TYPE_STATUS_REQUEST;
    }

    @Override
    public JSONObject makeContent() {
        return new JSONObject();
    }
}
