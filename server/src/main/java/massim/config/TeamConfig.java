package massim.config;

import java.util.*;

/**
 * Represents one configured team.
 * @author ta10
 */
public class TeamConfig {
    private String name;

    private Map<String, String> passwords = new HashMap<>();
    private Vector<String> agents = new Vector<>();

    /**
     * Creates a new team with the given name.
     * @param name the name of the team
     */
    public TeamConfig(String name){
        this.name = name;
    }

    /**
     * @return the name of the team
     */
    public String getName(){
        return name;
    }

    /**
     * Adds an agent to the team (in order).
     * @param name name of the agent
     * @param password the agent's password
     */
    public void addAgent(String name, String password){
        agents.add(name);
        passwords.put(name, password);
    }

    /**
     * @param agentName name of an agent
     * @return the agent's password or null if no such agent exists
     */
    public String getPassword(String agentName){
        return passwords.get(agentName);
    }

    /**
     * Creates a new list containing the names of all agents in this team.
     * @return the new set
     */
    public ArrayList<String> getAgentNames(){
        return new ArrayList<>(agents);
    }

    public int size() {
        return agents.size();
    }
}
