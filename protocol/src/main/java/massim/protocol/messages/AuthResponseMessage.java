package massim.protocol.messages;

import org.json.JSONObject;

public class AuthResponseMessage extends Message {

    public final static String OK = "ok";
    public final static String FAIL = "fail";

    public String result;

    public AuthResponseMessage(long time, JSONObject content) {
        super(time);
        this.result = content.optString("result");
    }

    public AuthResponseMessage(long time, String result) {
        super(time);
        this.result = result;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_AUTH_RESPONSE;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.append("result", result);
        return content;
    }
}
