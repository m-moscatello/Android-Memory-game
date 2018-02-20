package hr.uniri.diplomski.memory;

/**
 * Created by Matija on 26.9.2017..
 */

public class Player {
    protected String name;
    protected int points;
    protected long time;

    Player(String name) {
        this.name = name;
        points = 0;
    }

    Player(String name, long time) {
        this.name = name;
        this.time = time;
    }

    public String getTimeTxt() {
        Integer seconds = (int) time/1000;
        Integer minutes = seconds/60;
        seconds %= 60;

        if ( seconds < 10 )
            return minutes.toString() +":0"+ seconds.toString();
        else
            return minutes.toString() +":"+ seconds.toString();
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public long getTime() { return time; }

    public void addPoint() {
        points++;
    }
}
