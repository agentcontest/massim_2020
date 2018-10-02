package massim.protocol.messages.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Actions {

    public final static String GOTO = "goto";

    public static final List<String> ALL_ACTIONS = new ArrayList<>(Arrays.asList(GOTO));
}
