package massim.protocol.messages.scenario.data;

import org.json.JSONObject;

public class Thing {

    public static final String TYPE_ENTITY = "entity";
    public static final String TYPE_BLOCK = "block";
    public static final String TYPE_DISPENSER = "dispenser";

    public int x;
    public int y;
    public String type;
    public String details;

    public Thing(int x, int y, String type, String details) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.details = details;
    }

    public JSONObject toJSON() {
        JSONObject thing = new JSONObject();
        thing.put("x", x);
        thing.put("y", y);
        thing.put("type", type);
        thing.put("details", details);
        return thing;
    }

    public static Thing fromJson(JSONObject jsonThing) {
        return new Thing(jsonThing.getInt("x"), jsonThing.getInt("y"), jsonThing.getString("type"), jsonThing.getString("details"));
    }
}
