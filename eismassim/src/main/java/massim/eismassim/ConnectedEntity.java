package massim.eismassim;

import eis.PerceptUpdate;
import eis.exceptions.ActException;
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
import java.util.concurrent.LinkedBlockingQueue;
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
    private static boolean throwExceptions = false; // throw exceptions? (some agent platforms don't like that)

    // config for this entity
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private boolean useJSON = false;
    private boolean useIILang = false;

    private boolean connected = false;
    private boolean connecting = false;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    private final Set<Percept> simStartPercepts = new HashSet<>();
    private final Set<Percept> previousSimStartPercepts = new HashSet<>();
    private final Set<Percept> requestActionPercepts = new HashSet<>();
    private final Set<Percept> previousRequestActionPercepts = new HashSet<>();
    private final Set<Percept> simEndPercepts = new HashSet<>();
    private final Set<Percept> previousSimEndPercepts = new HashSet<>();
    private final Set<Percept> byePercepts = new HashSet<>();
    private final Set<Percept> previousByePercepts = new HashSet<>();

    private long lastUsedActionId;
    protected long currentActionId;
    private long lastActionIdPerceivedFor;

    private final LinkedBlockingQueue<Message> inbox = new LinkedBlockingQueue<>();

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

    static void enableExceptions() {
        throwExceptions = true;
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

    @Override
    public void run() {

        new Thread(() -> {
            while (connected) {
                try {
                    var json = receiveMessage();
                    var msg = Message.buildFromJson(json);
                    if (msg != null)
                        inbox.add(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    releaseConnection();
                    break;
                }
            }
        }).start();

        while (connected) {
            try {
                updatePercepts(inbox.take());
            } catch (InterruptedException ignored) {}
        }
    }

    private synchronized void updatePercepts(Message msg) {
        if (msg == null) return;

        if (msg instanceof SimStartMessage) {
            SimStartMessage startMessage = (SimStartMessage) msg;
            simStartPercepts.clear();
            simStartPercepts.add(new Percept("simStart"));
            simStartPercepts.addAll(simStartToIIL(startMessage));

            if (notifications) EI.sendNotifications(getName(), simStartPercepts);
        }
        else if (msg instanceof RequestActionMessage) {
            RequestActionMessage rac = (RequestActionMessage) msg;
            requestActionPercepts.clear();
            requestActionPercepts.add(new Percept("requestAction"));
            requestActionPercepts.addAll(requestActionToIIL(rac));

            currentActionId = rac.getId();

            if (notifications) EI.sendNotifications(this.getName(), requestActionPercepts);
        }
        else if (msg instanceof SimEndMessage) {
            SimEndMessage endMessage = (SimEndMessage) msg;
            simStartPercepts.clear();
            requestActionPercepts.clear();
            simEndPercepts.clear();
            simEndPercepts.add(new Percept("simEnd"));
            simEndPercepts.addAll(simEndToIIL(endMessage));

            if (notifications) EI.sendNotifications(this.getName(), simEndPercepts);
        }
        else if (msg instanceof ByeMessage) {
            simStartPercepts.clear();
            requestActionPercepts.clear();
            byePercepts.clear();
            byePercepts.add(new Percept("bye"));

            if (notifications) EI.sendNotifications(this.getName(), byePercepts);
            this.releaseConnection();
        }
        else {
            log("unexpected type " + msg.getMessageType());
        }
    }

    /**
     * @return whether the entity is not connected to a massim server
     */
    boolean isNotConnected() {
        return !connected;
    }

    /**
     * If scheduling is enabled, the method blocks until a new action id, i.e. new percepts, are received
     * or the configured timeout is reached.
     * @return the percepts for this entity
     * @throws PerceiveException if timeout configured and occurred
     */
    @Override
    public PerceptUpdate getPercepts() throws PerceiveException{
        if (scheduling) {
            // block if already perceived for the same action ID
            long startTime = System.currentTimeMillis();
            while (currentActionId <= lastActionIdPerceivedFor || currentActionId == -1 ) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ignored) {}
                if (timeout > 0 && System.currentTimeMillis() - startTime >= timeout) {
                    if (throwExceptions)
                        throw new PerceiveException("timeout. no valid action-id available in time");
                    else {
                        return new PerceptUpdate();
                    }
                }
            }
            lastActionIdPerceivedFor = currentActionId;
        }

        return takePercepts();
    }

    private synchronized PerceptUpdate takePercepts() {
        var ret = takePercepts(previousSimStartPercepts, simStartPercepts);
        ret.merge(takePercepts(previousRequestActionPercepts, requestActionPercepts));
        ret.merge(takePercepts(previousSimEndPercepts, simEndPercepts));
        ret.merge(takePercepts(previousByePercepts, byePercepts));

        if (useIILang) log(ret.toString());

        return ret;
    }

    private static PerceptUpdate takePercepts(Set<Percept> previousPercepts, Set<Percept> currentPercepts) {
        var res = createPerceptUpdate(previousPercepts, currentPercepts);
        previousPercepts.clear();
        previousPercepts.addAll(currentPercepts);
        return res;
    }

    private static PerceptUpdate createPerceptUpdate(Set<Percept> previousPercepts, Set<Percept> currentPercepts) {
        var addList = new ArrayList<>(currentPercepts);
        addList.removeAll(previousPercepts);
        var delList = new ArrayList<>(previousPercepts);
        delList.removeAll(currentPercepts);
        return new PerceptUpdate(addList, delList);
    }

    /**
     * Performs an action by transforming it to JSON and sending it to the massim server.
     * @param action the action to perform
     * @throws ActException if the action could not be sent
     */
    void performAction(Action action) throws ActException{

        if (!connected) {
            if (throwExceptions)
                throw new ActException(ActException.FAILURE, "cannot perform action - no valid connection");
            else return;
        }

        // wait for a valid action id
        long startTime = System.currentTimeMillis();
        if (scheduling) {
            while (currentActionId <= lastUsedActionId || currentActionId == -1) {
                try {
                    TimeUnit.MILLISECONDS.timedWait(this, 50);
                } catch (InterruptedException ignored) {}
                if (timeout > 0 && System.currentTimeMillis() - startTime >= timeout) {
                    if (throwExceptions)
                        throw new ActException(ActException.FAILURE, "timeout. no valid action-id available in time");
                    else
                        return;
                }
            }
        }

        JSONObject json = actionToJSON(currentActionId, action);
        try {
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
        if (connecting) return;
        connecting = true;
        try {
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            log("socket successfully created");

            if (authenticate()) {
                log("authentication acknowledged");

                lastUsedActionId = -1;
                currentActionId = -1;
                lastActionIdPerceivedFor = -1;
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

        Message authReq = new AuthRequestMessage(username, password);
        try {
            sendMessage(authReq.toJson());
        } catch (IOException e) {
            log(e.getMessage());
            return false;
        }

        JSONObject jsonResponse;
        try {
            jsonResponse = receiveMessage();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Message responseMsg = Message.buildFromJson(jsonResponse);

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
            TimeUnit.MILLISECONDS.sleep(1000);
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
}