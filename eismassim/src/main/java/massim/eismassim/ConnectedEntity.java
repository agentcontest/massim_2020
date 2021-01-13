package massim.eismassim;

import eis.PerceptUpdate;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.Percept;
import massim.protocol.messages.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * An entity for the EIS to realize client-server communication following the MASSim protocol.
 */
public abstract class ConnectedEntity extends Entity {

    private static EnvironmentInterface EI;

    // config for all entities
    private static int timeout; // timeout for performing actions (if scheduling is enabled)
    private static boolean scheduling = false; // send only one action per action-id?
    private static boolean notifications = false; // send percepts as notifications?
    private static boolean queued = false; // only get one set of percepts per call
    private static boolean onlyOnce = false; // clear percepts after retrieval

    // config for this entity
    private String username;
    private String password;
    private String host;
    private int port;
    private boolean useJSON = false;
    private boolean useIILang = false;

    private boolean connected = false;
    private boolean connecting = false;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean terminated = false;

    private Set<Percept> simStartPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> previousSimStartPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> requestActionPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> previousRequestActionPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> simEndPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> previousSimEndPercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> byePercepts = Collections.synchronizedSet(new HashSet<>());
    private Set<Percept> previousByePercepts = Collections.synchronizedSet(new HashSet<>());

    // used to store the percepts in the order of arrival, if queuing is activated
    private AbstractQueue<PerceptUpdate> perceptsQueue = new ConcurrentLinkedQueue<>();

    // action IDs
    private long lastUsedActionId;
    protected long currentActionId;
    private long lastUsedActionIdPercept;

    public ConnectedEntity(String name, String host, int port, String username, String password) {
        super(name);
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Maps the sim-start-message to IILang.
     * @param startPercept the sim-start message to map
     * @return a list of percepts derived from the sim-start message
     */
    protected abstract List<Percept> simStartToIIL(SimStartMessage startPercept);

    /**
     * Maps the request-action-message to IILang.
     * @param message the step percept message
     * @return a collection of percepts derived from the request-action message
     */
    protected abstract Collection<Percept> requestActionToIIL(RequestActionMessage message);

    /**
     * Maps the sim-end-message to IILang.
     * @param endPercept the sim-end percept to map
     * @return a collection of percepts derived from the sim-end message
     */
    protected abstract Collection<Percept> simEndToIIL(SimEndMessage endPercept);

    /**
     * Maps an IILang-action to JSON.
     * @param id the action ID to use
     * @param action the action to transform
     * @return the JSON object (to send)
     */
    protected abstract JSONObject actionToJSON(long id, Action action);

    /**
     * Sets the environment interface for all entities
     * @param environmentInterface the EI
     */
    static void setEnvironmentInterface(EnvironmentInterface environmentInterface) {
        ConnectedEntity.EI = environmentInterface;
    }

    /**
     * Set the timeout of all entities.
     * @param timeout the timeout for all entities in ms for scheduling (if scheduling is enabled)
     */
    static void setTimeout(int timeout) {
        ConnectedEntity.timeout = timeout;
    }

    /**
     * Enables scheduling, i.e. only one action can be sent per action id.
     */
    static void enableScheduling() {
        scheduling = true;
    }

    /**
     * Enables notifications.
     */
    static void enableNotifications() {
        notifications = true;
    }

    /**
     * Enables queued percepts.
     */
    static void enablePerceptQueue() {
        queued = true;
    }

    /**
     * If enabled, percepts will be cleared after each call to {@link #getAllPercepts}.
     */
    static void enableOnlyOnceRetrieval() {
        onlyOnce = true;
    }

    /**
     * Enables json output for percepts.
     */
    void enableJSON() {
        useJSON = true;
    }

    /**
     * Enables IILang output for percepts.
     */
    void enableIILang() {
        useIILang = true;
    }

    /**
     * Stops this entity and its thread. Closes the socket, if there is one.
     */
    public void terminate(){
        terminated = true;
        releaseConnection();
    }

    @Override
    public void run() {
        while (!terminated && connected){

            // receive a message
            JSONObject json;
            try {
                json = receiveMessage();
            } catch (IOException e) {
                e.printStackTrace();
                releaseConnection();
                break;
            }

            // process message
            Message msg = Message.buildFromJson(json);
            if (msg == null) continue;

            if (msg instanceof SimStartMessage) {
                SimStartMessage startMessage = (SimStartMessage) msg;
                simStartPercepts.clear();
                simStartPercepts.add(new Percept("simStart"));
                simStartPercepts.addAll(simStartToIIL(startMessage));

                if (notifications) EI.sendNotifications(getName(), simStartPercepts);
                if (queued) {
                	var addList = new ArrayList<>(simStartPercepts);
            		addList.removeAll(previousSimStartPercepts);
            		var delList = new ArrayList<>(previousSimStartPercepts);
            		delList.removeAll(simStartPercepts);
            		previousSimStartPercepts = Collections.synchronizedSet(new HashSet<>(simStartPercepts));
                	perceptsQueue.add(new PerceptUpdate(addList, delList));
                }
            }
            else if (msg instanceof RequestActionMessage) {
                RequestActionMessage rac = (RequestActionMessage) msg;
                long id = rac.getId();

                requestActionPercepts.clear();
                requestActionPercepts.add(new Percept("requestAction"));
                requestActionPercepts.addAll(requestActionToIIL(rac));

                if (notifications) EI.sendNotifications(this.getName(), requestActionPercepts);
                currentActionId = id;
                if (queued) {
                	var addList = new ArrayList<>(requestActionPercepts);
            		addList.removeAll(previousRequestActionPercepts);
            		var delList = new ArrayList<>(previousRequestActionPercepts);
            		delList.removeAll(requestActionPercepts);
            		previousRequestActionPercepts = Collections.synchronizedSet(new HashSet<>(requestActionPercepts));
                	perceptsQueue.add(new PerceptUpdate(addList, delList));
                }
            }
            else if (msg instanceof SimEndMessage) {
                SimEndMessage endMessage = (SimEndMessage) msg;
                simStartPercepts.clear();
                requestActionPercepts.clear();
                simEndPercepts.clear();
                simEndPercepts.add(new Percept("simEnd"));
                simEndPercepts.addAll(simEndToIIL(endMessage));
                if (notifications) EI.sendNotifications(this.getName(), simEndPercepts);
                if (queued) {
                	var addList = new ArrayList<>(simEndPercepts);
            		addList.removeAll(previousSimEndPercepts);
            		var delList = new ArrayList<>(previousSimEndPercepts);
            		delList.removeAll(simEndPercepts);
            		previousSimEndPercepts = Collections.synchronizedSet(new HashSet<>(simEndPercepts));
                	perceptsQueue.add(new PerceptUpdate(addList, delList));
                }
            }
            else if (msg instanceof ByeMessage) {
                simStartPercepts.clear();
                requestActionPercepts.clear();
                byePercepts.clear();
                byePercepts.add(new Percept("bye"));
                if (notifications) EI.sendNotifications(this.getName(), byePercepts);
                if (queued) {
                	var addList = new ArrayList<>(byePercepts);
            		addList.removeAll(previousByePercepts);
            		var delList = new ArrayList<>(previousByePercepts);
            		delList.removeAll(byePercepts);
            		previousByePercepts = Collections.synchronizedSet(new HashSet<>(byePercepts));
                	perceptsQueue.add(new PerceptUpdate(addList, delList));
                }
            }
            else {
                log("unexpected type " + msg.getMessageType());
            }
        }
    }

    /**
     * @return true if the entity is connected to a massim server
     */
    boolean isConnected() {
        return connected;
    }

    /**
     * If scheduling is enabled, the method blocks until a new action id, i.e. new percepts, are received
     * or the configured timeout is reached.
     * If queued is enabled, scheduling is overridden. Also, if queued is enabled, this method has to be called
     * repeatedly, as only one collection of percepts is removed from the queue with each call (until an empty list
     * is returned).
     * @return the percepts for this entity
     * @throws PerceiveException if timeout configured and occurred
     */
    @Override
    public synchronized PerceptUpdate getPercepts() throws PerceiveException{
        if (scheduling && !queued) {
            // wait for new action id or timeout
            long startTime = System.currentTimeMillis();
            while (currentActionId <= lastUsedActionIdPercept || currentActionId == -1 ) {
                try {
                    TimeUnit.MILLISECONDS.timedWait(this, 100);
                } catch (InterruptedException ignored) {}
                if (System.currentTimeMillis() - startTime >= timeout) {
                    throw new PerceiveException("timeout. no valid action-id available in time");
                }
            }
            lastUsedActionIdPercept = currentActionId;
        }

        if(!queued){
            //return all percepts
        	var addList1 = new ArrayList<>(simStartPercepts);
    		addList1.removeAll(previousSimStartPercepts);
    		var delList1 = new ArrayList<>(previousSimStartPercepts);
    		delList1.removeAll(simStartPercepts);
    		previousSimStartPercepts = Collections.synchronizedSet(new HashSet<>(simStartPercepts));
        	var ret = new PerceptUpdate(addList1,delList1);
        	

        	var addList2 = new ArrayList<>(requestActionPercepts);
        	addList2.removeAll(previousRequestActionPercepts);
    		var delList2 = new ArrayList<>(previousRequestActionPercepts);
    		delList2.removeAll(requestActionPercepts);
    		previousRequestActionPercepts = Collections.synchronizedSet(new HashSet<>(requestActionPercepts));
    		ret.merge(new PerceptUpdate(addList2, delList2));
        	

        	var addList3 = new ArrayList<>(simEndPercepts);
        	addList3.removeAll(previousSimEndPercepts);
    		var delList3 = new ArrayList<>(simStartPercepts);
    		delList3.removeAll(previousSimEndPercepts);
    		previousSimEndPercepts = Collections.synchronizedSet(new HashSet<>(simEndPercepts));
    		ret.merge(new PerceptUpdate(addList3, delList3));
        	

        	var addList4 = new ArrayList<>(byePercepts);
        	addList4.removeAll(previousByePercepts);
    		var delList4 = new ArrayList<>(previousByePercepts);
    		delList4.removeAll(byePercepts);
    		previousByePercepts = Collections.synchronizedSet(new HashSet<>(byePercepts));
    		ret.merge(new PerceptUpdate(addList4, delList4));
        	
        	
            if (useIILang) log(ret.toString());
            if (onlyOnce) {
                simStartPercepts.clear();
                requestActionPercepts.clear();
                simEndPercepts.clear();
                byePercepts.clear();
            }
            return ret;
        }
        else{
            //return only the first queued elements
            return perceptsQueue.peek() != null? perceptsQueue.poll() : new PerceptUpdate();
        }
    }

    /**
     * Performs an action by transforming it to JSON and sending it to the massim server.
     * @param action the action to perform
     * @throws ActException if the action could not be sent
     */
    void performAction(Action action) throws ActException{

        if (!connected) throw new ActException(ActException.FAILURE, "no valid connection");

        // wait for a valid action id
        long startTime = System.currentTimeMillis();
        if (scheduling) {
            while (currentActionId <= lastUsedActionId || currentActionId == -1) {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                if (System.currentTimeMillis() - startTime >= timeout) {
                    throw new ActException(ActException.FAILURE, "timeout. no valid action-id available in time");
                }
            }
        }

        JSONObject json = actionToJSON(currentActionId, action);
        try {
            assert currentActionId != lastUsedActionId;
            sendMessage(json);
            lastUsedActionId = currentActionId;
        } catch (IOException e) {
            releaseConnection();
            throw new ActException(ActException.FAILURE, "sending action failed", e);
        }
    }

    /**
     * Tries to connect to a MASSim server. Including authentication and all.
     */
    void establishConnection() {
        if(connecting) return;
        connecting = true;
        try {
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            log("socket successfully created");

            boolean result = authenticate();
            if (result) {
                log("authentication acknowledged");

                lastUsedActionId = -1;
                currentActionId = -1;
                lastUsedActionIdPercept = -1;
                connected = true;
                log("connection successfully authenticated");

                // start a listening thread
                new Thread(this).start();
                log("listening for incoming messages");
            }
            else {
                log("authentication denied");
            }
        } catch (UnknownHostException e) {
            log("unknown host " + e.getMessage());
        } catch (IOException e) {
            log(e.getMessage());
        }
        connecting = false;
    }

    /**
     * Sends an authentication-message to the server and waits for the reply.
     * @return true if authentication succeeded
     */
    private boolean authenticate() {

        // create and try to send message
        Message authReq = new AuthRequestMessage(username, password);
        try {
            sendMessage(authReq.toJson());
        } catch (IOException e) {
            log(e.getMessage());
            return false;
        }

        // get responseMsg
        JSONObject jsonResponse;
        try {
            jsonResponse = receiveMessage();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Message responseMsg = Message.buildFromJson(jsonResponse);

        // check for success
        if (responseMsg instanceof AuthResponseMessage) {
            AuthResponseMessage authResponse = (AuthResponseMessage) responseMsg;
            return authResponse.getResult().equals(AuthResponseMessage.OK);
        }
        return false;
    }

    /**
     * Tries to close the current socket if it exists.
     * Then sleeps for a second.
     */
    private void releaseConnection() {
        if (socket != null){
            try {
                socket.close();
            }
            catch(IOException ignored) {}
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        connected = false;
        log("connection released");
    }

    /**
     * Logs a message prefixed with this entity's name
     * @param s the string to log
     */
    protected void log(String s) {
        Log.log("Entity " + getName() + ": " + s);
    }

    /**
     * Sends a document.
     * @param json the message to be sent
     * @throws IOException if the document could not be sent
     */
    private void sendMessage(JSONObject json) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        osw.write(json.toString());
        osw.write(0);
        osw.flush();
        if (useJSON) log(json.toString(3) + "\tsent");
    }

    /**
     * Receives a document from the server,
     * @return the received document.
     * @throws IOException if no message could be received
     */
    private JSONObject receiveMessage() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) != 0) {
            if (read == -1) throw new IOException();
            buffer.write(read);
        }
        String message = buffer.toString(StandardCharsets.UTF_8);
        try {
            JSONObject json = new JSONObject(message);
            if (useJSON) log(json.toString(3) + "\treceived");
            return json;
        } catch(JSONException e){
            log("Invalid object: " + message);
        }
        return null;
    }

    protected void setType(String type) {
        try {
            if(EI.isEntityConnected(getName())) EI.setType(getName(), type);
        } catch (EntityException e) {
            e.printStackTrace();
        }
    }
}