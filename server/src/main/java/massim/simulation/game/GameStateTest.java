package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.data.Position;
import massim.protocol.messages.scenario.Actions;
import massim.util.RNG;
import org.json.JSONObject;

import java.util.Set;

public class GameStateTest {

    private static final JSONObject CONFIG = new JSONObject("{\n" +
            "      \"steps\" : 10,\n" +
            "      \"randomSeed\" : 17,\n" +
            "      \"randomFail\" : 1,\n" +
            "      \"entities\" : [{\"standard\" : 10}],\n" +
            "      \"grid\" : {\n" +
            "        \"height\" : 20,\n" +
            "        \"width\" : 20\n" +
            "      },\n" +
            "      \"attachLimit\" : 3,\n" +
            "      \"blockTypes\" : [3, 3],\n" +
            "      \"dispensers\" : [2, 3],\n" +
            "      \"tasks\" : {\n" +
            "        \"size\" : [2, 4],\n" +
            "        \"duration\" : [100, 200],\n" +
            "        \"probability\" : 1\n" +
            "      }\n" +
            "    }");

    private GameState state;

    @org.junit.Before
    public void setUp() throws Exception {
        RNG.initialize(17);

        var team = new TeamConfig("A");
        for (var i = 1; i <= 10; i++) team.addAgent("A" + i, "1");
        state = new GameState(CONFIG, Set.of(team));
    }

    @org.junit.Test
    public void handleRequestAction() {
        var blockTypes = state.getBlockTypes();
        var dispenserPos = Position.of(3, 3);
        Entity a1 = state.getEntityByName("A1");
        assert a1 != null;
        assert state.createDispenser(dispenserPos, blockTypes.iterator().next());
        assert state.teleport("A1", dispenserPos.moved("s", 2));

        // too far away -> fail
        assert !state.handleRequestAction(a1, "n").equals(Actions.RESULT_SUCCESS);
        //move closer
        assert state.handleMoveAction(a1, "n").equals(Actions.RESULT_SUCCESS);
        // wrong param -> fail
        assert !state.handleRequestAction(a1, "w").equals(Actions.RESULT_SUCCESS);
        // everything correct -> success
        assert state.handleRequestAction(a1, "n").equals(Actions.RESULT_SUCCESS);
        // repeat -> fail
        assert !state.handleRequestAction(a1, "s").equals(Actions.RESULT_SUCCESS);
    }
}