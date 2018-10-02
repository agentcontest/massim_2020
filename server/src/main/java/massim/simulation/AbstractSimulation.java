package massim.simulation;

import massim.config.TeamConfig;
import massim.protocol.messages.ActionMessage;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * A (very abstract) simulation.
 * @author ta10
 */
public abstract class AbstractSimulation {

    /**
     * Setup the simulation. Called before the first step.
     * @param steps total number of steps
     * @param config the simulation's configuration.
     * @param matchTeams set of participating teams and their details
     * @return map of agent names to their respective initial (sim-start) percept
     */
    public abstract Map<String, SimStartMessage> init(int steps, JSONObject config, Set<TeamConfig> matchTeams);

    /**
     * Called before each step.
     * Can be used to prepare the actual step and definitely needs to calculate the agents' percepts.
     * @param stepNo number of the simulation step
     * @return map from agent names to their current step percept (for request-action message)
     */
    public abstract Map<String, RequestActionMessage> preStep(int stepNo);

    /**
     * Execute one step in the simulation.
     * The agent's new actions have been set now.
     * @param stepNo number of the simulation step
     * @param actionMap mapping from agent names to their actions for this step
     */
    public abstract void step(int stepNo, Map<String, ActionMessage> actionMap);

    /**
     * Finish simulation execution, prepare results, etc.
     * @return map from agent names to sim-end percepts
     */
    public abstract Map<String, SimEndMessage> finish();

    /**
     * @return a json object containing the simulation results in any form
     */
    public abstract JSONObject getResult();

    /**
     * @return the ID of this simulation
     */
    public abstract String getName();

    /**
     * Called after each {@link #step(int, Map)}
     * @return a snapshot of the current world state
     */
    public abstract JSONObject getSnapshot();

    /**
     * Must return a valid object after {@link #init(int, JSONObject, Set)} has been called.
     * @return all world data that does not change during the simulation
     */
    public abstract JSONObject getStaticData();

    /**
     * Handles a given command if supported.
     * @param command the command to handle
     */
    public abstract void handleCommand(String[] command);
}
