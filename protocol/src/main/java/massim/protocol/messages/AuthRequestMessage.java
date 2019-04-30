package massim.protocol.messages;

import org.json.JSONObject;

public class AuthRequestMessage extends Message {

    private String username;
    private String password;

    public AuthRequestMessage(JSONObject content) {
        this.username = content.optString("user");
        this.password = content.optString("pw");
    }

    public AuthRequestMessage(String username, String password) {
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
        content.put("user", username);
        content.put("pw", password);
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
