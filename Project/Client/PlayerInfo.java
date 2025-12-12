package Project.Client;

public class PlayerInfo {
    public long id;
    public String name;
    public int points;
    public boolean pending = true;
    public boolean eliminated = false;

    public PlayerInfo(long id, String name) {
        this.id = id;
        this.name = name;
        this.points = 0;
    }
}