package massim.protocol.messages;

import org.json.JSONObject;

public class RequestActionMessage extends Message {

    public long id;
    public long deadline;

    public RequestActionMessage(long time, long id, long deadline) {
        super(time);
        this.id = id;
        this.deadline = deadline;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_REQUEST_ACTION;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.append("id", id);
        content.append("deadline", deadline);
        return content;
    }
}
