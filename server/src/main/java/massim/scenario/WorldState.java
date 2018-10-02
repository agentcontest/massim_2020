package massim.scenario;

import massim.config.TeamConfig;
import massim.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * State of the world.
 */
public class WorldState {

    private int totalSteps;
    private int randomFail;
    private String id;
    private Map<String, TeamState> teams = new HashMap<>();
    private Map<String, String> agentToTeam = new HashMap<>();
    private Map<String, Entity> agentToEntity = new HashMap<>();
    private Map<Entity, String> entityToAgent = new HashMap<>();
    private Map<String, Role> roles;
    private List<String> agentNames;

    public WorldState(int steps, JSONObject config, Set<TeamConfig> matchTeams) {

        totalSteps = steps;

        // parse simulation config
        id = config.optString("id", "Default-simulation");
        Log.log(Log.Level.NORMAL, "Configuring simulation id: " + id);
        randomFail = config.optInt("randomFail", 1);
        Log.log(Log.Level.NORMAL, "Configuring random fail probability: " + randomFail);

        roles = parseRoles(config.optJSONObject("roles"));

        // store teams
        matchTeams.forEach(team -> {
            Vector<String> agNames = team.getAgentNames();
            agNames.forEach(agName -> agentToTeam.put(agName, team.getName()));
            teams.put(team.getName(), new TeamState(team.getName()));
        });
        agentNames = new ArrayList<>(agentToTeam.keySet());

        // create entities
        JSONArray entities = config.optJSONArray("entities");
        if(entities != null){
            for (int i = 0; i < entities.length(); i++) {
                JSONObject entityConf = entities.optJSONObject(i);
                if (entityConf != null){
                    String roleName = entityConf.keys().next();
                    int amount = entityConf.optInt(roleName, 0);
                    for (int j = 0; j < amount; j++){
                        Entity entity = new Entity(roles.get(roleName));
                        int finalI = i;
                        matchTeams.forEach(team -> {
                            String agentName;
                            if(team.getAgentNames().size() > finalI) {
                                agentName = team.getAgentNames().get(finalI);
                            }
                            else {
                                agentName = team.getName() + "-unconfigured-" + finalI;
                                Log.log(Log.Level.ERROR, "Too few agents configured for team " + team.getName()
                                        + ", using agent name " + agentName + ".");
                            }
                            agentToEntity.put(agentName, entity);
                            entityToAgent.put(entity, agentName);
                        });
                    }
                }
            }
        }
    }

    /**
     * Reads role information from the "role" JSON object.
     * @param roleConf the JSON object hopefully containing some roles
     */
    private static Map<String, Role> parseRoles(JSONObject roleConf) {
        if (roleConf == null){
            Log.log(Log.Level.CRITICAL, "No roles defined");
            return new HashMap<>();
        }
        Map<String, Role> roleMap = new HashMap<>();
        roleConf.keys().forEachRemaining(roleName -> {
            JSONObject roleJson = roleConf.optJSONObject(roleName);
            if (roleJson == null) {
                Log.log(Log.Level.ERROR, "Invalid JSON role object.");
            }
            else {
                roleMap.put(roleName, new Role(roleName));
            }
        });
        return roleMap;
    }
}

