package massim.eismassim.entities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import eis.PerceptUpdate;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import massim.eismassim.Entity;
import massim.eismassim.Log;
import massim.protocol.messages.Message;
import massim.protocol.messages.StatusRequestMessage;
import massim.protocol.messages.StatusResponseMessage;

public class StatusEntity extends Entity {

    private final String host;
    private final int port;

    private final Set<Percept> statusPercepts = new HashSet<>();
    private final Set<Percept> previousStatusPercepts = new HashSet<>();

    public StatusEntity(String name, String host, int port) {
        super(name);
        this.host = host;
        this.port = port;
    }

    public static StatusResponseMessage queryServerStatus(String host, int port) throws IOException {

        Message result;
        try (var socket = new Socket(host, port);
                var out = socket.getOutputStream();
                var in = socket.getInputStream()) {
            var statusRequest = new StatusRequestMessage();
            var osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            osw.write(statusRequest.toJson().toString());
            osw.write(0);
            osw.flush();

            var buffer = new ByteArrayOutputStream();
            int read;
            while ((read = in.read()) != 0) {
                if (read == -1)
                    throw new IOException();
                buffer.write(read);
            }
            String message = buffer.toString(StandardCharsets.UTF_8);
            try {
                result = Message.buildFromJson(new JSONObject(message));
                if (result instanceof StatusResponseMessage)
                    return (StatusResponseMessage) result;
            } catch (JSONException ignored) {}
        }
        return null;
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true) {
            Log.log(getName() + ": Query server status");
            StatusResponseMessage status = null;
            try {
                status = queryServerStatus(host, port);
            } catch (IOException ignored) {}
            var percepts = new LinkedList<Percept>();
            if (status != null) {
                var teams = new ParameterList();
                for (var team : status.teams) {
                    teams.add(new Identifier(team));
                }
                percepts.add(new Percept("teams", teams));

                var teamSizes = new ParameterList();
                for (var teamSize : status.teamSizes) {
                    teamSizes.add(new Numeral(teamSize));
                }
                percepts.add(new Percept("teamSizes", teamSizes));

                percepts.add(new Percept("currentSim", new Numeral(status.currentSimulation)));

                int currentIndex = Math.max(status.currentSimulation, 0);
                percepts.add(new Percept("currentTeamSize", new Numeral(status.teamSizes[currentIndex])));
            } else {
                percepts.add(new Percept("error"));
            }

            updatePercepts(percepts);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void updatePercepts(List<Percept> percepts) {
        statusPercepts.clear();
        statusPercepts.addAll(percepts);
    }

    @Override
    public synchronized PerceptUpdate getPercepts() {
    	var addList = new ArrayList<>(statusPercepts);
		addList.removeAll(previousStatusPercepts);
		var delList = new ArrayList<>(previousStatusPercepts);
		delList.removeAll(statusPercepts);
		previousStatusPercepts.clear();
        previousStatusPercepts.addAll(statusPercepts);
        return new PerceptUpdate(addList, delList);
    }
}