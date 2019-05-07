package massim.simulation.game;

import massim.config.TeamConfig;
import massim.protocol.data.Position;
import massim.protocol.data.TaskInfo;
import massim.protocol.data.Thing;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.protocol.messages.scenario.Actions;
import massim.protocol.messages.scenario.InitialPercept;
import massim.protocol.messages.scenario.StepPercept;
import massim.simulation.game.environment.*;
import massim.util.Log;
import massim.util.RNG;
import massim.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * State of the game.
 */
class GameState {

    private static Map<Integer, Terrain> terrainColors =
            Map.of(-16777216, Terrain.OBSTACLE, -1, Terrain.EMPTY, -65536, Terrain.GOAL);

    private Map<String, Team> teams = new HashMap<>();
    private Map<String, Entity> agentToEntity = new HashMap<>();
    private Map<Entity, String> entityToAgent = new HashMap<>();

    private int step = -1;
    private Grid grid;
    private Map<String, GameObject> gameObjects = new HashMap<>();
    private Map<Position, Dispenser> dispensers = new HashMap<>();
    private Map<String, Task> tasks = new HashMap<>();
    private Set<String> blockTypes = new TreeSet<>();

    // config parameters
    private int randomFail;
    private double pNewTask;
    private int taskDurationMin;
    private int taskDurationMax;
    private int taskSizeMin;
    private int taskSizeMax;

    GameState(JSONObject config, Set<TeamConfig> matchTeams) {
        // parse simulation config
        randomFail = config.getInt("randomFail");
        Log.log(Log.Level.NORMAL, "config.randomFail: " + randomFail);
        int attachLimit = config.getInt("attachLimit");
        Log.log(Log.Level.NORMAL, "config.attachLimit: " + attachLimit);
        var blockTypeBounds = config.getJSONArray("blockTypes");
        var numberOfBlockTypes = RNG.betweenClosed(blockTypeBounds.getInt(0), blockTypeBounds.getInt(1));
        Log.log(Log.Level.NORMAL, "config.blockTypes: " + blockTypeBounds + " -> " + numberOfBlockTypes);
        for (int i = 0; i < numberOfBlockTypes; i++) {
            blockTypes.add("b" + i);
        }
        var dispenserBounds = config.getJSONArray("dispensers");
        Log.log(Log.Level.NORMAL, "config.dispensersBounds: " + dispenserBounds);

        var taskConfig = config.getJSONObject("tasks");
        var taskDurationBounds = taskConfig.getJSONArray("duration");
        Log.log(Log.Level.NORMAL, "config.tasks.duration: " + taskDurationBounds);
        taskDurationMin = taskDurationBounds.getInt(0);
        taskDurationMax = taskDurationBounds.getInt(1);
        var taskSizeBounds = taskConfig.getJSONArray("size");
        Log.log(Log.Level.NORMAL, "config.tasks.size: " + taskSizeBounds);
        taskSizeMin = taskSizeBounds.getInt(0);
        taskSizeMax = taskSizeBounds.getInt(1);
        pNewTask = taskConfig.getInt("probability");
        Log.log(Log.Level.NORMAL, "config.tasks.probability: " + pNewTask);

        // create teams
        matchTeams.forEach(team -> teams.put(team.getName(), new Team(team.getName())));

        // create grid environment
        JSONObject gridConf = config.getJSONObject("grid");
        var gridX = gridConf.getInt("width");
        var gridY = gridConf.getInt("height");
        grid = new Grid(gridX, gridY, attachLimit);

        // read bitmap if available
        String mapFilePath = gridConf.optString("file");
        if (!mapFilePath.equals("")){
            var mapFile = new File(mapFilePath);
            if (mapFile.exists()) {
                try {
                    BufferedImage img = ImageIO.read(mapFile);
                    var width = Math.min(gridX, img.getWidth());
                    var height = Math.min(gridY, img.getHeight());
                    for (int x = 0; x < width; x++) { for (int y = 0; y < height; y++) {
                        grid.setTerrain(x, y, terrainColors.getOrDefault(img.getRGB(x, y), Terrain.EMPTY));
                    }}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else Log.log(Log.Level.ERROR, "File " + mapFile.getAbsolutePath() + " not found.");
        }

        // create entities
        JSONArray entities = config.getJSONArray("entities");
        for (var type = 0; type < entities.length(); type++) {
            var entityConf = entities.optJSONObject(type);
            if (entityConf != null){
                var roleName = entityConf.keys().next();
                var amount = entityConf.optInt(roleName, 0);
                for (var n = 0; n < amount; n++){
                    var position = grid.findRandomFreePosition(); // entities from the same team start on the same spot
                    for (TeamConfig team: matchTeams) {
                        String agentName;
                        if(n < team.getAgentNames().size()) {
                            agentName = team.getAgentNames().get(n);
                        }
                        else {
                            agentName = team.getName() + "-unconfigured-" + n;
                            Log.log(Log.Level.ERROR, "Too few agents configured for team " + team.getName()
                                    + ", using agent name " + agentName + ".");
                        }
                        createEntity(position, agentName, team.getName());
                    }
                }
            }
        }

        // create env. things
        for (var block : blockTypes) {
            var numberOfDispensers = RNG.betweenClosed(dispenserBounds.getInt(0), dispenserBounds.getInt(1));
            for (var i = 0; i < numberOfDispensers; i++) {
                createDispenser(grid.findRandomFreePosition(), block);
            }
        }

        // check for setup file
        var setupFilePath = config.optString("setup");
        if (!setupFilePath.equals("")){
            Log.log(Log.Level.NORMAL, "Running setup actions");
            try {
                var b = new BufferedReader(new FileReader(setupFilePath));
                var line = "";
                while ((line = b.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    handleCommand(line.split(" "));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Team> getTeams() {
        return this.teams;
    }

    void handleCommand(String[] command) {
        switch (command[0]) {
            case "move":
                if (command.length != 4) break;
                var x = Util.tryParseInt(command[1]);
                var y = Util.tryParseInt(command[2]);
                var entity = agentToEntity.get(command[3]);

                if (entity == null || x == null || y == null) break;
                Log.log(Log.Level.NORMAL, "Setup: Try to move " + command[3] + " to (" + x +", " + y + ")");
                grid.move(entity, Position.of(x, y));
                break;

            case "add":
                if (command.length != 5) break;
                x = Util.tryParseInt(command[1]);
                y = Util.tryParseInt(command[2]);
                if (x == null || y == null) break;
                switch (command[3]) {
                    case "block":
                        var blockType = command[4];
                        createBlock(Position.of(x, y), blockType);
                        break;
                    case "dispenser":
                        blockType = command[4];
                        createDispenser(Position.of(x, y), blockType);
                        break;
                    default:
                        Log.log(Log.Level.ERROR, "Cannot add " + command[3]);
                }
                break;

            case "create":
                if (command.length != 5) break;
                if (command[1].equals("task")) {
                    var name = command[2];
                    var duration = Util.tryParseInt(command[3]);
                    var requirements = command[4].split(";");
                    if (duration == null) break;
                    var requireMap = new HashMap<Position, String>();
                    Arrays.stream(requirements).map(req -> req.split(",")).forEach(req -> {
                        var bx = Util.tryParseInt(req[0]);
                        var by = Util.tryParseInt(req[1]);
                        var blockType = req[2];
                        if (bx != null && by != null) {
                            requireMap.put(Position.of(bx, by), blockType);
                        }
                    });
                    createTask(name, duration, requireMap);
                }
                break;

            case "attach":
                if (command.length != 5) break;
                var x1 = Util.tryParseInt(command[1]);
                var y1 = Util.tryParseInt(command[2]);
                var x2 = Util.tryParseInt(command[3]);
                var y2 = Util.tryParseInt(command[4]);
                if (x1 == null || x2 == null || y1 == null || y2 == null) break;
                Attachable a1 = getAttachable(Position.of(x1, y1));
                Attachable a2 = getAttachable(Position.of(x2, y2));
                if (a1 == null || a2 == null) break;
                grid.attach(a1, a2);
                break;
            default:
                Log.log(Log.Level.ERROR, "Cannot handle command " + Arrays.toString(command));
        }
    }

    int getRandomFail() {
        return this.randomFail;
    }

    public Grid getGrid() {
        return grid;
    }

    /**
     * @return the agent entity of the given name or null if it does not exist
     */
    Entity getEntityByID(String goID) {
        GameObject entity = gameObjects.get(goID);
        if (!(entity instanceof Entity)) return null;
        return (Entity) entity;
    }

    Entity getEntityByName(String agentName) {
        return agentToEntity.get(agentName);
    }

    Map<String, SimStartMessage> getInitialPercepts(int steps) {
        Map<String, SimStartMessage> result = new HashMap<>();
        for (Entity e: entityToAgent.keySet()) {
            result.put(e.getAgentName(), new InitialPercept(e.getAgentName(), e.getTeamName(), steps));
        }
        return result;
    }

    Map<String, RequestActionMessage> prepareStep(int step) {
        this.step = step;

        //handle tasks
        if (RNG.nextDouble() < pNewTask) {
            createTask(RNG.betweenClosed(taskDurationMin, taskDurationMax), RNG.betweenClosed(taskSizeMin, taskSizeMax));
        }

        return getStepPercepts();
    }

    private Map<String, RequestActionMessage> getStepPercepts(){
        Map<String, RequestActionMessage> result = new HashMap<>();
        Set<TaskInfo> allTasks = tasks.values().stream()
                .filter(t -> !t.isCompleted())
                .map(Task::toPercept)
                .collect(Collectors.toSet());
        for (Entity entity : entityToAgent.keySet()) {
            int vision = entity.getVision();
            Position pos = entity.getPosition();
            Set<Thing> visibleThings = new HashSet<>();
            Map<String, Set<Position>> visibleTerrain = new HashMap<>();
            for (int dy = -vision; dy <= vision ; dy++) {
                int y = pos.y + dy;
                int visionLeft = vision - Math.abs(dy);
                for (int x = pos.x - visionLeft ; x <= pos.x + visionLeft; x++) {
                    Position currentPos = Position.of(x, y);
                    GameObject go = getGameObject(currentPos);
                    if (go != null) visibleThings.add(go.toPercept(entity.getPosition()));
                    Terrain terrain = grid.getTerrain(currentPos);
                    if (terrain != Terrain.EMPTY) {
                        visibleTerrain.computeIfAbsent(terrain.name,
                                t -> new HashSet<>()).add(currentPos.toLocal(entity.getPosition()));
                    }
                }
            }
            result.put(entity.getAgentName(), new StepPercept(step, teams.get(entity.getTeamName()).getScore(),
                    visibleThings, visibleTerrain, allTasks, entity.getLastAction(), entity.getLastActionResult()));
        }
        return result;
    }

    Map<String, SimEndMessage> getFinalPercepts() {
        Map<String, SimEndMessage> result = new HashMap<>();
        List<Team> teamsSorted = new ArrayList<>(teams.values());
        teamsSorted.sort((t1, t2) -> (int) (t2.getScore() - t1.getScore()));
        Map<Team, Integer> rankings = new HashMap<>();
        for (int i = 0; i < teamsSorted.size(); i++) {
            rankings.put(teamsSorted.get(i), i + 1);
        }
        for (Entity e: entityToAgent.keySet()) {
            Team team = teams.get(e.getTeamName());
            result.put(e.getAgentName(), new SimEndMessage(team.getScore(), rankings.get(team)));
        }
        return result;
    }

    String handleMoveAction(Entity entity, String direction) {
        if (grid.moveWithAttached(entity, direction, 1)) {
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F_PATH;
    }

    String handleRotateAction(Entity entity, boolean clockwise) {
        if (grid.rotateWithAttached(entity, clockwise)) {
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleAttachAction(Entity entity, String direction) {
        Position target = entity.getPosition().moved(direction, 1);
        if (target == null) return Actions.RESULT_F_TARGET;
        GameObject gameObject = getGameObject(target);
        if (!(gameObject instanceof Attachable)) return Actions.RESULT_F;
        Attachable a = (Attachable) gameObject;
        if(!attachedToOpponent(a, entity) && grid.attach(entity, a)) {
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleDetachAction(Entity entity, String direction) {
        Position target = entity.getPosition().moved(direction, 1);
        Attachable a = getAttachable(target);
        if (a == null) return Actions.RESULT_F_TARGET;
        if (grid.detach(entity, a)){
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleConnectAction(Entity entity, Position blockPos, Entity partnerEntity, Position partnerBlockPos) {
        Attachable block1 = getAttachable(blockPos.translate(entity.getPosition()));
        Attachable block2 = getAttachable(partnerBlockPos.translate(partnerEntity.getPosition()));

        if(!(block1 instanceof Block) || !(block2 instanceof Block)) return Actions.RESULT_F_TARGET;

        Set<Attachable> attachables = grid.getAllAttached(entity);
        if (attachables.contains(partnerEntity)) return Actions.RESULT_F;
        if (!attachables.contains(block1)) return Actions.RESULT_F_TARGET;
        if (attachables.contains(block2)) return Actions.RESULT_F_TARGET;

        Set<Attachable> partnerAttachables = grid.getAllAttached(partnerEntity);
        if (!partnerAttachables.contains(block2)) return Actions.RESULT_F_TARGET;
        if (partnerAttachables.contains(block1)) return Actions.RESULT_F_TARGET;

        if(grid.attach(block1, block2)){
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleRequestAction(Entity entity, String direction) {
        Position requestPosition = entity.getPosition().moved(direction, 1);
        Dispenser dispenser = dispensers.get(requestPosition);
        if (dispenser != null && grid.isFree(requestPosition)){
            createBlock(requestPosition, dispenser.getBlockType());
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleSubmitAction(Entity e, String taskName) {
        Task task = tasks.get(taskName);
        if (task == null || task.isCompleted()) return Actions.RESULT_F_TARGET;
        Position ePos = e.getPosition();
        if (grid.getTerrain(ePos) != Terrain.GOAL) return Actions.RESULT_F;
        Set<Attachable> attachedBlocks = grid.getAllAttached(e);
        for (Map.Entry<Position, String> entry : task.getRequirements().entrySet()) {
            Position pos = entry.getKey();
            String reqType = entry.getValue();
            Position checkPos = Position.of(pos.x + ePos.x, pos.y + ePos.y);
            Attachable actualBlock = getAttachable(checkPos);
            if (actualBlock instanceof Block
                && ((Block) actualBlock).getBlockType().equals(reqType)
                && attachedBlocks.contains(actualBlock)) {
                continue;
            }
            return Actions.RESULT_F;
        }
        task.getRequirements().keySet().forEach(pos -> {
            Attachable a = getAttachable(pos.translate(e.getPosition()));
            if (a != null) {
                grid.removeAttachable(a);
                gameObjects.remove(a.getID());
            }
        });
        teams.get(e.getTeamName()).addScore(task.getReward());
        task.complete();
        return Actions.RESULT_SUCCESS;
    }

    Task createTask(int duration, int size) {
        if (size < 1) return null;
        var name = "task" + tasks.values().size();
        var requirements = new HashMap<Position, String>();
        var blockList = new ArrayList<>(blockTypes);
        Position lastPosition = Position.of(0, 1);
        requirements.put(lastPosition, blockList.get(RNG.nextInt(blockList.size())));
        for (int i = 0; i < size - 1; i++) {
            int index = RNG.nextInt(blockTypes.size());
            double direction = RNG.nextDouble();
            if (direction <= .3) {
                lastPosition = lastPosition.translate(-1, 0);
            }
            else if (direction <= .6) {
                lastPosition = lastPosition.translate(1, 0);
            }
            else {
                lastPosition = lastPosition.translate(0, 1);
            }
            requirements.put(lastPosition, blockList.get(index));
        }
        Task t = new Task(name, step + duration, requirements);
        tasks.put(t.getName(), t);
        return t;
    }

    Task createTask(String name, int duration, Map<Position, String> requirements) {
        if (requirements.size() == 0) return null;
        Task t = new Task(name, step + duration, requirements);
        tasks.put(t.getName(), t);
        return t;
    }

    private Entity createEntity(Position xy, String name, String teamName) {
        Entity e = grid.createEntity(xy, name, teamName);
        registerGameObject(e);
        agentToEntity.put(name, e);
        entityToAgent.put(e, name);
        return e;
    }

    private Block createBlock(Position xy, String blockType) {
        if (!blockTypes.contains(blockType)) return null;
        Block b = grid.createBlock(xy, blockType);
        if (b == null) return null;
        registerGameObject(b);
        return b;
    }

    private void createDispenser(Position xy, String blockType) {
        if (!blockTypes.contains(blockType)) return;
        Dispenser d = new Dispenser(xy, blockType);
        registerGameObject(d);
        dispensers.put(xy, d);
    }

    private void registerGameObject(GameObject o) {
        this.gameObjects.put(o.getID(), o);
    }

    private Attachable getAttachable(Position position) {
        GameObject go = getGameObject(position);
        return (go instanceof Attachable)? (Attachable) go : null;
    }

    private GameObject getGameObject(Position pos) {
        return gameObjects.get(grid.getCollidable(pos));
    }

    private boolean attachedToOpponent(Attachable a, Entity entity) {
        return grid.getAllAttached(a).stream().anyMatch(other -> other instanceof Entity && ofDifferentTeams((Entity) other, entity));
    }

    private boolean ofDifferentTeams(Entity e1, Entity e2) {
        return !e1.getTeamName().equals(e2.getTeamName());
    }

    JSONObject takeSnapshot() {
        JSONObject snapshot = new JSONObject();
        JSONArray entities = new JSONArray();
        snapshot.put("entities", entities);
        JSONArray blocks = new JSONArray();
        snapshot.put("blocks", blocks);
        JSONArray dispensers = new JSONArray();
        snapshot.put("dispensers", dispensers);
        JSONArray taskArr = new JSONArray();
        snapshot.put("tasks", taskArr);
        for (GameObject o : gameObjects.values()) {
            if (o instanceof Entity) {
                JSONObject entity = new JSONObject();
                entity.put("id", o.getID());
                entity.put("x", ((Entity) o).getPosition().x);
                entity.put("y", ((Entity) o).getPosition().y);
                entity.put("name", ((Entity) o).getAgentName());
                entity.put("team", ((Entity) o).getTeamName());
                entities.put(entity);
            }
            else if (o instanceof Block) {
                JSONObject block = new JSONObject();
                block.put("x", ((Block) o).getPosition().x);
                block.put("y", ((Block) o).getPosition().y);
                block.put("type", ((Block) o).getBlockType());
                blocks.put(block);
            }
            else if (o instanceof Dispenser) {
                JSONObject dispenser = new JSONObject();
                dispenser.put("id", o.getID());
                dispenser.put("x", ((Dispenser) o).getPosition().x);
                dispenser.put("y", ((Dispenser) o).getPosition().y);
                dispenser.put("type", ((Dispenser) o).getBlockType());
                dispensers.put(dispenser);
            }
        }
        tasks.values().stream().filter(t -> !t.isCompleted()).forEach(t -> {
            JSONObject task  = new JSONObject();
            task.put("name", t.getName());
            task.put("deadline", t.getDeadline());
            task.put("reward", t.getReward());
            JSONArray requirementsArr = new JSONArray();
            task.put("requirements", requirementsArr);
            t.getRequirements().forEach((pos, type) -> {
                JSONObject requirement = new JSONObject();
                requirement.put("x", pos.x);
                requirement.put("y", pos.y);
                requirement.put("type", type);
                requirementsArr.put(requirement);
            });
            taskArr.put(task);
        });
        return snapshot;
    }

    JSONObject getResult() {
        JSONObject result =  new JSONObject();
        teams.values().forEach(t -> {
            JSONObject teamResult = new JSONObject();
            teamResult.put("score", t.getScore());
            result.put(t.getName(), teamResult);
        });
        return result;
    }
}
