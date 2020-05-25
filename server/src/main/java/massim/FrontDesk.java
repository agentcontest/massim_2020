package massim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

import massim.config.ServerConfig;
import massim.protocol.messages.AuthRequestMessage;
import massim.protocol.messages.AuthResponseMessage;
import massim.protocol.messages.Message;
import massim.protocol.messages.StatusRequestMessage;
import massim.protocol.messages.StatusResponseMessage;
import massim.util.Log;

/**
 * This is where all initial network requests go in.
 * @author ta10
 */
class FrontDesk {

    private boolean stopped = false;
    private final ServerSocket serverSocket;
    private final Thread thread;
    private final AgentManager agentManager;

    private final Status simStatus = new Status();

    /**
     * Creates a new listener waiting for incoming connections.
     * @param agentMng the agent connection manager
     * @throws IOException if socket with the given data cannot be opened
     */
    FrontDesk(AgentManager agentMng, ServerConfig config) throws IOException {
        setTeamSizes(config.teamSizes.toArray(new Integer[0]));
        agentManager = agentMng;
        serverSocket = new ServerSocket(config.port, config.backlog, null);
        thread = new Thread(() -> {
            while (!stopped) {
                try {
                    Log.log(Log.Level.DEBUG, "Waiting for connection...");
                    Socket s = serverSocket.accept();
                    Log.log(Log.Level.DEBUG,"Got a connection.");
                    Thread t = new Thread(() -> handleSocket(s));
                    t.start();
                } catch (IOException e) {
                    Log.log(Log.Level.DEBUG,"Stop listening");
                }
            }
        });
    }

    /**
     * Starts listening on the socket.
     */
    void open() {
        thread.start();
    }

    /**
     * Stops listening.
     */
    void close() {
        try {
            stopped = true;
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates and sends an auth-response message on the given socket.
     * @param s the socket to send on
     * @param result whether the authentication was successful
     */
    private void sendAuthResponse(Socket s, String result) {
        sendMessage(s, new AuthResponseMessage(System.currentTimeMillis(), result));
    }

    private void sendStatusResponse(Socket s) {
        sendMessage(s, buildStatusResponse());
    }

    private void sendMessage(Socket s, Message msg) {
        try {
            var out = s.getOutputStream();
            out.write(msg.toJson().toString().getBytes());
            out.write(0);
        } catch (IOException e) {
            Log.log(Log.Level.CRITICAL, msg.getMessageType() + " message could not be sent.");
            e.printStackTrace();
        }
    }

    /**
     * Tries to perform agent authentication on the new socket.
     * @param s the socket to use
     */
    private void handleSocket(Socket s) {
        try {
            InputStream is = s.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int b;
            while (true) {
                b = is.read();
                if (b == 0) break; // message completely read
                else if (b == -1) return; // stream ended
                else buffer.write(b);
            }

            String received = buffer.toString(StandardCharsets.UTF_8);
            JSONObject json = null;
            try {
                json = new JSONObject(received);
            } catch(JSONException e){
                Log.log(Log.Level.ERROR, "Invalid JSON object received: " + received);
            }
            Message msg = Message.buildFromJson(json);

            if(msg != null){
                if(msg instanceof AuthRequestMessage) {
                    AuthRequestMessage auth = (AuthRequestMessage) msg;
                    Log.log(Log.Level.NORMAL, "got authentication: username=" + auth.getUsername() + " password="
                            + auth.getPassword() + " address=" + s.getInetAddress().getHostAddress());
                    // check credentials and act accordingly
                    if (agentManager.auth(auth.getUsername(), auth.getPassword())) {
                        Log.log(Log.Level.NORMAL, auth.getUsername() + " authentication successful");
                        sendAuthResponse(s, AuthResponseMessage.OK);
                        agentManager.handleNewConnection(s, auth.getUsername());
                    } else {
                        Log.log(Log.Level.ERROR, "Got invalid authentication from: " + s.getInetAddress().getHostAddress());
                        sendAuthResponse(s, AuthResponseMessage.FAIL);
                        try {
                            s.close();
                        } catch (IOException ignored) {}
                    }
                }
                else if (msg instanceof StatusRequestMessage) {
                    Log.log(Log.Level.DEBUG, "Got status request from: " + s.getInetAddress().getHostAddress());
                    sendStatusResponse(s);
                }
                else{
                    Log.log(Log.Level.ERROR, "Expected AuthRequest, Received message of type: " + msg.getClass());
                }
            }
            else{
                Log.log(Log.Level.ERROR, "Cannot handle message: " + received);
            }
        } catch (IOException e) {
            Log.log(Log.Level.ERROR, "Error while receiving authentication message");
            e.printStackTrace();
        }
    }

    public void setTeams(String[] teams) {
        synchronized (simStatus) {
            simStatus.teams = teams;
        }
    }

    private void setTeamSizes(Integer[] teamSizes) {
        synchronized (simStatus) {
            simStatus.teamSizes = teamSizes;
        }
    }

    public void setCurrentSimulation(int currentSimulation) {
        synchronized (simStatus) {
            simStatus.currentSimulation = currentSimulation;
        }
    }

    private StatusResponseMessage buildStatusResponse() {
        synchronized (simStatus) {
            return new StatusResponseMessage(System.currentTimeMillis(), simStatus.teams, simStatus.teamSizes,
                    simStatus.currentSimulation);
        }
    }

    static class Status {
        String[] teams = new String[0];
        Integer[] teamSizes = new Integer[0];
        int currentSimulation = -1;
    }
}
