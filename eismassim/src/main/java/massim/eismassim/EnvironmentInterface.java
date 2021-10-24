package massim.eismassim;

import eis.EIDefaultImpl;
import eis.PerceptUpdate;
import eis.exceptions.*;
import eis.iilang.*;
import massim.eismassim.entities.ScenarioEntity;
import massim.eismassim.entities.StatusEntity;
import massim.protocol.messages.scenario.Actions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Environment interface to the MASSim server following the Environment Interface Standard (EIS).
 */
public class EnvironmentInterface extends EIDefaultImpl implements Runnable{
	private static final long serialVersionUID = 1L;
	private Set<String> supportedActions = new HashSet<>();
    private Map<String, Entity> entities = new HashMap<>();

    private String configFile = "eismassimconfig.json";

    /**
     * Constructor.
     * Might be used by {@link eis.EILoader}.
     */
    public EnvironmentInterface() {
        super();
    }

    /**
     * Additional constructor for when the config file is not in the current working directory.
     * @param configFile the actual path to the config file (including the file name)
     */
    public EnvironmentInterface(String configFile){
        super();
        this.configFile = configFile;
        setup();
    }

    public void init(Map<String, Parameter> parameters) throws ManagementException {
        super.init(parameters);
        setup();
    }

    /**
     * Setup method to be called at the end of each constructor.
     */
    private void setup(){
        ConnectedEntity.setEnvironmentInterface(this);
        supportedActions.addAll(Actions.ALL_ACTIONS);
        try {
            parseConfig();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        IILElement.toProlog = true;
        try {
            setState(EnvironmentState.PAUSED);
        } catch (ManagementException e1) {
            e1.printStackTrace();
        }
        entities.values().forEach(entity -> {
            try {
                addEntity(entity.getName());
            } catch (EntityException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void start() throws ManagementException{
        if(getState() == EnvironmentState.INITIALIZING) super.pause();
        super.start();
        new Thread(this).start();
    }

    @Override
    protected PerceptUpdate getPerceptsForEntity(String name) throws PerceiveException, NoEnvironmentException {
        var e = entities.get(name);
        if (e == null) throw new PerceiveException("unknown entity");
        if (e instanceof ConnectedEntity && !((ConnectedEntity) e).isConnected())
            throw new PerceiveException("no valid connection");
        return e.getPercepts();
    }

    @Override
    protected boolean isSupportedByEnvironment(Action action) {
        return action != null && supportedActions.contains(action.getName());
    }

    @Override
    protected boolean isSupportedByType(Action action, String type) {
        return action != null && supportedActions.contains(action.getName());
    }

    @Override
    protected boolean isSupportedByEntity(Action action, String entity) {
        return action != null && supportedActions.contains(action.getName());
    }

    @Override
    protected void performEntityAction(Action action, String name) throws ActException {
        var entity = entities.get(name);
        if (entity instanceof ConnectedEntity) {
            ((ConnectedEntity) entity).performAction(action);
        } else {
        	throw new ActException(ActException.NOTREGISTERED);
        }
    }

    /**
     * Parses the eismassimconfig.json file.
     * In case of invalid configuration, standard values are used where reasonable.
     * @throws ParseException in case configuration is invalid and no reasonable standard value can be assumed
     */
    private void parseConfig() throws ParseException {

        JSONObject config = new JSONObject();
        try {
            config = new JSONObject(new String(Files.readAllBytes(Paths.get(configFile))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // parse host and port
        String host = config.optString("host", "localhost");
        int port = config.optInt("port", 12300);
        Log.log("Configuring EIS: " + host + ":" + port);

        // enable scheduling
        if(config.optBoolean("scheduling", true)){
            ConnectedEntity.enableScheduling();
            Log.log("Scheduling enabled.");
        }

        // enable notifications
        if(config.optBoolean("notifications", true)){
            ConnectedEntity.enableNotifications();
            Log.log("Notifications enabled.");
        }

        // timeout
        int timeout = config.optInt("timeout", 3000);
        ConnectedEntity.setTimeout(timeout);
        Log.log("Timeout set to " + timeout);

        // queue
        if(config.optBoolean("queued", true)){
            ConnectedEntity.enablePerceptQueue();
            Log.log("Percept queue enabled.");
        }

        if(config.optBoolean("only-once", false)){
            ConnectedEntity.enableOnlyOnceRetrieval();
            Log.log("Only once retrieval enabled.");
        }

        // parse entities
        JSONArray jsonEntities = config.optJSONArray("entities");
        if(jsonEntities == null) jsonEntities = new JSONArray();
        for (int i = 0; i < jsonEntities.length(); i++) {
            JSONObject jsonEntity = jsonEntities.optJSONObject(i);
            if(jsonEntity == null) continue;
            String name = jsonEntity.optString("name");
            if (name == null) throw new ParseException("Entity must have a valid name", 0);
            String username = jsonEntity.optString("username");
            if (username == null) throw new ParseException("Entity must have a valid username", 0);
            String password = jsonEntity.optString("password");
            if (password == null) throw new ParseException("Entity must have a valid password", 0);

            ConnectedEntity entity = new ScenarioEntity(name, host, port, username, password);

            if(jsonEntity.optBoolean("print-json", true)){
                entity.enableJSON();
                Log.log("Enable JSON printing for entity " + entity.getName());
            }
            if(jsonEntity.optBoolean("print-iilang", true)){
                entity.enableIILang();
                Log.log("Enable IILang printing for entity " + entity.getName());
            }

            if(entities.put(entity.getName(), entity) != null){
                // entity by that name already existed
                Log.log("Entity by name " + entity.getName() + " configured multiple times. Previous one replaced.");
            }
        }

        // parse "multi-entities"
        var multiEntities = config.optJSONArray("multi-entities");
        if(multiEntities == null) multiEntities = new JSONArray();
        for (int i = 0; i < multiEntities.length(); i++) {
            var multiEntity = multiEntities.optJSONObject(i);
            if (multiEntity == null) continue;
            var namePrefix = multiEntity.getString("name-prefix");
            var usernamePrefix = multiEntity.getString("username-prefix");
            var password = multiEntity.getString("password");
            var count = multiEntity.optInt("count", -1);
            var startIndex = multiEntity.getInt("start-index");
            var printIILang = multiEntity.optBoolean("print-iilang", true);
            var printJSON = multiEntity.optBoolean("print-json", true);

            if (count == -1) {
                Log.log("EISMASSim auto config found. Querying server for number of entities.");
                var result = StatusEntity.queryServerStatus(host, port);
                if (result == null) Log.log("Error while trying to contact MASSim server at " + host + ":" + port);
                else {
                    for (var size: result.teamSizes) if (size > count) count = size;
                }
            }
            int endIndex = startIndex + count - 1;
            Log.log("Creating " + count + " new EISMASSim entities " + namePrefix + startIndex + " to " + namePrefix + endIndex);
            for (int index = startIndex; index <= endIndex; index++) {
                ConnectedEntity entity = new ScenarioEntity(namePrefix + index, host, port, usernamePrefix + index, password);
                if (printIILang) entity.enableIILang();
                if (printJSON) entity.enableJSON();
                if(entities.put(entity.getName(), entity) != null){
                    Log.log("Entity by name " + entity.getName() + " configured multiple times. Previous one replaced.");
                }
            }
        }

        // create status entity if requested
        var statusEntity = config.optJSONObject("status-entity");
        if (statusEntity != null) {
            var name = statusEntity.getString("name");
            Log.log("Creating status entity: " + name);
            entities.put(name, new StatusEntity(name, host, port));
        }
    }

    @Override
    public void run() {

        entities.values().stream().filter(e -> e instanceof StatusEntity).forEach(e -> ((StatusEntity)e).start());

        while (this.getState() != EnvironmentState.KILLED) {

            // check connections and attempt to reconnect if necessary
            for ( Entity e : entities.values()) {
                if (e instanceof ConnectedEntity && !((ConnectedEntity) e).isConnected()) {
                    Log.log("entity \"" + e.getName() + "\" is not connected. trying to connect.");
                    Executors.newSingleThreadExecutor().execute(((ConnectedEntity)e)::establishConnection);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {} // be nice to the server and our CPU
        }
    }

    /**
     * Sends notifications to an agent for a collection of percepts
     * @param name the name of the entity
     * @param percepts the percepts to send notifications for
     */
    void sendNotifications(String name, Collection<Percept> percepts) {
        if (getState() != EnvironmentState.RUNNING) return;
        for (Percept p : percepts){
            try {
                notifyAgentsViaEntity(p, name);
            } catch (EnvironmentInterfaceException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if an entity has a connection to a MASSim server.
     * @param entityName name of an entity
     * @return true if the entity exists and is connected to a MASSim server
     */
    public boolean isEntityConnected(String entityName){
        var entity = entities.get(entityName);
        return entity != null && entity instanceof ConnectedEntity && ((ConnectedEntity) entity).isConnected();
    }
}
