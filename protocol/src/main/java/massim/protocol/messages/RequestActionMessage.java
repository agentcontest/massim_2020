package massim.protocol.messages;

import org.json.JSONObject;

/**
 * Should be sub-classed for request-action messages including an agent's current percepts.
 */
public abstract class RequestActionMessage extends Message {

    private long time;
    private long id;
    private long deadline;
    private int step;

    public RequestActionMessage(JSONObject content) {
        this.time = content.optLong("time");
        this.id = content.optLong("id", -1);
        this.deadline = content.optLong("deadline", -1);
        this.step = content.optInt("step", -1);
    }

    public RequestActionMessage(long time, long id, long deadline, int step) {
        this.time = time;
        this.id = id;
        this.deadline = deadline;
        this.step = step;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_REQUEST_ACTION;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.put("id", id);
        content.put("time" , time);
        content.put("deadline", deadline);
        content.put("step", step);
        content.put("percept", makePercept());
        return content;
    }

    /**
     * Create the JSON representation of the percept part.
     * Will be appended under the "percept" key of the "content" object.
     */
    public abstract JSONObject makePercept();

    public void updateIdAndDeadline(long id, long deadline) {
        this.id = id;
        this.deadline = deadline;
    }

    public long getTime() {
        return time;
    }

    public long getId() {
        return id;
    }

    public long getDeadline() {
        return deadline;
    }

    public int getStep() {
        return step;
    }
}
