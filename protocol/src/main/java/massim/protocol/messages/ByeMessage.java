package massim.protocol.messages;

import org.json.JSONObject;

public class ByeMessage extends Message {

    private long time;

    public ByeMessage(long time) {
        this.time = time;
    }

    public ByeMessage(JSONObject content) {
        this.time = content.optLong("time");
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_BYE;
    }

    @Override
    public JSONObject makeContent() {
        return new JSONObject();
    }

    public long getTime() {
        return time;
    }
}
