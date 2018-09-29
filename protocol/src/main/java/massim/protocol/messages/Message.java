package massim.protocol.messages;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class Message {

    public final static String TYPE_REQUEST_ACTION = "request-action";
    public final static String TYPE_ACTION = "action";
    public final static String TYPE_AUTH_REQUEST = "auth-request";
    public final static String TYPE_AUTH_RESPONSE = "auth-response";
    public final static String TYPE_SIM_START = "sim-start";
    public final static String TYPE_SIM_END = "sim-end";
    public final static String TYPE_BYE = "bye";

    private long time;

    public abstract String getMessageType();

    public abstract JSONObject makeContent();

    public Message(long time) {
        this.time = time;
    }

    public JSONObject toJson() {
        JSONObject message = new JSONObject();
        message.append("time", time);
        message.append("type", getMessageType());
        message.append("content", makeContent());
        return message;
    }

    public static Message buildFromJson(JSONObject src) {
        long time = src.optLong("time", -1);
        String type = src.optString("type");
        JSONObject content = src.optJSONObject("content");
        if(content == null) return null;
        switch(type) {
            case TYPE_ACTION:
                String actionType = content.optString("type", "no-type");
                long id = content.optLong("id", -1);
                JSONArray p = content.optJSONArray("p");
                List<String> params = new ArrayList<>();
                for(int i = 0; i < p.length(); i++) {
                    params.add(p.optString(i));
                }
                return new ActionMessage(time, actionType, id, params);

            case TYPE_REQUEST_ACTION:
                id = content.optLong("id", -1);
                long deadline = content.optLong("deadline", -1);
                return new RequestActionMessage(time, id, deadline);

            case TYPE_AUTH_RESPONSE:
                String result = content.optString("result");
                return new AuthResponseMessage(time, result);

            case TYPE_AUTH_REQUEST:
                String username = content.optString("user");
                String password = content.optString("pw");
                return new AuthRequestMessage(time, username, password);

            case TYPE_BYE:
                return new ByeMessage(time);

            case TYPE_SIM_START:

            case TYPE_SIM_END:

            default:
                System.out.println("Message of type " + type + " cannot be build.");
        }
        return null;
    }
}
