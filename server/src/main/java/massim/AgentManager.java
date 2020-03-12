package massim;

import massim.config.TeamConfig;
import massim.protocol.messages.*;
import massim.protocol.messages.scenario.Actions;
import massim.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles agent accounts and network connections to all agents.
 * @author ta10
 */
class AgentManager {

    private Map<String, AgentProxy> agents = new HashMap<>();

    private long agentTimeout;
    private boolean disconnecting = false;
    private int maxPacketLength;

    /**
     * If an agent's sendQueue is already "full", the oldest element will be removed before a new one is added
     */
    private int sendBufferSize = 4;

    /**
     * Creates a new agent manager responsible for sending and receiving messages.
     * @param teams a list of all teams to configure the manager for
     * @param agentTimeout the timeout to use for request-action messages (to wait for actions) in milliseconds
     * @param maxPacketLength the maximum size of packets to <b>process</b> (they are received anyway, just not parsed
     *                        in case they are too big)
     */
    AgentManager(List<TeamConfig> teams, long agentTimeout, int maxPacketLength) {
        teams.forEach(team -> team.getAgentNames().forEach((name) -> {
            agents.put(name, new AgentProxy(name, team.getName(), team.getPassword(name)));
        }));
        this.agentTimeout = agentTimeout;
        this.maxPacketLength = maxPacketLength;
    }

    /**
     * Stops all related threads and closes all sockets involved.
     */
    void stop(){
        disconnecting = true;
        agents.values().forEach(AgentProxy::close);
    }

    /**
     * Sets a new socket for the given agent that was just authenticated (again or for the first time).
     * @param s the new socket opened for the agent
     * @param agentName the name of the agent
     */
    void handleNewConnection(Socket s, String agentName){
        if (agents.containsKey(agentName)) agents.get(agentName).handleNewConnection(s);
    }

    /**
     * Checks if the given credentials are valid.
     * @param user name of the agent
     * @param inPass password of the agent
     * @return true iff the credentials are valid
     */
    boolean auth(String user, String inPass) {
        return agents.containsKey(user) && agents.get(user).password.equals(inPass);
    }

    /**
     * Sends initial percepts to the agents and stores them for later (possible agent reconnection).
     * @param initialPercepts mapping from agent names to initial percepts
     */
    void handleInitialPercepts(Map<String, SimStartMessage> initialPercepts) {
        initialPercepts.forEach((agName, percept) -> {
            if (agents.containsKey(agName)){
                agents.get(agName).handleInitialPercept(percept);
            }
        });
    }

    /**
     * Uses the percepts to send a request-action message and waits for the action answers.
     * {@link #agentTimeout} is used to limit the waiting time per agent.
     * @param percepts mapping from agent names to percepts of the current simulation state
     * @return mapping from agent names to actions received in response
     */
    Map<String, ActionMessage> requestActions(Map<String, RequestActionMessage> percepts) {
        // each thread needs to countdown the latch when it finishes
        CountDownLatch latch = new CountDownLatch(percepts.keySet().size());
        Map<String, ActionMessage> resultMap = new ConcurrentHashMap<>();
        percepts.forEach((agName, percept) -> {
            // start a new thread to get each action
            new Thread(() -> {
                ActionMessage action = agents.get(agName).requestAction(percept);
                resultMap.put(agName, action);
                latch.countDown();
            }).start();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.log(Log.Level.ERROR, "Latch interrupted. Actions probably incomplete.");
        }
        return resultMap;
    }

    /**
     * Sends sim-end percepts to the agents.
     * @param finalPercepts mapping from agent names to sim-end percepts
     */
    void handleFinalPercepts(Map<String, SimEndMessage> finalPercepts) {
        finalPercepts.forEach((agName, percept) -> {
            if (agents.containsKey(agName)){
                agents.get(agName).handleFinalPercept(percept);
            }
        });
    }

    /**
     * Stores account info of an agent.
     * Receives messages from and sends messages to remote agents.
     */
    private class AgentProxy {

        // things that do not change
        private String name;
        private String teamName;
        private String password;

        // networking things
        private Socket socket;
        private Thread sendThread;
        private Thread receiveThread;

        // concurrency magic
        private AtomicLong messageCounter = new AtomicLong();
        private LinkedBlockingDeque<JSONObject> sendQueue = new LinkedBlockingDeque<>();
        private Map<Long, CompletableFuture<JSONObject>> futureActions = new ConcurrentHashMap<>();

        private SimStartMessage lastSimStartMessage;

        /**
         * Creates a new instance with the given credentials.
         * @param name the name of the agent
         * @param team the name of the agent's team
         * @param pass the password to authenticate the agent with
         */
        private AgentProxy(String name, String team, String pass) {
            this.name = name;
            this.teamName = team;
            this.password = pass;
        }

        /**
         * Creates a message for the given initial percept and sends it to the remote agent.
         * @param percept the initial percept to forward
         */
        void handleInitialPercept(SimStartMessage percept) {
            lastSimStartMessage = percept;
            sendMessage(lastSimStartMessage);
        }

        /**
         * Creates a request-action message and sends it to the agent.
         * Should be called within a new thread, as it blocks up to {@link #agentTimeout} milliseconds.
         * @param percept the step percept to forward
         * @return the action that was received by the agent (or {@link Actions#NO_ACTION})
         */
        ActionMessage requestAction(RequestActionMessage percept) {
            long id = messageCounter.getAndIncrement();
            percept.updateIdAndDeadline(id, System.currentTimeMillis() + agentTimeout);
            CompletableFuture<JSONObject> futureAction = new CompletableFuture<>();
            futureActions.put(id, futureAction);
            sendMessage(percept);
            try {
                // wait for action to be received
                JSONObject json = futureAction.get(agentTimeout, TimeUnit.MILLISECONDS);
                Message msg = Message.buildFromJson(json);
                if(msg instanceof ActionMessage){
                    return (ActionMessage) msg;
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.log(Log.Level.ERROR, "Interrupted while waiting for action.");
            } catch (TimeoutException e) {
                Log.log(Log.Level.NORMAL, "No valid action available in time for agent " + name + ".");
            }
            return new ActionMessage(Actions.NO_ACTION, id, new ArrayList<>());
        }

        /**
         * Creates and send a sim-end message to the agent.
         * @param percept the percept to append to the message.
         */
        void handleFinalPercept(SimEndMessage percept) {
            lastSimStartMessage = null; // now we can stop resending it
            sendMessage(percept);
        }

        /**
         * Sets a new endpoint for sending and receiving messages. If a socket is already present, it is replaced and closed.
         * @param newSocket the new socket to use for this agent
         */
        private void handleNewConnection(Socket newSocket){
            // potentially close old socket
            if (sendThread != null) sendThread.interrupt();
            if (receiveThread != null) receiveThread.interrupt();
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
            // set new socket and open new threads
            socket = newSocket;
            sendQueue.clear();
            // resend sim start message if available
            if(lastSimStartMessage != null) sendQueue.addFirst(lastSimStartMessage.toJson());
            sendThread = new Thread(this::send);
            sendThread.start();
            receiveThread = new Thread(this::receive);
            receiveThread.start();
        }

        /**
         * Reads JSON objects (0-terminated) from the socket. If any "packet" is bigger than
         * {@link #maxPacketLength}, the read bytes are immediately discarded until the next 0 byte.
         */
        private void receive() {
            InputStream in;
            try {
                in = new BufferedInputStream(socket.getInputStream());
                var buffer = new ByteArrayOutputStream(maxPacketLength);
                var readBytes = 0;
                var skipping = false;
                while (!disconnecting){
                    var b = in.read();
                    if (!skipping && b != 0) buffer.write(b);
                    if(b == -1) break; // stream ended
                    if (b == 0){
                        if (skipping){
                            skipping = false; // new packet next up
                        }
                        else {
                            // json object complete
                            JSONObject json = new JSONObject(new String(buffer.toByteArray()));
                            handleReceivedMessage(json);
                            buffer = new ByteArrayOutputStream();
                            readBytes = 0;
                        }
                    }
                    if (readBytes++ >= maxPacketLength){
                        buffer = new ByteArrayOutputStream();
                        readBytes = 0;
                        skipping = true;
                    }
                }
            } catch (IOException | JSONException e) {
                Log.log(Log.Level.ERROR, "Error receiving json object. Stop receiving.");
            }
        }

        /**
         * Handles one received document (from the remote agent).
         * @param json the json object that needs to be processed
         */
        private void handleReceivedMessage(JSONObject json) {

            Message message = Message.buildFromJson(json);
            if(message == null) {
                Log.log(Log.Level.ERROR, "Received invalid message.");
                return;
            }
            if(message instanceof ActionMessage){
                ActionMessage action = (ActionMessage) message;
                long actionID = action.getId();
                if(actionID != -1 && futureActions.containsKey(actionID)){
                    futureActions.get(actionID).complete(json);
                }
                else Log.log(Log.Level.ERROR, "Invalid action id " + actionID + " from " + name);
            }
            else{
                Log.log(Log.Level.NORMAL, "Received unknown message type from " + name);
            }
        }

        /**
         * Sends all messages from {@link #sendQueue}, blocks if it is empty.
         */
        private void send() {
            while (true) {
                if (disconnecting && sendQueue.isEmpty()) { // we can stop when everything is sent (e.g. the bye message)
                    break;
                }
                try {
                    var osw = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                    var msg = sendQueue.take();
                    osw.write(msg.toString());
                    osw.write(0);
                    osw.flush();
                } catch (IOException | InterruptedException e){
                    Log.log(Log.Level.DEBUG, name + ": Error writing to socket. Stop sending now.");
                    break;
                }
            }
        }

        /**
         * Closes socket and stops threads (if they exist).
         */
        private void close() {
            sendMessage(new ByeMessage(System.currentTimeMillis()));
            try {
                if(sendThread != null)
                    sendThread.join(5000); // give bye-message some time to be sent (but not too much)
            } catch (InterruptedException e) {
                Log.log(Log.Level.ERROR, "Interrupted while waiting for disconnection.");
            }
            if (sendThread != null) sendThread.interrupt();
            if (receiveThread != null) receiveThread.interrupt();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /**
         * Puts the given message into the send queue as soon as possible.
         * @param message the message document to send
         */
        private void sendMessage(Message message){
            while (sendQueue.size() > sendBufferSize)
                sendQueue.poll();

            try {
                sendQueue.put(message.toJson());
            } catch (InterruptedException e) {
                Log.log(Log.Level.ERROR, "Interrupted while trying to put message into queue.");
            }
        }
    }
}
