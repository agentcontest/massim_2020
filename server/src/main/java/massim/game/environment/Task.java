package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.TaskInfo;
import massim.protocol.data.Thing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Task {

    private String name;
    private Map<Position, String> requirements;
    private int deadline;
    private boolean completed = false;

    public Task(String name, int deadline, Map<Position, String> requirements) {
        this.name = name;
        this.deadline = deadline;
        this.requirements = requirements;
    }

    public String getName() {
        return name;
    }

    public int getDeadline() {
        return deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void complete() {
        completed = true;
    }

    public Map<Position, String> getRequirements() {
        return requirements;
    }

    @Override
    public String toString() {
        return requirements.entrySet()
                .stream()
                .map(e -> "task(" + name + "," + e.getKey() + ","+e.getValue()+")")
                .collect(Collectors.joining(","));
    }

    public int getReward() {
        return (int) (10 * Math.pow(requirements.size(), 2));
    }

    public TaskInfo toPercept() {
        Set<Thing> reqs = new HashSet<>();
        requirements.forEach((pos, type) -> {
            reqs.add(new Thing(pos.x, pos.y, type, ""));
        });
        return new TaskInfo(name, deadline, getReward(), reqs);
    }
}
