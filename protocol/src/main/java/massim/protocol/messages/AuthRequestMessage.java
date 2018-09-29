package massim.protocol.messages;

import org.json.JSONObject;

public class AuthRequestMessage extends Message {

    public String username;
    public String password;

    public AuthRequestMessage(long time, String username, String password) {
        super(time);
        this.username = username;
        this.password = password;
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
}
