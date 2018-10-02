package massim.protocol.messages;

import org.json.JSONObject;

public class SimEndMessage extends Message {

    private long time;
    private long score;
    private int ranking;

    public SimEndMessage(JSONObject content) {
        this.time = content.optLong("time");
        this.score = content.optLong("score", -1);
        this.ranking = content.optInt("ranking", -1);
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_SIM_END;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject json = new JSONObject();
        json.append("score", score);
        json.append("ranking", ranking);
        return json;
    }

    public long getTime() {
        return time;
    }

    public long getScore() {
        return score;
    }

    public int getRanking() {
        return ranking;
    }
}
