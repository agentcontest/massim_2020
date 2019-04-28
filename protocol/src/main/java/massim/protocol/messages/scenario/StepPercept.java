package massim.protocol.messages.scenario;

import massim.protocol.messages.RequestActionMessage;
import massim.protocol.messages.scenario.data.TaskInfo;
import massim.protocol.messages.scenario.data.Thing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class StepPercept extends RequestActionMessage {

    public Set<Thing> things = new HashSet<>();
    public Set<TaskInfo> taskInfo = new HashSet<>();
    public long score;

    // TODO last action

    public StepPercept(JSONObject content) {
        super(content);
        parsePercept(content.getJSONObject("percept"));
    }

    public StepPercept(long score, Set<Thing> things, Set<TaskInfo> taskInfo) {
        super(System.currentTimeMillis(), -1, -1); // id and deadline are updated later
        this.score = score;
        this.things.addAll(things);
        this.taskInfo.addAll(taskInfo);
    }

    @Override
    public JSONObject makePercept() {
        JSONObject percept = new JSONObject();
        JSONArray jsonThings = new JSONArray();
        JSONArray jsonTasks = new JSONArray();
        percept.put("score", score);
        percept.put("things", jsonThings);
        percept.put("tasks", jsonTasks);
        things.forEach(t -> jsonThings.put(t.toJSON()));
        taskInfo.forEach(t -> jsonTasks.put(t.toJSON()));
        return percept;
    }

    private void parsePercept(JSONObject percept) {
        score = percept.getLong("score");
        JSONArray jsonThings = percept.getJSONArray("things");
        JSONArray jsonTasks = percept.getJSONArray("tasks");
        for (int i = 0; i < jsonThings.length(); i++) {
            JSONObject jsonThing = jsonThings.getJSONObject(i);
            things.add(Thing.fromJson(jsonThing));
        }
        for (int i = 0; i < jsonTasks.length(); i++) {
            JSONObject jsonTask = jsonTasks.getJSONObject(i);
            taskInfo.add(TaskInfo.fromJson(jsonTask));
        }
    }
}
