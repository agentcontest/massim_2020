package massim.protocol.messages;

import org.json.JSONObject;

public class AuthResponseMessage extends Message {

    public final static String OK = "ok";
    public final static String FAIL = "fail";

    private long time;
    private String result;

    public AuthResponseMessage(JSONObject content) {
        this.time = content.optLong("time");
        this.result = content.optString("result");
    }

    public AuthResponseMessage(long time, String result) {
        this.time = time;
        this.result = result;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_AUTH_RESPONSE;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.put("result", result);
        return content;
    }

    public long getTime() {
        return time;
    }

    public String getResult() {
        return result;
    }
}
