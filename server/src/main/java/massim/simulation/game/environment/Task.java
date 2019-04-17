package massim.simulation.game.environment;

import java.util.ArrayList;
import java.util.List;

public class Task {

    private int width;
    private List<String[]> rows = new ArrayList<>();

    public Task(int width) {
        this.width = width;
    }

    public boolean addRow(String[] row) {
        if (row.length != width) return false;
        rows.add(row);
        return true;
    }

    public String[][] getBlocks() {
        return rows.toArray(new String[0][]);
    }
}
