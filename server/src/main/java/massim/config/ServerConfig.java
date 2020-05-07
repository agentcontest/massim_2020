package massim.config;

import org.json.JSONObject;

import java.util.*;

/**
 * Holds all massim.config values relevant for the server part. Initialized with the massim.config JSON object.
 * Also references all other massim.config objects.
 * @author ta10
 */
public class ServerConfig {

    public final static String MODE_ROUND_ROBIN = "round-robin";
    public final static String MODE_MANUAL = "manual";
    public final static String MODE_RANDOM = "random";

    public String tournamentMode;
    public String launch;
    public int teamsPerMatch;
    public List<TeamConfig> teams = new ArrayList<>();
    public List<JSONObject> simConfigs = new ArrayList<>();
    public int port;
    public int backlog;
    public Map<String, String> accounts = new HashMap<>();
    public long agentTimeout;
    public String logPath;
    public String resultPath;

    /**
     * The level at which to log.
     */
    public String logLevel;

    /**
     * All teams to participate in any simulation.
     */
    public List<Set<TeamConfig>> manualModeTeams;

    /**
     * The maximum length of a received XML document (in bytes). Larger files will not be processed.
     */
    public int maxPacketLength;

    /**
     * The path were replays should be saved. If null, replay won't be saved.
     */
    public String replayPath;

    /**
     * The port for the webmonitor or 0.
     */
    public int monitorPort;

    /**
     * The amount of ms to pause between simulations.
     */
    public int waitBetweenSimulations = 0;

    /**
     * Actual number of agents required in each simulation.
     */
    public List<Integer> teamSizes = new ArrayList<>();
}
