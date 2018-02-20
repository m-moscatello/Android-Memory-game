package hr.uniri.diplomski.memory;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    protected ConstraintLayout mainFrame;
    protected ImageView singlePlayerBtn;
    protected ImageView multiPlayerBtn;
    protected ImageView toggleSoundBtn;
    protected ImageView scoresBtn;

    protected Intent getGameActivity;

    protected Thread music;
    protected MediaPlayer song;

    protected PopupWindow pw;

    protected ScoresDbHelper dbHelper;
    protected SQLiteDatabase db;

    protected View.OnClickListener dismissPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainFrame = (ConstraintLayout) findViewById(R.id.mainFrame);
        singlePlayerBtn = (ImageView) findViewById(R.id.singlePlayerBtn);
        multiPlayerBtn = (ImageView) findViewById(R.id.multiPlayerBtn);
        toggleSoundBtn = (ImageView) findViewById(R.id.toggleSoundBtn);
        scoresBtn = (ImageView) findViewById(R.id.scoresBtn);

        mainFrame.getForeground().setAlpha(0);

        getGameActivity = new Intent(this, GameActivity.class);
        getGameActivity.putExtra("wakeLock", false);

        song = MediaPlayer.create(this, R.raw.marimba_boy);

        dbHelper = new ScoresDbHelper(this);
        db = dbHelper.getReadableDatabase();

        music = new Thread(new Runnable() {
            @Override
            public void run() {
                song.setLooping(true);

                toggleSoundBtn.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v){
                        if(song.isPlaying()){
                            song.pause();
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            getGameActivity.putExtra("wakeLock", false);
                            toggleSoundBtn.setImageResource(R.drawable.sound_off_img);
                        }
                        else {
                            song.start();
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            getGameActivity.putExtra("wakeLock", true);
                            toggleSoundBtn.setImageResource(R.drawable.sound_on_img);
                        }
                    }
                });
            }
        });

        music.run();

        dismissPopup = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pw.dismiss();
                mainFrame.getForeground().setAlpha(0);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        singlePlayerBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                getGameActivity.putExtra("mode", 's');
                startActivity(getGameActivity);
            }
        });

        multiPlayerBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                getGameActivity.putExtra("mode", 'm');
                startActivity(getGameActivity);
            }
        });

        scoresBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                initiateScoresPopup(mainFrame.getWidth()-100, mainFrame.getHeight()-100);
            }
        });
    }

    @Override
    protected void onDestroy() {
        song.release();
        song = null;

        dbHelper.close();

        super.onDestroy();
    }

    public void initiateScoresPopup(int width, int height) {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View scoresPopup = inflater.inflate(R.layout.scores_popup, (ViewGroup) findViewById(R.id.scoresPopup));
        final LinearLayout dataContainer = scoresPopup.findViewById(R.id.dataContainer);

        pw = new PopupWindow(scoresPopup, width, height, true);
        pw.showAtLocation(scoresPopup, Gravity.CENTER, 0, 50);

        mainFrame.getForeground().setAlpha(200);

        Button backBtn = scoresPopup.findViewById(R.id.scoresBackBtn);
        backBtn.setOnClickListener(dismissPopup);

        dataContainer.post(new Runnable() {
            @Override
            public void run() {
                int fieldWidth = dataContainer.getWidth();

                //Line layout parameters
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lineParams.gravity = Gravity.CENTER_HORIZONTAL;

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        Math.round(fieldWidth/2), ViewGroup.LayoutParams.WRAP_CONTENT
                );

                List<Player> players = getTheBest();
                if( players.isEmpty() ) {
                    TextView msgTxt = new TextView(getApplicationContext());
                    msgTxt.setText("No records");
                    msgTxt.setTextColor(Color.BLACK);
                    msgTxt.setTextSize(dpToPx(10, getApplicationContext()));
                    dataContainer.addView(msgTxt);
                    msgTxt.setGravity(Gravity.CENTER);
                }
                else {
                    Integer c=1;
                    for( Player p : players) {
                        //Generate line layout
                        LinearLayout line = new LinearLayout(getApplicationContext());
                        line.setLayoutParams(lineParams);
                        line.setOrientation(LinearLayout.HORIZONTAL);

                        //Name text
                        TextView name = new TextView(getApplicationContext());
                        name.setLayoutParams(textParams);
                        name.setText(c.toString()+". "+p.getName());
                        name.setTextColor(Color.BLACK);
                        name.setTextSize(dpToPx(8, getApplicationContext()));
                        line.addView(name);
                        name.setGravity(Gravity.LEFT);

                        //Time text
                        TextView time = new TextView(getApplicationContext());
                        time.setLayoutParams(textParams);
                        time.setText(p.getTimeTxt());
                        time.setTextColor(Color.BLACK);
                        time.setTextSize(dpToPx(8, getApplicationContext()));
                        line.addView(time);
                        time.setGravity(Gravity.RIGHT);

                        dataContainer.addView(line);
                        c++;
                    }
                }
            }
        });
    }

    public List<Player> getTheBest() {
        int c = 0;
        // uncomment to clear high score history
        //int rowsAffected = db.delete(ScoresDbContract.ScoresDb.TABLE_NAME, "1", null);

        String[] projection = {
                ScoresDbContract.ScoresDb.COLUMN_NAME_PLAYER,
                ScoresDbContract.ScoresDb.COLUMN_NAME_TIME
        };
        String sortOrder = ScoresDbContract.ScoresDb.COLUMN_NAME_TIME +" ASC";

        Cursor cursor = db.query(
                ScoresDbContract.ScoresDb.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        List<Player> players = new ArrayList<>();
        while( cursor.moveToNext() && c<10 ) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ScoresDbContract.ScoresDb.COLUMN_NAME_PLAYER));
            long time = cursor.getLong(cursor.getColumnIndexOrThrow(ScoresDbContract.ScoresDb.COLUMN_NAME_TIME));

            players.add( new Player(name, time) );
            c++;
        }

        return sortPlayers(players);
    }

    public List<Player> sortPlayers(List<Player> players) {
        List<Player> sortedPlayers = new ArrayList<>();
        long min;
        int minIndex;

        while( players.size()>0 ) {
            min = players.get(0).getTime();
            minIndex = 0;

            for(int i=1; i<players.size(); i++) {
                if( players.get(i).getTime() < min ) {
                    min = players.get(i).getTime();
                    minIndex = i;
                }
            }

            sortedPlayers.add(players.get(minIndex));
            players.remove(minIndex);
        }

        return sortedPlayers;
    }

    public float dpToPx(float dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }
}
