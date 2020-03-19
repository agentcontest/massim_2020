package massim.game.environment;

import massim.protocol.data.Position;
import massim.protocol.data.TaskInfo;
import massim.protocol.data.Thing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Task {

    private static int lowerRewardLimit;

    private String name;
    private Map<Position, String> requirements;
    private int deadline;
    private boolean completed = false;
    private int reward;
    private int rewardDecay;
    private int minimumReward;

    public Task(String name, int deadline, Map<Position, String> requirements, int rewardDecay) {
        this.name = name;
        this.deadline = deadline;
        this.requirements = requirements;
        this.reward = (int) (10 * Math.pow(requirements.size(), 2));
        this.rewardDecay = rewardDecay;
        this.minimumReward = (int) Math.ceil(reward/100. * lowerRewardLimit);
    }

    public static void setLowerRewardLimit(int newLimit) {
        lowerRewardLimit = newLimit;
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

    /**
     * decrease reward
     */
    public void preStep() {
        this.reward = (this.reward*(100-this.rewardDecay)) / 100;
        if (this.reward <= minimumReward) reward = minimumReward;
    }

    public int getReward() {
        return this.reward;
    }

    public TaskInfo toPercept() {
        Set<Thing> reqs = new HashSet<>();
        requirements.forEach((pos, type) -> {
            reqs.add(new Thing(pos.x, pos.y, type, ""));
        });
        return new TaskInfo(name, deadline, getReward(), reqs);
    }
}
