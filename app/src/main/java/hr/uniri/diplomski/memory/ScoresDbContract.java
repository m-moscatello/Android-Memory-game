package hr.uniri.diplomski.memory;

import android.provider.BaseColumns;

/**
 * Created by Matija on 7.10.2017..
 */

public final class ScoresDbContract {

    private ScoresDbContract() {}

    public static class ScoresDb implements BaseColumns {
        public static final String TABLE_NAME = "High_Scores";
        public static final String COLUMN_NAME_PLAYER = "Player";
        public static final String COLUMN_NAME_TIME = "Time";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE "+ ScoresDb.TABLE_NAME +" ("+
                ScoresDb._ID +" INTEGER PRIMARY KEY, "+
                ScoresDb.COLUMN_NAME_PLAYER +" VARCHAR(20),"+
                ScoresDb.COLUMN_NAME_TIME +" INT)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS "+ ScoresDb.TABLE_NAME;
    }
}
