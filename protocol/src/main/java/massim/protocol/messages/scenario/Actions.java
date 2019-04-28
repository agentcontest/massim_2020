package massim.protocol.messages.scenario;

import java.util.List;

public abstract class Actions {

    public final static String MOVE = "move";
    public final static String ATTACH = "attach";
    public final static String DETACH = "detach";
    public final static String ROTATE = "rotate";
    public final static String CONNECT = "connect";
    public final static String REQUEST = "request";
    public final static String SUBMIT = "submit";

    public static final List<String> ALL_ACTIONS = List.of(
            MOVE, ATTACH, DETACH, ROTATE, CONNECT, REQUEST, SUBMIT);
}
