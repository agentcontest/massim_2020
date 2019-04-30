package massim.eismassim;

import eis.EIDefaultImpl;
import eis.exceptions.*;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.IILElement;
import eis.iilang.Percept;
import massim.eismassim.entities.ScenarioEntity;
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

    private Set<String> supportedActions = new HashSet<>();
    private Map<String, EISEntity> entities = new HashMap<>();

    private String configFile = "eismassimconfig.json";

    /**
     * Constructor.
     * Might be used by {@link eis.EILoader}.
     */
    public EnvironmentInterface() {
        super();
        setup();
    }

    /**
     * Additional constructor for when the config file does not rest in the current working directory.
     * @param configFile the actual path to the config file (including the file name)
     */
    public EnvironmentInterface(String configFile){
        super();
        this.configFile = configFile;
        setup();
    }

    /**
     * Setup method to be called at the end of each constructor.
     */
    private void setup(){
        EISEntity.setEnvironmentInterface(this);
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
        super.start();
        new Thread(this).start();
    }

    @Override
    protected LinkedList<Percept> getAllPerceptsFromEntity(String name) throws PerceiveException, NoEnvironmentException {
        EISEntity e = entities.get(name);
        if (e == null) throw new PerceiveException("unknown entity");
        if (!e.isConnected()) throw new PerceiveException("no valid connection");
        return e.getAllPercepts();
    }

    @Override
    protected boolean isSupportedByEnvironment(Action action) {
        return action != null && supportedActions.contains( action.getName());
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
    protected Percept performEntityAction(String name, Action action) throws ActException {
        EISEntity entity = entities.get(name);
        entity.performAction(action);
        return new Percept("done");
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

        // annotate percepts with timestamps
        if(config.optBoolean("times", true)){
            EISEntity.enableTimeAnnotations();
            Log.log("Timed annotations enabled.");
        }

        // enable scheduling
        if(config.optBoolean("scheduling", true)){
            EISEntity.enableScheduling();
            Log.log("Scheduling enabled.");
        }

        // enable notifications
        if(config.optBoolean("notifications", true)){
            EISEntity.enableNotifications();
            Log.log("Notifications enabled.");
        }

        // timeout
        int timeout = config.optInt("timeout", 3900);
        EISEntity.setTimeout(timeout);
        Log.log("Timeout set to " + timeout);

        // queue
        if(config.optBoolean("queued", true)){
            EISEntity.enablePerceptQueue();
            Log.log("Percept queue enabled.");
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

            EISEntity entity = new ScenarioEntity(name, host, port, username, password);

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
    }

    @Override
    public void run() {
        while (this.getState() != EnvironmentState.KILLED) {

            // check connections and attempt to reconnect if necessary
            for ( EISEntity e : entities.values() ) {
                if (!e.isConnected()) {
                    Log.log("entity \"" + e.getName() + "\" is not connected. trying to connect.");
                    Executors.newSingleThreadExecutor().execute(e::establishConnection);
                }
            }

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {} // be nice to the server and our CPU
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
        EISEntity entity = entities.get(entityName);
        return entity != null && entity.isConnected();
    }
}
