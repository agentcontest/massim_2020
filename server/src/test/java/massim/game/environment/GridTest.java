package massim.game.environment;

import massim.config.TeamConfig;
import massim.game.environment.Block;
import massim.game.environment.Terrain;
import massim.protocol.data.Position;
import massim.protocol.data.Thing;
import massim.protocol.messages.scenario.Actions;
import massim.protocol.messages.scenario.StepPercept;
import massim.util.RNG;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.Assert.assertNotNull;

import java.util.*;

public class GridTest {
    private JSONObject gridjson;

    @org.junit.Before
    public void setUp() {
        RNG.initialize(17);
        
        this.gridjson = new JSONObject();
        this.gridjson.put("height", 70);
        this.gridjson.put("width", 70);
        this.gridjson.put("instructions", new JSONArray("[[\"cave\", 0.45, 9, 5, 4]]"));
        this.gridjson.put("goals", new JSONObject("{\"number\" : 3,\"size\" : [1,2]}"));
    }

    @org.junit.Test
    public void findRandomFreeClusterPosition() {
        this.gridjson.put("height", 5);
        this.gridjson.put("width", 5);
        System.out.println(this.gridjson.toString());
        Grid grid = new Grid(this.gridjson, 10, 8);

        printGridTerrain(grid);
        
        System.out.println("Testing cluster size 1");
        RNG.initialize(15);
        ArrayList<Position> cluster = grid.findRandomFreeClusterPosition(1);
        assertNotNull(cluster);
        assert(cluster.size()==1);

        assert(grid.getTerrain(cluster.get(0)) == Terrain.EMPTY);
        assert(cluster.get(0).toString().equals("(2,2)"));

        System.out.println("Testing cluster size 3");
        RNG.initialize(15);
        printGridTerrain(grid);
        ArrayList<Position> cluster3 = grid.findRandomFreeClusterPosition(3);
        assertNotNull(cluster3);
        assert(cluster3.size()==3);

        assert(grid.getTerrain(cluster3.get(0)) == Terrain.EMPTY);
        assert(cluster3.get(0).toString().equals("(3,0)"));
        assert(grid.getTerrain(cluster3.get(1)) == Terrain.EMPTY);
        assert(cluster3.get(1).toString().equals("(3,1)"));
        assert(grid.getTerrain(cluster3.get(2)) == Terrain.EMPTY);
        assert(cluster3.get(2).toString().equals("(4,0)"));
    }

    private void printGridTerrain(Grid grid){
        for (int x=0; x < grid.getDimX(); x++){
            System.out.println(" ");
            for (int y=0; y < grid.getDimY(); y++)
                System.out.print(" "+String.format("%1$5s",grid.getTerrain(new Position(x, y)).toString()));
        }
        System.out.println(" ");
    }
    private void printGridAgents(Grid grid){
        for (int x=0; x < grid.getDimX(); x++){
            System.out.println(" ");
            for (int y=0; y < grid.getDimY(); y++){
                System.out.print(" "+String.format("%1$5s",grid.getThings(new Position(x, y)).toString()));
            }
        }
        System.out.println(" ");
    }
}