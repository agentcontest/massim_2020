package massim.protocol.messages;

import org.json.JSONObject;

public abstract class SimStartMessage extends Message {

    private long time;

    public SimStartMessage(JSONObject content) {
        this.time = content.optLong("time");
    }

    public SimStartMessage(long time) {
        this.time = time;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_SIM_START;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.put("time", time);
        content.put("percept", makePercept());
        return content;
    }

    /**
     * Create the JSON representation of the percept part.
     * Will be appended under the "percept" key of the "content" object.
     */
    public abstract JSONObject makePercept();

    public long getTime() {
        return time;
    }
}
