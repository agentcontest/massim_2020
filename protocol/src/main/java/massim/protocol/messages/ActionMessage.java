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

    public ActionMessage(long time, JSONObject content) {
        super(time);
        this.actionType = content.optString("type", UNKNOWN_ACTION);
        this.id = content.optLong("id", -1);
        JSONArray p = content.optJSONArray("p");
        this.params = new ArrayList<>();
        for(int i = 0; i < p.length(); i++) {
            this.params.add(p.optString(i));
        }
    }

    public ActionMessage(long time, String actionType, long id, ArrayList<String> params) {
        super(time);
        this.actionType = actionType;
        this.id = id;
        this.params = params;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String type) {
        this.actionType = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
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
