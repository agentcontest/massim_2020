package massim.protocol.messages;

import org.json.JSONObject;

public class AuthRequestMessage extends Message {

    public String username;
    public String password;

    public AuthRequestMessage(long time, JSONObject content) {
        super(time);
        this.username = content.optString("user");
        this.password = content.optString("pw");
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_AUTH_REQUEST;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.append("user", username);
        content.append("pw", password);
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
