package massim.protocol.messages;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActionMessage extends Message{

    public final static String NO_ACTION = "noAction";
    private final static String UNKNOWN_ACTION = "unknownAction";
    public final static String RANDOM_FAIL = "randomFail";

    private String actionType;
    private long id;
    private List<String> params;

    public ActionMessage(JSONObject content) {
        this.actionType = content.optString("type", UNKNOWN_ACTION);
        this.id = content.optLong("id", -1);
        JSONArray p = content.optJSONArray("p");
        this.params = new ArrayList<>();
        for(int i = 0; i < p.length(); i++) {
            this.params.add(p.optString(i));
        }
    }

    public ActionMessage(String actionType, long id, List<String> params) {
        this.actionType = actionType;
        this.id = id;
        this.params = params;
    }

    public String getActionType() {
        return actionType;
    }

    public long getId() {
        return id;
    }

    public List<String> getParams() {
        return params;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_ACTION;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();
        content.append("type", actionType);
        content.append("id", id);
        JSONArray params = new JSONArray();
        this.params.forEach(params::put);
        content.append("p", params);
        return content;
    }
}
