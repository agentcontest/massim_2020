package massim.game;

import massim.config.TeamConfig;
import massim.game.environment.*;
import massim.protocol.data.Position;
import massim.protocol.data.Thing;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.SimEndMessage;
import massim.protocol.messages.SimStartMessage;
import massim.protocol.messages.scenario.Actions;
import massim.protocol.messages.scenario.InitialPercept;
import massim.protocol.messages.scenario.StepPercept;
import massim.util.Log;
import massim.util.RNG;
import massim.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * State of the game.
 */
class GameState {

    private Map<String, Team> teams = new HashMap<>();
    private Map<String, Entity> agentToEntity = new HashMap<>();
    private Map<Entity, String> entityToAgent = new HashMap<>();

    private int step = -1;
    private int teamSize;
    private Grid grid;
    private Map<Integer, GameObject> gameObjects = new HashMap<>();
    private Map<Position, Dispenser> dispensers = new HashMap<>();
    private Map<Position, TaskBoard> taskboards = new HashMap<>();
    private Map<String, Task> tasks = new HashMap<>();
    private Set<String> blockTypes = new TreeSet<>();
    private Set<ClearEvent> clearEvents = new HashSet<>();
    private Set<Position> agentCausedClearMarkers = new HashSet<>();

    // config parameters
    private int randomFail;
    private double pNewTask;
    private int taskDurationMin;
    private int taskDurationMax;
    private int taskSizeMin;
    private int taskSizeMax;
    private int taskRewardDecayMin;
    private int taskRewardDecayMax;
    int clearSteps;
    private int eventChance;
    private int eventRadiusMin;
    private int eventRadiusMax;
    private int eventWarning;
    private int eventCreateMin;
    private int eventCreateMax;
    private int eventCreatePerimeter;
    private int numberOfTaskboards;
    /** Minimum percentage of a reward to not decay beyond - range: [0,100] */
    private int lowerRewardLimit;

    private JSONArray logEvents = new JSONArray();

    GameState(JSONObject config, Set<TeamConfig> matchTeams) {
        // parse simulation config
        randomFail = config.getInt("randomFail");
        Log.log(Log.Level.NORMAL, "config.randomFail: " + randomFail);
        int attachLimit = config.getInt("attachLimit");
        Log.log(Log.Level.NORMAL, "config.attachLimit: " + attachLimit);
        clearSteps = config.getInt("clearSteps");
        Log.log(Log.Level.NORMAL, "config.clearSteps: " + clearSteps);
        var clusterSizes = config.getJSONArray("clusterBounds");
        int clusterSizeMin = clusterSizes.getInt(0);
        int clusterSizeMax = clusterSizes.getInt(1);
        Log.log(Log.Level.NORMAL, "config.clusterBounds: " + clusterSizeMin + ", " + clusterSizeMax);

        Entity.clearEnergyCost = config.getInt("clearEnergyCost");
        Log.log(Log.Level.NORMAL, "config.clearEnergyCost: " + Entity.clearEnergyCost);
        Entity.disableDuration = config.getInt("disableDuration");
        Log.log(Log.Level.NORMAL, "config.disableDuration: " + Entity.disableDuration);
        Entity.maxEnergy = config.getInt("maxEnergy");
        Log.log(Log.Level.NORMAL, "config.maxEnergy: " + Entity.maxEnergy);

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
        pNewTask = taskConfig.getDouble("probability");
        var taskRewardDecayBounds = taskConfig.getJSONArray("rewardDecay");
        Log.log(Log.Level.NORMAL, "config.tasks.rewardDecay: " + taskRewardDecayBounds);
        taskRewardDecayMin = taskRewardDecayBounds.getInt(0);
        taskRewardDecayMax = taskRewardDecayBounds.getInt(1);
        Log.log(Log.Level.NORMAL, "config.tasks.probability: " + pNewTask);
        numberOfTaskboards = taskConfig.getInt("taskboards");
        Log.log(Log.Level.NORMAL, "config.taskboards: " + numberOfTaskboards);
        int distanceToTaskboards = taskConfig.getInt("distanceToTaskboards");
        Log.log(Log.Level.NORMAL, "config.distanceToTaskboards: " + distanceToTaskboards);
        lowerRewardLimit = taskConfig.getInt("lowerRewardLimit");
        Log.log(Log.Level.NORMAL, "config.tasks.lowerRewardLimit: " + lowerRewardLimit);

        var eventConfig = config.getJSONObject("events");
        eventChance = eventConfig.getInt("chance");
        Log.log(Log.Level.NORMAL, "config.events.chance: " + eventChance);
        var eventRadius = eventConfig.getJSONArray("radius");
        eventRadiusMin = eventRadius.getInt(0);
        eventRadiusMax = eventRadius.getInt(1);
        Log.log(Log.Level.NORMAL, "config.events.radius: " + eventRadiusMin + " - " + eventRadiusMax);
        eventWarning = eventConfig.getInt("warning");
        Log.log(Log.Level.NORMAL, "config.events.warning: " + eventWarning);
        var eventCreate = eventConfig.getJSONArray("create");
        eventCreateMin = eventCreate.getInt(0);
        eventCreateMax = eventCreate.getInt(1);
        Log.log(Log.Level.NORMAL, "config.events.create: " + eventCreateMin + " - " + eventCreateMax);
        eventCreatePerimeter = eventConfig.getInt("perimeter");
        Log.log(Log.Level.NORMAL, "config.events.perimeter: " + eventCreatePerimeter);

        // set config values
        Task.setLowerRewardLimit(lowerRewardLimit);

        // create teams
        matchTeams.forEach(team -> teams.put(team.getName(), new Team(team.getName())));

        // create grid environment
        grid = new Grid(config.getJSONObject("grid"), attachLimit, distanceToTaskboards);

        // create entities
        var entities = config.getJSONObject("entities");
        var it = entities.keys();
        int agentCounter = 0;
        while (it.hasNext()) {
            var numberOfAgents = entities.getInt(it.next());
            List<Integer> agentsRange = IntStream.rangeClosed(0, numberOfAgents-1).boxed().collect(Collectors.toList());
            while (!agentsRange.isEmpty()) {
                int clusterSize = Math.min(RNG.betweenClosed(clusterSizeMin, clusterSizeMax), agentsRange.size());
                ArrayList<Position> cluster = grid.findRandomFreeClusterPosition(clusterSize);
                for (Position p : cluster) {
                    int index = agentsRange.remove(RNG.nextInt(agentsRange.size()));
                    for (TeamConfig team: matchTeams) {
                        createEntity(p, team.getAgentNames().get(index), team.getName());
                    }
                    agentCounter++;
                    if (agentCounter == numberOfAgents) break;
                }
            }
        }
        teamSize = agentCounter;

        // create env. things
        for (var block : blockTypes) {
            var numberOfDispensers = RNG.betweenClosed(dispenserBounds.getInt(0), dispenserBounds.getInt(1));
            for (var i = 0; i < numberOfDispensers; i++) {
                createDispenser(grid.findRandomFreePosition(), block);
            }
        }
        for (var i = 0; i < numberOfTaskboards; i++) {
            createTaskboard(grid.findNewTaskboardPosition());
        }

        // check for setup file
        var setupFilePath = config.optString("setup");
        if (!setupFilePath.equals("")){
            Log.log(Log.Level.NORMAL, "Running setup actions");
            try (var b = new BufferedReader(new FileReader(setupFilePath));){
                var line = "";
                while ((line = b.readLine()) != null) {
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    if (line.startsWith("stop")) break;
                    handleCommand(line.split(" "));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Map<String, Team> getTeams() {
        return this.teams;
    }

    public Set<String> getBlockTypes() {
        return this.blockTypes;
    }

    private void handleCommand(String[] command) {
        if (command.length == 0) return;

        switch (command[0]) {
            case "move":
                if (command.length != 4) break;
                var x = Util.tryParseInt(command[1]);
                var y = Util.tryParseInt(command[2]);
                var entity = agentToEntity.get(command[3]);

                if (entity == null || x == null || y == null) break;
                Log.log(Log.Level.NORMAL, "Setup: Try to move " + command[3] + " to (" + x +", " + y + ")");
                grid.moveWithoutAttachments(entity, Position.of(x, y));
                break;

            case "add":
                if (command.length < 4 || command.length > 5) break;
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
                    case "taskboard":
                        createTaskboard(Position.of(x, y));
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
                Attachable a1 = getUniqueAttachable(Position.of(x1, y1));
                Attachable a2 = getUniqueAttachable(Position.of(x2, y2));
                if (a1 == null || a2 == null) break;
                grid.attach(a1, a2);
                break;

            case "terrain":
                if (command.length != 4) break;
                x = Util.tryParseInt(command[1]);
                y = Util.tryParseInt(command[2]);
                var type = command[3];
                if (x == null || y == null || type.isEmpty()) break;
                if (type.equalsIgnoreCase("obstacle")) setTerrain(Position.of(x, y), Terrain.OBSTACLE);
                else if (type.equalsIgnoreCase("goal")) setTerrain(Position.of(x, y), Terrain.GOAL);
                else if (type.equalsIgnoreCase("empty")) setTerrain(Position.of(x, y), Terrain.EMPTY);
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

    Entity getEntityByName(String agentName) {
        return agentToEntity.get(agentName);
    }

    Map<String, SimStartMessage> getInitialPercepts(int steps) {
        Map<String, SimStartMessage> result = new HashMap<>();
        for (Entity e: entityToAgent.keySet()) {
            result.put(e.getAgentName(), new InitialPercept(e.getAgentName(), e.getTeamName(), teamSize, steps, e.getVision()));
        }
        return result;
    }

    Map<String, RequestActionMessage> prepareStep(int step) {
        this.step = step;

        logEvents = new JSONArray();

        //cleanup & transfer markers
        grid.deleteMarkers();
        for (Position pos : agentCausedClearMarkers) grid.createMarker(pos, Marker.Type.CLEAR);
        agentCausedClearMarkers.clear();

        //handle tasks
        if (RNG.nextDouble() < pNewTask) {
            createTask(RNG.betweenClosed(taskDurationMin, taskDurationMax), RNG.betweenClosed(taskSizeMin, taskSizeMax));
        }

        //handle entities
        agentToEntity.values().forEach(Entity::preStep);

        //handle activated tasks
        tasks.values().forEach(Task::preStep);

        //handle (map) events
        if (RNG.nextInt(100) < eventChance) {
            clearEvents.add(new ClearEvent(grid.getRandomPosition(), step + eventWarning,
                    RNG.betweenClosed(eventRadiusMin, eventRadiusMax)));
        }
        var processedEvents = new HashSet<ClearEvent>();
        for (ClearEvent event: clearEvents) {
            if (event.getStep() == step) {
                processEvent(event);
                processedEvents.add(event);
            }
            else {
                var type = event.getStep() - step <= 2? Marker.Type.CLEAR_IMMEDIATE : Marker.Type.CLEAR;
                var clearArea = event.getPosition().spanArea(event.getRadius());
                var clearPerimeter = event.getPosition().spanArea(event.getRadius() + eventCreatePerimeter);
                clearPerimeter.removeAll(clearArea);
                for (Position pos: clearArea) grid.createMarker(pos, type);
                for (Position pos: clearPerimeter) grid.createMarker(pos, Marker.Type.CLEAR_PERIMETER);
            }
        }
        clearEvents.removeAll(processedEvents);

        return getStepPercepts();
    }

    private void processEvent(ClearEvent event) {
        var removed = clearArea(event.getPosition(), event.getRadius());
        var distributeNew = RNG.betweenClosed(eventCreateMin, eventCreateMax) + removed;

        for (var i = 0; i < distributeNew; i++) {
            var pos = grid.findRandomFreePosition(event.getPosition(),eventCreatePerimeter + event.getRadius());
            if(pos != null && grid.getTerrain(pos) == Terrain.EMPTY
                    && dispensers.get(pos) == null && !grid.outOfBounds(pos)) {
                grid.setTerrain(pos, Terrain.OBSTACLE);
            }
        }
    }

    Map<String, RequestActionMessage> getStepPercepts(){
        Map<String, RequestActionMessage> result = new HashMap<>();
        var allTasks = tasks.values().stream()
                .filter(t -> !t.isCompleted())
                .map(Task::toPercept)
                .collect(Collectors.toSet());
        for (Entity entity : entityToAgent.keySet()) {
            var pos = entity.getPosition();
            var visibleThings = new HashSet<Thing>();
            Map<String, Set<Position>> visibleTerrain = new HashMap<>();
            Set<Position> attachedThings = new HashSet<>();
            for (Position currentPos: pos.spanArea(entity.getVision())){
                getThingsAt(currentPos).forEach(go -> {
                    visibleThings.add(go.toPercept(pos));
                    if (go != entity && go instanceof Attachable && ((Attachable)go).isAttachedToAnotherEntity()){
                        attachedThings.add(go.getPosition().relativeTo(pos));
                    }
                });
                var d = dispensers.get(currentPos);
                if (d != null) visibleThings.add(d.toPercept(pos));
                var tb = taskboards.get(currentPos);
                if (tb != null) visibleThings.add(tb.toPercept(pos));
                var terrain = grid.getTerrain(currentPos);
                if (terrain != Terrain.EMPTY) {
                    visibleTerrain.computeIfAbsent(terrain.name,
                            t -> new HashSet<>()).add(currentPos.relativeTo(pos));
                }
            }
            var percept = new StepPercept(step, teams.get(entity.getTeamName()).getScore(),
                    visibleThings, visibleTerrain, allTasks, entity.getLastAction(), entity.getLastActionParams(),
                    entity.getLastActionResult(), attachedThings, entity.getTask());
            percept.energy = entity.getEnergy();
            percept.disabled = entity.isDisabled();
            result.put(entity.getAgentName(), percept);
        }
        return result;
    }

    Map<String, SimEndMessage> getFinalPercepts() {
        var result = new HashMap<String, SimEndMessage>();
        var teamsSorted = new ArrayList<>(teams.values());
        teamsSorted.sort((t1, t2) -> (int) (t2.getScore() - t1.getScore()));
        var rankings = new HashMap<Team, Integer>();
        for (int i = 0; i < teamsSorted.size(); i++) {
            rankings.put(teamsSorted.get(i), i + 1);
        }
        for (Entity e: entityToAgent.keySet()) {
            var team = teams.get(e.getTeamName());
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
        Attachable a = getUniqueAttachable(target);
        if (a == null) return Actions.RESULT_F_TARGET;
        if (a instanceof Entity && ofDifferentTeams(entity, (Entity) a)) {
            return Actions.RESULT_F_TARGET;
        }
        if(!attachedToOpponent(a, entity) && grid.attach(entity, a)) {
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleDetachAction(Entity entity, String direction) {
        Position target = entity.getPosition().moved(direction, 1);
        Attachable a = getUniqueAttachable(target);
        if (a == null) return Actions.RESULT_F_TARGET;
        if (a instanceof Entity && ofDifferentTeams(entity, (Entity) a)) {
            return Actions.RESULT_F_TARGET;
        }
        if (grid.detachNeighbors(entity, a)){
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleDisconnectAction(Entity entity, Position attPos1, Position attPos2) {
        var attachable1 = getUniqueAttachable(attPos1.translate(entity.getPosition()));
        var attachable2 = getUniqueAttachable(attPos2.translate(entity.getPosition()));
        if (attachable1 == null || attachable2 == null) return Actions.RESULT_F_TARGET;
        var allAttachments = entity.collectAllAttachments();
        if (!allAttachments.contains(attachable1) || !allAttachments.contains(attachable2))
            return Actions.RESULT_F_TARGET;
        if (grid.detachNeighbors(attachable1, attachable2)) return Actions.RESULT_SUCCESS;
        return Actions.RESULT_F_TARGET;
    }

    String handleConnectAction(Entity entity, Position blockPos, Entity partnerEntity, Position partnerBlockPos) {
        Attachable block1 = getUniqueAttachable(blockPos.translate(entity.getPosition()));
        Attachable block2 = getUniqueAttachable(partnerBlockPos.translate(partnerEntity.getPosition()));

        if(!(block1 instanceof Block) || !(block2 instanceof Block)) return Actions.RESULT_F_TARGET;

        Set<Attachable> attachables = entity.collectAllAttachments();
        if (attachables.contains(partnerEntity)) return Actions.RESULT_F;
        if (!attachables.contains(block1)) return Actions.RESULT_F_TARGET;
        if (attachables.contains(block2)) return Actions.RESULT_F_TARGET;

        Set<Attachable> partnerAttachables = partnerEntity.collectAllAttachments();
        if (!partnerAttachables.contains(block2)) return Actions.RESULT_F_TARGET;
        if (partnerAttachables.contains(block1)) return Actions.RESULT_F_TARGET;

        if(grid.attach(block1, block2)){
            return Actions.RESULT_SUCCESS;
        }
        return Actions.RESULT_F;
    }

    String handleRequestAction(Entity entity, String direction) {
        var requestPosition = entity.getPosition().moved(direction, 1);
        var dispenser = dispensers.get(requestPosition);
        if (dispenser == null) return Actions.RESULT_F_TARGET;
        if (!grid.isUnblocked(requestPosition)) return Actions.RESULT_F_BLOCKED;
        createBlock(requestPosition, dispenser.getBlockType());
        return Actions.RESULT_SUCCESS;
    }

    String handleSubmitAction(Entity e, String taskName) {
        Task task = tasks.get(taskName);
        if (task == null || task.isCompleted() || step > task.getDeadline() || !e.getTask().equals(taskName))
            return Actions.RESULT_F_TARGET;
        Position ePos = e.getPosition();
        if (grid.getTerrain(ePos) != Terrain.GOAL) return Actions.RESULT_F;
        Set<Attachable> attachedBlocks = e.collectAllAttachments();
        for (Map.Entry<Position, String> entry : task.getRequirements().entrySet()) {
            var pos = entry.getKey();
            var reqType = entry.getValue();
            var checkPos = Position.wrapped(pos.x + ePos.x, pos.y + ePos.y);
            var actualBlock = getUniqueAttachable(checkPos);
            if (actualBlock instanceof Block
                && ((Block) actualBlock).getBlockType().equals(reqType)
                && attachedBlocks.contains(actualBlock)) {
                continue;
            }
            return Actions.RESULT_F;
        }
        task.getRequirements().keySet().forEach(pos -> {
            Attachable a = getUniqueAttachable(pos.translate(e.getPosition()));
            removeObjectFromGame(a);
        });
        teams.get(e.getTeamName()).addScore(task.getReward());
        task.complete();

        var result = new JSONObject();
        result.put("type", "task completed");
        result.put("task", task.getName());
        result.put("team", e.getTeamName());
        if (logEvents != null) logEvents.put(result);

        return Actions.RESULT_SUCCESS;
    }

    /**
     * @param entity entity executing the action
     * @param xy target position in entity local system
     * @return action result
     */
    String handleClearAction(Entity entity, Position xy) {
        var target = xy.translate(entity.getPosition());
        if (target.distanceTo(entity.getPosition()) > entity.getVision()) return Actions.RESULT_F_TARGET;
        if (entity.getEnergy() < Entity.clearEnergyCost) return Actions.RESULT_F_RESOURCES;

        var previousPos = entity.getPreviousClearPosition();
        if(entity.getPreviousClearStep() != step - 1 || previousPos.x != target.x || previousPos.y != target.y) {
            entity.resetClearCounter();
        }
        var counter = entity.incrementClearCounter();
        if (counter == clearSteps) {
            clearArea(target, 1);
            entity.consumeClearEnergy();
            entity.resetClearCounter();
        }
        else {
            agentCausedClearMarkers.addAll(target.spanArea(1));
        }
        entity.recordClearAction(step, target);
        return Actions.RESULT_SUCCESS;
    }

    public String handleAcceptAction(Entity entity, String taskName) {
        if (taskName == null || taskName.equals("")) return Actions.RESULT_F_TARGET;
        var task = tasks.get(taskName);
        if (task == null) return Actions.RESULT_F_TARGET;

        var nearTaskboard = false;
        var pos = entity.getPosition();
        for (var tb: taskboards.values()) {
            if (tb.getPosition().distanceTo(pos) <= 2) {
                nearTaskboard = true;
                break;
            }
        }
        if (!nearTaskboard) return Actions.RESULT_F_LOCATION;

        entity.acceptTask(task);
        return Actions.RESULT_SUCCESS;
    }

    int clearArea(Position center, int radius) {
        var removed = 0;
        for (var position : center.spanArea(radius)) {
            for (var go : getThingsAt(position)) {
                if (go instanceof Entity) {
                    ((Entity)go).disable();
                }
                else if (go instanceof Block) {
                    removed++;
                    grid.destroyThing(go);
                    gameObjects.remove(go.getID());
                }
            }
            if (grid.getTerrain(position) == Terrain.OBSTACLE) {
                removed++;
                grid.setTerrain(position, Terrain.EMPTY);
            }
        }
        return removed;
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
                lastPosition = Position.of(lastPosition.x - 1, lastPosition.y);
            }
            else if (direction <= .6) {
                lastPosition = Position.of(lastPosition.x + 1, lastPosition.y);
            }
            else {
                lastPosition = Position.of(lastPosition.x, lastPosition.y + 1);
            }
            requirements.put(lastPosition, blockList.get(index));
        }
        Task t = new Task(name, step + duration, requirements, RNG.betweenClosed(this.taskRewardDecayMin, this.taskRewardDecayMax));
        tasks.put(t.getName(), t);
        return t;
    }

    Task createTask(String name, int duration, Map<Position, String> requirements) {
        if (requirements.size() == 0) return null;
        Task t = new Task(name, step + duration, requirements, RNG.betweenClosed(this.taskRewardDecayMin, this.taskRewardDecayMax));
        tasks.put(t.getName(), t);
        return t;
    }

    private void removeObjectFromGame(GameObject go){
        if (go == null) return;
        if (go instanceof Positionable) grid.destroyThing((Positionable) go);
        gameObjects.remove(go.getID());
    }

    private Entity createEntity(Position xy, String name, String teamName) {
        Entity e = grid.createEntity(xy, name, teamName);
        registerGameObject(e);
        agentToEntity.put(name, e);
        entityToAgent.put(e, name);
        return e;
    }

    Block createBlock(Position xy, String blockType) {
        if (!blockTypes.contains(blockType)) return null;
        Block b = grid.createBlock(xy, blockType);
        registerGameObject(b);
        return b;
    }

    boolean createDispenser(Position xy, String blockType) {
        if (!blockTypes.contains(blockType)) return false;
        if (!grid.isUnblocked(xy)) return false;
        if (dispensers.get(xy) != null) return false;
        Dispenser d = new Dispenser(xy, blockType);
        registerGameObject(d);
        dispensers.put(xy, d);
        Log.log(Log.Level.NORMAL, "Created " + d);
        return true;
    }

    boolean createTaskboard(Position xy) {
        if (!grid.isUnblocked(xy)) return false;
        if (taskboards.get(xy) != null) return false;
        TaskBoard tb = new TaskBoard(xy);
        registerGameObject(tb);
        taskboards.put(xy, tb);
        Log.log(Log.Level.NORMAL, "Created " + tb);
        return true;
    }

    private void registerGameObject(GameObject o) {
        if (o == null) return;
        this.gameObjects.put(o.getID(), o);
    }

    private Attachable getUniqueAttachable(Position pos) {
        var attachables = getAttachables(pos);
        if (attachables.size() != 1) return null;
        return attachables.iterator().next();
    }

    private Set<Attachable> getAttachables(Position position) {
        return getThingsAt(position).stream()
                .filter(go -> go instanceof Attachable)
                .map(go -> (Attachable)go)
                .collect(Collectors.toSet());
    }

    Set<Positionable> getThingsAt(Position pos) {
        return grid.getThings(pos);
    }

    private boolean attachedToOpponent(Attachable a, Entity entity) {
        return a.collectAllAttachments().stream().anyMatch(other -> other instanceof Entity && ofDifferentTeams((Entity) other, entity));
    }

    private boolean ofDifferentTeams(Entity e1, Entity e2) {
        return !e1.getTeamName().equals(e2.getTeamName());
    }

    JSONObject takeStatusSnapshot() {
        JSONObject snapshot = new JSONObject();
        snapshot.put("step", step);
        JSONArray entityArr = new JSONArray();
        snapshot.put("entities", entityArr);
        for (Entity o : agentToEntity.values()) {
            JSONObject obj = new JSONObject();
            obj.put("name", o.getAgentName());
            obj.put("team", o.getTeamName());
            obj.put("action", Actions.ALL_ACTIONS.contains(o.getLastAction()) ? "HIDDEN" : o.getLastAction());
            obj.put("actionResult", o.getLastActionResult());
            entityArr.put(obj);
        }
        return snapshot;
    }

    JSONObject takeSnapshot() {
        JSONObject snapshot = new JSONObject();
        snapshot.put("step", step);
        JSONArray entities = new JSONArray();
        snapshot.put("entities", entities);
        JSONArray blocks = new JSONArray();
        snapshot.put("blocks", blocks);
        JSONArray dispensers = new JSONArray();
        snapshot.put("dispensers", dispensers);
        JSONArray taskboardsArr = new JSONArray();
        snapshot.put("taskboards", taskboardsArr);
        JSONArray taskArr = new JSONArray();
        snapshot.put("tasks", taskArr);
        JSONArray cells = new JSONArray();
        snapshot.put("cells", cells);
        JSONArray clear = new JSONArray();
        snapshot.put("clear", clear);
        JSONObject scores = new JSONObject();
        snapshot.put("scores", scores);
        for (int y = 0; y < grid.getDimY(); y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < grid.getDimX(); x++) {
                row.put(grid.getTerrain(Position.of(x, y)).id);
            }
            cells.put(row);
        }
        for (GameObject o : gameObjects.values()) {
            JSONObject obj = new JSONObject();
            if (o instanceof Positionable) {
                obj.put("x", ((Positionable) o).getPosition().x);
                obj.put("y", ((Positionable) o).getPosition().y);
            }
            if (o instanceof Attachable) {
                JSONArray arr = new JSONArray();
                ((Attachable) o).collectAllAttachments().stream().filter(a -> a != o).forEach(a -> {
                    JSONObject pos = new JSONObject();
                    pos.put("x", a.getPosition().x);
                    pos.put("y", a.getPosition().y);
                    arr.put(pos);
                });
                if (!arr.isEmpty()) obj.put("attached", arr);
            }
            if (o instanceof Entity) {
                obj.put("id", o.getID());
                obj.put("name", ((Entity) o).getAgentName());
                obj.put("team", ((Entity) o).getTeamName());
                obj.put("energy", ((Entity) o).getEnergy());
                obj.put("vision", ((Entity) o).getVision());
                obj.put("action", ((Entity) o).getLastAction());
                obj.put("actionParams", ((Entity) o).getLastActionParams());
                obj.put("actionResult", ((Entity) o).getLastActionResult());
                obj.put("acceptedTask", ((Entity) o).getTask());
                if (((Entity) o).isDisabled()) obj.put("disabled", true);
                entities.put(obj);
            } else if (o instanceof Block) {
                obj.put("type", ((Block) o).getBlockType());
                blocks.put(obj);
            } else if (o instanceof Dispenser) {
                obj.put("id", o.getID());
                obj.put("type", ((Dispenser) o).getBlockType());
                dispensers.put(obj);
            } else if (o instanceof TaskBoard) {
                taskboardsArr.put(obj);
            }
        }
        for (ClearEvent e : clearEvents) {
            JSONObject event = new JSONObject();
            event.put("x", e.getPosition().x);
            event.put("y", e.getPosition().y);
            event.put("radius", e.getRadius());
            clear.put(event);
        }
        tasks.values().stream().filter(t -> !t.isCompleted() && step <= t.getDeadline()).sorted(Comparator.comparing(t -> t.getDeadline())).forEach(t -> {
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
        teams.values().forEach(t -> scores.put(t.getName(), t.getScore()));
        snapshot.put("events", logEvents);
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

    boolean teleport(String entityName, Position targetPos) {
        Entity entity = getEntityByName(entityName);
        if (entity == null || targetPos == null) return false;
        if (grid.isUnblocked(targetPos)) {
            grid.moveWithoutAttachments(entity, targetPos);
            return true;
        }
        return false;
    }

    void setTerrain(Position p, Terrain terrain) {
        grid.setTerrain(p, terrain);
    }

    boolean attach(Position p1, Position p2) {
        Attachable a1 = getUniqueAttachable(p1);
        Attachable a2 = getUniqueAttachable(p2);
        if (a1 == null || a2 == null) return false;
        return grid.attach(a1, a2);
    }

    Terrain getTerrain(Position pos) {
        return grid.getTerrain(pos);
    }
}
