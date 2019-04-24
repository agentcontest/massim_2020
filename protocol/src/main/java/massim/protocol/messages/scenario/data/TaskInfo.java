package massim.protocol.messages.scenario.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class TaskInfo {
    public String name;
    public int deadline;
    public Set<Thing> requirements;

    public TaskInfo(String name, int deadline, Set<Thing> requirements) {
        this.name = name;
        this.deadline = deadline;
        this.requirements = requirements;
    }

    public JSONObject toJSON() {
        JSONObject task = new JSONObject();
        task.put("name", name);
        task.put("deadline", deadline);
        JSONArray jsonReqs = new JSONArray();
        for (Thing requirement : requirements) {
            jsonReqs.put(requirement.toJSON());
        }
        task.put("requirements", jsonReqs);
        return task;
    }

    public static TaskInfo fromJson(JSONObject jsonTask) {
        Set<Thing> requirements = new HashSet<>();
        JSONArray jsonRequirements = jsonTask.getJSONArray("requirements");
        for (int i = 0; i < jsonRequirements.length(); i++) {
            requirements.add(Thing.fromJson(jsonRequirements.getJSONObject(i)));
        }
        return new TaskInfo(jsonTask.getString("name"), jsonTask.getInt("deadline"), requirements);
    }
}
