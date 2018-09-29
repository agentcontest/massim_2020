package massim.protocol.messages;

import massim.protocol.messagecontent.Action;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ActionMessage extends Message{

    public final static String NO_ACTION = "noAction";
    private final static String UNKNOWN_ACTION = "unknownAction";
    public final static String RANDOM_FAIL = "randomFail";

    public static final Action STD_NO_ACTION = new Action(NO_ACTION);
    public static final Action STD_UNKNOWN_ACTION = new Action(UNKNOWN_ACTION);
    public static final Action STD_RANDOM_FAIL_ACTION = new Action(RANDOM_FAIL);

    private String actionType;
    private long id;
    private List<String> params;

    public ActionMessage(long time, String type, long id, List<String> params) {
        super(time);
        this.actionType = type;
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
