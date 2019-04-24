package massim.simulation.game;

import massim.config.TeamConfig;
import massim.simulation.game.environment.*;
import massim.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * State of the game.
 */
class GameState {

    private int randomFail;
    private int attachLimit;
    private Map<String, Team> teams = new HashMap<>();
    private Map<String, String> agentToTeam = new HashMap<>();
    private Map<String, Entity> agentToEntity = new HashMap<>();
    private Map<Entity, String> entityToAgent = new HashMap<>();
    private List<String> agentNames;

    private int step = -1;
    private Grid grid;
    private Map<String, GameObject> gameObjects = new HashMap<>();
    private Map<Position, Dispenser> dispensers = new HashMap<>();
    private Map<String, Task> tasks = new HashMap<>();
    private List<String> blockTypes = new ArrayList<>();

    GameState(JSONObject config, Set<TeamConfig> matchTeams) {
        // parse simulation config
        randomFail = config.getInt("randomFail");
        Log.log(Log.Level.NORMAL, "config.randomFail: " + randomFail);
        attachLimit = config.getInt("attachLimit");
        Log.log(Log.Level.NORMAL, "config.attachLimit: " + attachLimit);
        int numberOfBlockTypes = config.optInt("blockTypes");
        Log.log(Log.Level.NORMAL, "config.blockTypes: " + numberOfBlockTypes);
        for (int i = 0; i < numberOfBlockTypes; i++) {
            blockTypes.add("b" + i);
        }

        // create teams
        matchTeams.forEach(team -> {
            team.getAgentNames().forEach(agName -> agentToTeam.put(agName, team.getName()));
            teams.put(team.getName(), new Team(team.getName()));
        });
        agentNames = new ArrayList<>(agentToTeam.keySet());

        // create grid environment
        JSONObject gridConf = config.getJSONObject("grid");
        int gridX = gridConf.getInt("width");
        int gridY = gridConf.getInt("height");
        grid = new Grid(gridX, gridY, attachLimit);

        // create entities
        JSONArray entities = config.optJSONArray("entities");
        if(entities != null){
            for (int i = 0; i < entities.length(); i++) {
                JSONObject entityConf = entities.optJSONObject(i);
                if (entityConf != null){
                    String roleName = entityConf.keys().next();
                    int amount = entityConf.optInt(roleName, 0);
                    for (int j = 0; j < amount; j++){
                        Position position = grid.findRandomFreePosition(); // entities from the same team start on the same spot
                        for (TeamConfig team: matchTeams) {
                            String agentName;
                            if(team.getAgentNames().size() > i) {
                                agentName = team.getAgentNames().get(i);
                            }
                            else {
                                agentName = team.getName() + "-unconfigured-" + i;
                                Log.log(Log.Level.ERROR, "Too few agents configured for team " + team.getName()
                                        + ", using agent name " + agentName + ".");
                            }

                            Entity entity = grid.createEntity(position, agentName);
                            gameObjects.put(entity.getID(), entity);
                            agentToEntity.put(agentName, entity);
                            entityToAgent.put(entity, agentName);
                        }
                    }
                }
            }
        }

        // create goal area
        grid.setTerrain(Position.of(gridX/2, gridY/2), Terrain.GOAL);
    }

    int getRandomFail() {
        return this.randomFail;
    }

    /**
     * @return the agent entity of the given name or null if it does not exist
     */
    Entity getEntityByID(String agentName) {
        GameObject entity = gameObjects.get(agentName);
        if (!(entity instanceof Entity)) return null;
        return (Entity) entity;
    }

    Entity getEntityByName(String agentName) {
        return agentToEntity.get(agentName);
    }

    void prepare(int step) {
        this.step = step;
    }

    boolean move(Entity entity, String direction) {
        return grid.moveWithAttached(entity, direction, 1);
    }

    boolean rotate(Entity entity, boolean clockwise) {
        return grid.rotateWithAttached(entity, clockwise);
    }

    boolean attach(Entity entity, String direction) {
        Position target = entity.getPosition().moved(direction, 1);
        if (target == null) return false;
        String collidableID = grid.getCollidable(target);
        GameObject gameObject = gameObjects.get(collidableID);
        if (!(gameObject instanceof Attachable)) return false;
        Attachable a = (Attachable) gameObject;
        return !attachedToOpponent(a, entity) && grid.attach(entity, a);
    }

    boolean detach(Entity entity, String direction) {
        Position target = entity.getPosition().moved(direction, 1);
        Attachable a = getAttachable(target);
        if (a == null) return false;
        return grid.detach(entity, a);
    }

    boolean connectEntities(Entity entity, Entity partnerEntity) {
        Position ePos = entity.getPosition();
        Position partnerPos = partnerEntity.getPosition();
        if (ePos.distanceTo(partnerPos) < 3) return false;

        String direction = ePos.directionTo(partnerPos);
        if (direction == null) return false;

        Set<Attachable> attachables = grid.getAllAttached(entity);
        if (attachables.contains(partnerEntity)) return false;
        Set<Attachable> partnerAttachables = grid.getAllAttached(partnerEntity);

        Position p = ePos;
        Block block = null;
        Block partnerBlock = null;
        while (true) {
            p = p.moved(direction, 1);
            Attachable attachable = getAttachable(p);
            if (attachable instanceof Block && attachables.contains(attachable)) {
                block = (Block) attachable;
            }
            else {
                break;
            }
        }
        direction = Position.oppositeDirection(direction);
        p = partnerPos;
        while (true) {
            p = p.moved(direction, 1);
            Attachable attachable = getAttachable(p);
            if (attachable instanceof Block && partnerAttachables.contains(attachable)) {
                partnerBlock = (Block) attachable;
            }
            else {
                break;
            }
        }
        return grid.attach(block, partnerBlock);
    }

    boolean requestBlock(Entity entity, String direction) {
        Position requestPosition = entity.getPosition().moved(direction, 1);
        Dispenser dispenser = dispensers.get(requestPosition);
        System.out.println(dispenser + " sfs");
        if (dispenser != null && grid.isFree(requestPosition)){
            createBlock(requestPosition, dispenser.getBlockType());
            return true;
        }
        return false;
    }

    boolean submitTask(Entity e, String taskName) {
        Task task = tasks.get(taskName);
        if (task == null || task.isCompleted()) return false;
        Position ePos = e.getPosition();
        Set<Attachable> availableBlocks = grid.getAllAttached(e);
        for (Map.Entry<Position, String> entry : task.getRequirements().entrySet()) {
            Position pos = entry.getKey();
            String reqType = entry.getValue();
            Position checkPos = Position.of(pos.x + ePos.x, pos.y + ePos.y);
            Attachable actualBlock = getAttachable(checkPos);
            if (!(actualBlock instanceof Block)
                    || !(((Block) actualBlock).getBlockType().equals(reqType))
                    || !availableBlocks.contains(actualBlock)) {
                return false;
            }
        }
        task.getRequirements().keySet().forEach(pos -> {
            Attachable a = getAttachable(pos);
            if (a != null) {
                grid.removeAttachable(a);
                gameObjects.remove(a.getID());
            }
        });
        teams.get(agentToTeam.get(e.getAgentName())).addScore(task.getReward());
        return true;
    }

    Task createTask() {
        Task t = Task.generate("task" + tasks.values().size(), step + 200, 5, blockTypes);
        tasks.put(t.getName(), t);
        return t;
    }

    Entity createEntity(Position xy, String name) {
        Entity e = grid.createEntity(xy, name);
        registerGameObject(e);
        return e;
    }

    void createBlock(Position xy, String blockType) {
        Block b = grid.createBlock(xy, blockType);
        registerGameObject(b);
    }

    void createDispenser(Position xy, String blockType) {
        Dispenser d = new Dispenser(xy, blockType);
        registerGameObject(d);
        dispensers.put(xy, d);
    }

    private void registerGameObject(GameObject o) {
        this.gameObjects.put(o.getID(), o);
    }

    private Attachable getAttachable(Position position) {
        GameObject go = gameObjects.get(grid.getCollidable(position));
        return (go instanceof Attachable)? (Attachable) go : null;
    }

    private boolean attachedToOpponent(Attachable a, Entity entity) {
        return grid.getAllAttached(a).stream().anyMatch(other -> other instanceof Entity && ofDifferentTeams((Entity) other, entity));
    }

    private boolean ofDifferentTeams(Entity e1, Entity e2) {
        return agentToTeam.get(e1.getAgentName()).equals(agentToTeam.get(e2.getAgentName()));
    }

    public JSONObject takeSnapshot() {
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
                entity.put("team", agentToTeam.get(((Entity) o).getAgentName()));
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
}