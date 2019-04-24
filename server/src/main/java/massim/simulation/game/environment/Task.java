package massim.simulation.game.environment;

import massim.util.RNG;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Task {

    private String name;
    private Map<Position, String> requirements = new HashMap<>();

    private Task(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<Position, String> getRequirements() {
        return requirements;
    }

    private void addRequiredBlock(Position position, String blockType) {
        requirements.put(position, blockType);
    }

    public static Task generate(String name, int size, List<String> blockTypes) {
        Task task = new Task(name);
        // TODO improve task generation
        Position lastPosition = Position.of(0, 1);
        task.addRequiredBlock(lastPosition, blockTypes.get(RNG.nextInt(blockTypes.size())));
        for (int i = 0; i < size; i++) {
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
}
