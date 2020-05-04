package massim.protocol.messages.scenario;

import java.util.List;

public abstract class Actions {

    public final static String NO_ACTION = "no_action";
    public final static String UNKNOWN_ACTION = "unknown_action";

    public final static String MOVE = "move";
    public final static String ATTACH = "attach";
    public final static String DETACH = "detach";
    public final static String ROTATE = "rotate";
    public final static String CONNECT = "connect";
    public final static String REQUEST = "request";
    public final static String SUBMIT = "submit";
    public final static String CLEAR = "clear";
    public final static String DISCONNECT = "disconnect";
    public final static String SKIP = "skip";
    public final static String ACCEPT = "accept";

    public static final List<String> ALL_ACTIONS = List.of(
            MOVE, ATTACH, DETACH, ROTATE, CONNECT, REQUEST, SUBMIT, CLEAR, DISCONNECT, SKIP, ACCEPT);

    public final static String RESULT_UNPROCESSED = "unprocessed";
    public final static String RESULT_SUCCESS = "success";
    public final static String RESULT_F = "failed";
    public final static String RESULT_F_RANDOM = "failed_random";
    public final static String RESULT_F_PARAMETER = "failed_parameter";
    public final static String RESULT_F_PATH = "failed_path";
    public final static String RESULT_F_PARTNER = "failed_partner";
    public final static String RESULT_F_TARGET = "failed_target";
    public static final String RESULT_F_BLOCKED = "failed_blocked";
    public static final String RESULT_F_STATUS = "failed_status";
    public static final String RESULT_F_RESOURCES = "failed_resources";
    public static final String RESULT_F_LOCATION = "failed_location";
}
