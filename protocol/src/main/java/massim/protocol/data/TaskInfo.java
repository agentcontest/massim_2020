package massim.protocol.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class TaskInfo {
    public String name;
    public int deadline;
    public int reward;
    public List<Thing> requirements;

    public TaskInfo(String name, int deadline, int reward, Set<Thing> requirements) {
        this.name = name;
        this.deadline = deadline;
        this.requirements = new ArrayList<>(requirements);
        this.requirements.sort((t1, t2) -> {
            var r = Integer.compare(t1.x, t2.x);
            if (r == 0) return Integer.compare(t1.y, t2.y);
            return r;
        });
        this.reward = reward;
    }

    public JSONObject toJSON() {
        JSONObject task = new JSONObject();
        task.put("name", name);
        task.put("deadline", deadline);
        task.put("reward", reward);
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
        return new TaskInfo(jsonTask.getString("name"), jsonTask.getInt("deadline"),
                jsonTask.getInt("reward"), requirements);
    }
}
