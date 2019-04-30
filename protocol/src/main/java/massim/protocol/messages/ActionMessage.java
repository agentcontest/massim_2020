package massim.protocol.messages;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActionMessage extends Message{

    public final static String NO_ACTION = "no_action";
    public final static String UNKNOWN_ACTION = "unknown_action";

    public final static String RESULT_UNPROCESSED = "unprocessed";
    public final static String RESULT_PENDING = "pending";
    public final static String RESULT_SUCCESS = "success";
    public final static String RESULT_F = "failed";
    public final static String RESULT_F_RANDOM = "failed_random";
    public final static String RESULT_F_PARAMETER = "failed_parameter";
    public final static String RESULT_F_PATH = "failed_path";
    public final static String RESULT_F_PARTNER = "failed_partner";

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
        content.put("type", actionType);
        content.put("id", id);
        JSONArray params = new JSONArray();
        this.params.forEach(params::put);
        content.put("p", params);
        return content;
    }
}
