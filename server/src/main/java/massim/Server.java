package massim;

import massim.config.ServerConfig;
import massim.config.TeamConfig;
import massim.monitor.Monitor;
import massim.game.Simulation;
import massim.util.IOUtil;
import massim.util.InputManager;
import massim.util.Log;
import massim.util.RNG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created in 2017.
 * MASSim server main class/entry point.
 * @author ta10
 */
public class Server {

    private ServerConfig config;

    private final InputManager inputManager = new InputManager();
    private FrontDesk frontDesk;
    private AgentManager agentManager;
    private Monitor monitor;
    private ReplayWriter replayWriter;

    /**
     * whether server should stop after the next match (random mode)
     */
    private boolean stopped = false;

    public static void main(String[] args){

        Server server = new Server();

        int monitorPort = 0;

        // parse command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]){

                case "-conf":
                    try {
                        server.config = parseServerConfig(IOUtil.readJSONObjectWithImport(args[++i]));
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                        Log.log(Log.Level.ERROR, "Could not read massim.config file.");
                        i--;
                    }
                    break;
                case "-confString":
                    try {
                        server.config = parseServerConfig(new JSONObject(args[++i]));
                    } catch (JSONException e) {
                        Log.log(Log.Level.ERROR, "Passed configuration string invalid.");
                        i--;
                    }
                    break;
                case "--monitor":
                    if (i + 1 < args.length) {
                        try {
                            monitorPort = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            monitorPort = 8000;
                            i--;
                        }
                    } else {
                        monitorPort = 8000;
                    }
                    break;

                default:
                    Log.log(Log.Level.ERROR, "Unknown option: " + args[i]);
            }
        }

        // ask to choose massim.config file from conf directory
        if (server.config == null){
            File confDir = new File("conf");
            confDir.mkdirs();
            File[] confFiles = confDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (confFiles == null || confFiles.length == 0) {
                Log.log(Log.Level.NORMAL, "No massim.config files to load - exit MASSim.");
                System.exit(0);
            }
            else {
                Log.log(Log.Level.NORMAL, "Choose a number:");
                for (int i = 0; i < confFiles.length; i++) {
                    Log.log(Log.Level.NORMAL, i + " " + confFiles[i]);
                }
                Scanner in = new Scanner(System.in);
                Integer confNum = null;
                while (confNum == null) {
                    try {
                        confNum = Integer.parseInt(in.next());
                        if (confNum < 0 || confNum > confFiles.length - 1){
                            Log.log(Log.Level.NORMAL, "No massim.config for that number, try again:");
                            confNum = null;
                        }
                    } catch (Exception e) {
                        Log.log(Log.Level.NORMAL, "Invalid number, try again:");
                    }
                }
                try {
                    server.config = parseServerConfig(IOUtil.readJSONObjectWithImport(confFiles[confNum].getPath()));
                } catch (IOException e) {
                    Log.log(Log.Level.ERROR, "Could not read massim.config file, exiting MASSim");
                    System.exit(0);
                }
            }
        }
        server.config.monitorPort = monitorPort;

        if (server.config.replayPath != null) {
            server.replayWriter = new ReplayWriter(server.config.replayPath);
        }

        server.go();
        server.close();
    }

    /**
     * Cleanup all threads etc.
     */
    private void close() {
        Log.log(Log.Level.NORMAL, "All simulations run - server ending now.");
        if (frontDesk != null) frontDesk.close();
        if (agentManager != null) agentManager.stop();
        inputManager.stop();
    }

    /**
     * Starts server operation according to its configuration.
     */
    private void go(){

        //setup text I/O
        switch(config.logLevel){
            case "debug": Log.setLogLevel(Log.Level.DEBUG); break;
            case "error": Log.setLogLevel(Log.Level.ERROR); break;
            case "critical": Log.setLogLevel(Log.Level.CRITICAL); break;
            default: Log.setLogLevel(Log.Level.NORMAL);
        }
        if(config.logPath != null){
            File logFile = new File(config.logPath + File.separator + "MASSim-log-" + timestamp() + ".log");
            File dir = logFile.getParentFile();
            if(!dir.exists()) dir.mkdirs();
            Log.setLogFile(logFile);
        }
        inputManager.start();

        // setup backend
        agentManager = new AgentManager(config.teams, config.agentTimeout, config.maxPacketLength);
        try {
            frontDesk = new FrontDesk(agentManager, config);
            frontDesk.open();
        } catch (IOException e) {
            Log.log(Log.Level.CRITICAL, "Cannot open server socket.");
            return;
        }

        // setup monitor
        if (config.monitorPort > 0) try {
            monitor = new Monitor(config.monitorPort);
        } catch (ExecutionException e) {
            Log.log(Log.Level.ERROR, "Monitor not started: " + e.getLocalizedMessage());
        } catch (InterruptedException ignored) {}

        // delay tournament start according to launch type
        if (config.launch.equals("key")){
            Log.log(Log.Level.NORMAL,"Please press ENTER to start the tournament.");
            synchronized (inputManager) {
                try {inputManager.wait();} catch (InterruptedException ignored) {}
            }
        }
        else if(config.launch.endsWith("s")){
            try{
                int interval = Integer.parseInt(config.launch.substring(0, config.launch.length() - 1));
                Log.log(Log.Level.NORMAL, "Starting tournament in " + interval + " seconds.");
                Thread.sleep(interval * 1000);
            } catch(Exception e){
                Log.log(Log.Level.ERROR, "Failed waiting, starting tournament now.");
            }
        }
        else{
            Calendar now = Calendar.getInstance();
            Calendar startAt = Calendar.getInstance();
            Log.log(Log.Level.NORMAL, "Current time is: " + now.getTime().toString());
            try {
                startAt.setTime(new SimpleDateFormat("HH:mm").parse(config.launch));
                startAt.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                Log.log(Log.Level.NORMAL,"Starting time: " + startAt.getTime().toString());
                long diffTime = startAt.getTimeInMillis() - now.getTimeInMillis();
                if(diffTime > 0) {
                    Log.log(Log.Level.NORMAL, "The tournament will start in " + diffTime / 1000 + " seconds.");
                    Thread.sleep(diffTime);
                }
            } catch (Exception e) {
                Log.log(Log.Level.ERROR, "Could not determine or wait for start time. Starting tournament now.");
            }
        }

        // run matches according to tournament mode
        switch(config.tournamentMode){
            case ServerConfig.MODE_ROUND_ROBIN:
                // run a match for each team combination
                if (config.teamsPerMatch > config.teams.size()){
                    Log.log(Log.Level.ERROR, "Not enough teams configured. Stopping MASSim now.");
                    System.exit(0);
                }
                int[] indices = IntStream.rangeClosed(0, config.teamsPerMatch - 1).toArray();
                boolean nextMatch = true;
                while (nextMatch){
                    Set<TeamConfig> matchTeams = new HashSet<>();
                    for (int index : indices) matchTeams.add(config.teams.get(index));

                    runMatch(matchTeams);

                    // determine the next team constellation
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] < config.teams.size() - 1 - (indices.length - 1 - i)){
                            indices[i]++;
                            for (int j = i + 1; j < indices.length; j++){
                                indices[j] = indices[i] + (j - i);
                            }
                            break;
                        }
                        if (i == 0) nextMatch = false; // no team constellation left
                    }
                }
                break;
            case ServerConfig.MODE_MANUAL:
                if(config.manualModeTeams != null) config.manualModeTeams.forEach(this::runMatch);
                break;
            case ServerConfig.MODE_RANDOM:
                while(!stopped){
                    List<TeamConfig> teams = new Vector<>(config.teams);
                    RNG.shuffle(teams);
                    runMatch(new HashSet<>(teams.subList(0, config.teamsPerMatch)));
                }
                break;
            default:
                Log.log(Log.Level.ERROR, "Invalid tournament mode: " + config.tournamentMode);
        }
    }

    /**
     * @return a string representation of the current time in the form yyyy-MM-dd-HH-mm-ss
     */
    private String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    }


    /**
     * Stops running matches after the current one finishes (random tournament mode)
     */
    public void stop(){
        stopped = true;
    }

    /**
     * Runs a match for the given teams. Sim configuration is taken from the server config.
     * @param matchTeams a set of all teams to participate in the simulation
     */
    private void runMatch(Set<TeamConfig> matchTeams) {

        frontDesk.setTeams(matchTeams.stream().map(TeamConfig::getName).toArray(String[]::new));

        var startTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        var result = new JSONObject();

        for (var simIndex = 0; simIndex < config.simConfigs.size(); simIndex++){
            frontDesk.setCurrentSimulation(simIndex);
            var simConfig = config.simConfigs.get(simIndex);

            // initialize random
            long randomSeed = simConfig.optLong("randomSeed", System.currentTimeMillis());
            Log.log(Log.Level.NORMAL, "Configuring random seed: " + randomSeed);
            RNG.initialize(randomSeed);
            // create and run simulation instance with the given teams
            Simulation sim = new Simulation();

            int steps = simConfig.getInt("steps");

            // handle initial state
            var initialPercepts = sim.init(steps, simConfig, matchTeams);
            handleSimState(sim.getName(), startTime, sim.getStaticData());
            handleSimState(sim.getName(), startTime, sim.getSnapshot());
            agentManager.handleInitialPercepts(initialPercepts);

            // handle steps
            for (int i = 0; i < steps; i++){
                Log.log(Log.Level.NORMAL, "Simulation at step " + i);
                handleInputs(sim);
                var percepts = sim.preStep(i);
                var actions = agentManager.requestActions(percepts);
                sim.step(i, actions); // execute step with agent actions
                handleSimState(sim.getName(), startTime, sim.getSnapshot());
                if (monitor != null) monitor.updateStatus(sim.getStatusSnapshot());
            }

            // handle final state
            var finalPercepts = sim.finish();
            agentManager.handleFinalPercepts(finalPercepts);
            result.put(sim.getName(), sim.getResult());

            // pause between simulations
            if (config.waitBetweenSimulations > 0) {
                Log.log(Log.Level.NORMAL, "Waiting " + config.waitBetweenSimulations + "ms before starting the next round.");
                try {
                    TimeUnit.MILLISECONDS.sleep(config.waitBetweenSimulations);
                } catch (InterruptedException ignored) {}
            }
        }

        // write match result to file
        IOUtil.writeJSONToFile(result, new File(config.resultPath + File.separator + "result_" + timestamp() + ".json"));
    }

    /**
     * Takes and processes all inputs from the input manager
     * @param sim the simulation that may receive some of the commands
     */
    private void handleInputs(Simulation sim) {
        boolean paused = false;
        // read inputs if inputs are available or execution is paused
        while(inputManager.hasInput() || paused){
            try {
                String[] inputWords = inputManager.take().split(" ");
                switch (inputWords[0]) {
                    case "pause":
                        paused = true;
                        Log.log(Log.Level.NORMAL, "Simulation paused. Type further commands or 'continue' to proceed.");
                        break;
                    case "continue":
                        paused = false;
                        break;
                    default:
                        handleCommand(inputWords);
                        sim.handleCommand(inputWords);
                        break;
                }
            } catch (InterruptedException e) {
                Log.log(Log.Level.ERROR, "Interrupted while waiting for input.");
                return;
            }
        }
    }

    /**
     * Handles a single command
     * @param command the already split command string
     */
    private void handleCommand(String[] command) {
        // TODO handle something
        Log.log(Log.Level.NORMAL, "Command received: " + command[0]);
    }

    /**
     * Handles snapshots of the world state, i.e. notifies monitor and saves replays (if configured).
     * @param simId the ID of the current sim
     * @param startTime string representation of the simulation's start time
     * @param world the world state
     */
    private void handleSimState(String simId, String startTime, JSONObject world) {
        if (monitor != null) monitor.updateState(world);
        if (replayWriter != null) replayWriter.updateState(simId, startTime, world);
    }

    /**
     * Parses the given JSONObject into a new {@link ServerConfig} object.
     * Uses default values if the JSONObject is "broken" somehow.
     * @param conf the JSONObject (configuration) holding a "server" JSONObject
     * @return the parsed server config
     */
    public static ServerConfig parseServerConfig(JSONObject conf){
        ServerConfig config = new ServerConfig();
        JSONObject serverJSON = conf.getJSONObject("server");
        if (serverJSON == null) {
            Log.log(Log.Level.ERROR, "No server object in configuration.");
            serverJSON = new JSONObject();
        }
        config.launch = serverJSON.getString("launch");
        Log.log(Log.Level.NORMAL, "Configuring launch type: " + config.launch);
        config.tournamentMode = serverJSON.getString("tournamentMode");
        Log.log(Log.Level.NORMAL, "Configuring tournament mode: " + config.tournamentMode);
        config.teamsPerMatch = serverJSON.getInt("teamsPerMatch");
        Log.log(Log.Level.NORMAL, "Configuring teams per match: " + config.teamsPerMatch);
        config.port = serverJSON.getInt("port");
        Log.log(Log.Level.NORMAL, "Configuring port: " + config.port);
        config.backlog = serverJSON.getInt("backlog");
        Log.log(Log.Level.NORMAL, "Configuring backlog: " + config.backlog);
        config.agentTimeout = serverJSON.getInt("agentTimeout");
        Log.log(Log.Level.NORMAL, "Configuring agent timeout: " + config.agentTimeout);
        config.logPath = serverJSON.getString("logPath");
        Log.log(Log.Level.NORMAL, "Configuring log path: " + config.logPath);
        config.logLevel = serverJSON.getString("logLevel");
        Log.log(Log.Level.NORMAL, "Configuring log level: " + config.logLevel);
        config.resultPath = serverJSON.getString("resultPath");
        Log.log(Log.Level.NORMAL, "Configuring result path: " + config.resultPath);
        config.maxPacketLength = serverJSON.getInt("maxPacketLength");
        Log.log(Log.Level.NORMAL, "Configuring max packet length: " + config.maxPacketLength);
        config.replayPath = serverJSON.getString("replayPath");
        Log.log(Log.Level.NORMAL, "Configuring replay path: " + config.replayPath);
        config.waitBetweenSimulations = serverJSON.optInt("waitBetweenSimulations");
        Log.log(Log.Level.NORMAL, "Configuring wait time: " + config.waitBetweenSimulations);

        // parse matches
        JSONArray matchJSON = conf.getJSONArray("match");
        if (matchJSON == null){
            Log.log(Log.Level.ERROR, "No match configured.");
            System.exit(0);
        }
        for(int i = 0; i < matchJSON.length(); i++){
            JSONObject simConfig = matchJSON.getJSONObject(i);
            if (simConfig != null) {
                config.simConfigs.add(simConfig);
            }
        }

        // determine number of required agents (max. number of agents among all simulations)
        int requiredAgents = 0;
        for (int i = 0; i < matchJSON.length(); i++) {
            var match = matchJSON.getJSONObject(i);
            var entities = match.getJSONObject("entities");
            int numberOfAgents = 0;
            for (String role : entities.keySet()) {
                numberOfAgents += entities.getInt(role);
            }
            config.teamSizes.add(numberOfAgents);
            requiredAgents = Math.max(requiredAgents, numberOfAgents);
        }

        // parse teams
        var teamsJSON = conf.getJSONObject("teams");
        var allAgents = new HashSet<String>();
        if (teamsJSON == null) Log.log(Log.Level.ERROR, "No teams configured.");
        else{
            for (String teamName : teamsJSON.keySet()) {
                TeamConfig team = new TeamConfig(teamName);
                config.teams.add(team);
                var teamJSON = teamsJSON.getJSONObject(teamName);
                if (teamJSON != null) {
                    var prefix = teamJSON.getString("prefix");
                    var password = teamJSON.getString("password");
                    for (int i = 1; i <= requiredAgents; i++) {
                        var agentName = prefix + teamName + i;
                        if (!allAgents.add(agentName))
                            Log.log(Log.Level.CRITICAL, "Agent " + agentName + " occurs in multiple teams.");
                        team.addAgent(agentName, password);
                        config.accounts.put(agentName, password);
                    }
                }
            }
        }

        // parse manual mode config (if required)
        if(config.tournamentMode.equals(ServerConfig.MODE_MANUAL)){
            Map<String, TeamConfig> teamMap = config.teams.stream()
                    .collect(Collectors.toMap(TeamConfig::getName, t -> t));
            List<Set<TeamConfig>> matchTeams = new Vector<>();
            JSONArray manualConf = conf.getJSONArray("manual-mode");
            if (manualConf == null){
                Log.log(Log.Level.CRITICAL, "No teams configured for manual mode. Exiting.");
                System.exit(0);
            }
            for(int i = 0; i < manualConf.length(); i++){
                JSONArray teamList = manualConf.getJSONArray(i);
                if(teamList != null){
                    Set<TeamConfig> parsedTeamNames = new HashSet<>();
                    for (int j = 0; j < config.teamsPerMatch; j++){
                        String team = teamList.optString(i, "");
                        if (team.equals("")) break;
                        TeamConfig teamConfig = teamMap.get(team);

                        if(teamConfig != null) parsedTeamNames.add(teamMap.get(team));
                        else Log.log(Log.Level.ERROR, "No team with name " + team + " configured.");
                    }
                    if (parsedTeamNames.size() == config.teamsPerMatch) matchTeams.add(parsedTeamNames);
                }
            }
            config.manualModeTeams = matchTeams;
        }
        return config;
    }
}
