package massim.eismassim.entities;

import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;
import massim.eismassim.EISEntity;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.protocol.messages.scenario.InitialPercept;
import massim.protocol.messages.scenario.StepPercept;
import org.json.JSONObject;

import java.util.*;

/**
 * An EIS compatible entity for the 2017 MAPC City scenario.
 */
public class ScenarioEntity extends EISEntity {

    public ScenarioEntity(String name, String host, int port, String username, String password) {
        super(name, host, port, username, password);
    }

    @Override
    protected List<Percept> simStartToIIL(SimStartMessage startPercept) {

        List<Percept> ret = new Vector<>();
        if(!(startPercept instanceof InitialPercept)) return ret; // protocol incompatibility
        InitialPercept simStart = (InitialPercept) startPercept;

        // TODO

        return ret;
    }

    @Override
    protected Collection<Percept> requestActionToIIL(RequestActionMessage message) {
        Set<Percept> ret = new HashSet<>();
        if(!(message instanceof StepPercept)) return ret; // percept incompatible with entity
        StepPercept percept = (StepPercept) message;

        ret.add(new Percept("actionID", new Numeral(percept.getId())));
        ret.add(new Percept("timestamp", new Numeral(percept.getTime())));
        ret.add(new Percept("deadline", new Numeral(percept.getDeadline())));

        return ret;
    }



    @Override
    protected Collection<Percept> simEndToIIL(SimEndMessage endPercept) {
        HashSet<Percept> ret = new HashSet<>();
        if (endPercept != null){
            ret.add(new Percept("ranking", new Numeral(endPercept.getRanking())));
            ret.add(new Percept("score", new Numeral(endPercept.getScore())));
        }
        return ret;
    }

    @Override
    public JSONObject actionToJSON(long actionID, Action action) {

        // translate parameters to String
        List<String> parameters = new Vector<>();
        action.getParameters().forEach(param -> {
            if (param instanceof Identifier){
                parameters.add(((Identifier) param).getValue());
            }
            else if(param instanceof Numeral){
                parameters.add(((Numeral) param).getValue().toString());
            }
            else{
                log("Cannot translate parameter " + param);
                parameters.add(""); // add empty parameter so the order is not invalidated
            }
        });

        // create massim protocol action
        ActionMessage msg = new ActionMessage(action.getName(), actionID, parameters);
        return msg.toJson();
    }
}
