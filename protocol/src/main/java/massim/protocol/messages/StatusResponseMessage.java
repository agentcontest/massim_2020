package massim.protocol.messages;

import org.json.JSONArray;
import org.json.JSONObject;

public class StatusResponseMessage extends Message {

    public final long time;
    public final String[] teams;
    public final Integer[] teamSizes;
    public final int currentSimulation;

    public StatusResponseMessage(JSONObject content) {
        this.time = content.optLong("time");

        var jTeams = content.optJSONArray("teams");
        teams = new String[jTeams.length()];
        for (int i = 0; i < jTeams.length(); i++) {
            teams[i] = jTeams.getString(i);
        }

        var jTeamSizes = content.optJSONArray("teamSizes");
        teamSizes = new Integer[jTeamSizes.length()];
        for (int i = 0; i < jTeamSizes.length(); i++) {
            teamSizes[i] = jTeamSizes.getInt(i);
        }

        this.currentSimulation = content.optInt("currentSimulation");
    }

    public StatusResponseMessage(long time, String[] teams, Integer[] teamSizes, int currentSimulation) {
        this.time = time;
        this.teams = teams;
        this.teamSizes = teamSizes;
        this.currentSimulation = currentSimulation;
    }

    @Override
    public String getMessageType() {
        return Message.TYPE_STATUS_RESPONSE;
    }

    @Override
    public JSONObject makeContent() {
        JSONObject content = new JSONObject();

        content.put("time", time);

        var jTeams = new JSONArray();
        for (String team : teams) {
            jTeams.put(team);
        }
        content.put("teams", jTeams);

        var jTeamSizes = new JSONArray();
        for (Integer teamSize : teamSizes) {
            jTeamSizes.put(teamSize);
        }
        content.put("teamSizes", jTeamSizes);

        content.put("currentSimulation", currentSimulation);

        return content;
    }

    public long getTime() {
        return time;
    }


}
