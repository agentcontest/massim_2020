package massim;

import massim.protocol.messages.AuthRequestMessage;
import massim.protocol.messages.AuthResponseMessage;
import massim.protocol.messages.Message;
import massim.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Responsible for network actions.
 * Created in 2017.
 * @author ta10
 */
class LoginManager {

    private boolean stopped = false;
    private ServerSocket serverSocket;
    private Thread thread;
    private AgentManager agentManager;


    /**
     * Creates a new listener waiting for incoming connections.
     * @param port the port on which to listen
     * @param backlog the backlog of the socket
     * @param agentMng the agent connection manager
     * @throws IOException if socket with the given data cannot be opened
     */
    LoginManager(AgentManager agentMng, int port, int backlog) throws IOException {
        agentManager = agentMng;
        serverSocket = new ServerSocket(port, backlog, null);
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
    void start() {
        thread.start();
    }

    /**
     * Stops listening.
     */
    void stop() {
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
        try {
            OutputStream out = s.getOutputStream();
            AuthResponseMessage msg = new AuthResponseMessage(System.currentTimeMillis(), result);
            out.write(msg.toJson().toString().getBytes());
            out.write(0);
        } catch (IOException e) {
            Log.log(Log.Level.CRITICAL, "Auth response could not be sent.");
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
}
