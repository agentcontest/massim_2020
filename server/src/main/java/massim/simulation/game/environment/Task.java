package massim.simulation.game.environment;

import massim.util.RNG;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Task {

    private String name;
    private Map<Position, String> requirements = new HashMap<>();
    private int deadline;
    private boolean completed = false;

    private Task(String name, int deadline) {
        this.name = name;
        this.deadline = deadline;
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

    private void addRequiredBlock(Position position, String blockType) {
        requirements.put(position, blockType);
    }

    public static Task generate(String name, int deadline, int size, List<String> blockTypes) {
        Task task = new Task(name, deadline);
        // TODO improve task generation
        Position lastPosition = Position.of(0, 1);
        task.addRequiredBlock(lastPosition, blockTypes.get(RNG.nextInt(blockTypes.size())));
        for (int i = 0; i < size - 1; i++) {
            lastPosition = lastPosition.copy();
            int index = RNG.nextInt(blockTypes.size());
            double direction = RNG.nextDouble();
            if (direction <= .3) {
                lastPosition.x -= 1;
            }
            else if (direction <= .6) {
                lastPosition.x += 1;
            }
            else {
                lastPosition.y += 1;
            }
            task.addRequiredBlock(lastPosition, blockTypes.get(index));
        }
        return task;
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
}
