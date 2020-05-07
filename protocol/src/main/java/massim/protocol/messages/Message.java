package massim.protocol.messages;

import massim.protocol.messages.scenario.InitialPercept;
import massim.protocol.messages.scenario.StepPercept;
import org.json.JSONObject;

public abstract class Message {

    public final static String TYPE_REQUEST_ACTION = "request-action";
    public final static String TYPE_ACTION = "action";
    public final static String TYPE_AUTH_REQUEST = "auth-request";
    public final static String TYPE_AUTH_RESPONSE = "auth-response";
    public final static String TYPE_SIM_START = "sim-start";
    public final static String TYPE_SIM_END = "sim-end";
    public final static String TYPE_BYE = "bye";
    public final static String TYPE_STATUS_REQUEST = "status-request";
    public final static String TYPE_STATUS_RESPONSE = "status-response";

    public abstract String getMessageType();

    public abstract JSONObject makeContent();

    public JSONObject toJson() {
        JSONObject message = new JSONObject();
        message.put("type", getMessageType());
        message.put("content", makeContent());
        return message;
    }

    public static Message buildFromJson(JSONObject src) {
        if(src == null) return null;
        String type = src.optString("type");
        JSONObject content = src.optJSONObject("content");
        if(content == null) return null;
        switch(type) {
            case TYPE_ACTION: return new ActionMessage(content);
            case TYPE_REQUEST_ACTION: return new StepPercept(content);
            case TYPE_AUTH_RESPONSE: return new AuthResponseMessage(content);
            case TYPE_AUTH_REQUEST: return new AuthRequestMessage(content);
            case TYPE_BYE: return new ByeMessage(content);
            case TYPE_SIM_START: return new InitialPercept(content);
            case TYPE_SIM_END: return new SimEndMessage(content);
            case TYPE_STATUS_REQUEST: return new StatusRequestMessage();
            case TYPE_STATUS_RESPONSE: return new StatusResponseMessage(content);
            default: System.out.println("Message of type " + type + " cannot be build.");
        }
        return null;
    }
}
