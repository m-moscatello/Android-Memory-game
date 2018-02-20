package hr.uniri.diplomski.memory;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {
    protected ConstraintLayout gameFrame;
    protected GridLayout cardField;
    protected ConstraintLayout infoFrame;
    protected LinearLayout endGameLayout;
    protected TextView endGameTxt;

    protected static int cardWidth;
    protected int gridRows, gridCols;
    protected int rowIndex, colIndex;
    protected float aspectRatio;

    protected Bundle bundle;
    protected char mode;

    protected AlertDialog.Builder alert;

    protected Player player1;
    protected Player player2;
    protected Player currentPlayer;

    protected TextView player1NameTxt;
    protected TextView player2NameTxt;
    protected TextView timerTxt;

    protected Handler timerHandler;
    protected Runnable timerRunnable;
    protected long timer;

    protected Handler checkPairsHandler;
    protected Runnable checkPairsRunnable;

    protected BoardController controller;
    protected int clickCounter;
    protected boolean firstClick;

    protected ArrayList<Integer> coords;

    protected ScoresDbHelper dbHelper;
    protected SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameFrame = (ConstraintLayout) findViewById(R.id.gameFrame);
        cardField = (GridLayout) findViewById(R.id.cardField);
        infoFrame = (ConstraintLayout) findViewById(R.id.infoFrame);
        endGameLayout = (LinearLayout) findViewById(R.id.endGameLayout);
        endGameTxt = (TextView) findViewById(R.id.endGameTxt);

        alert = new AlertDialog.Builder(this);

        //get game mode
        bundle = new Bundle(getIntent().getExtras());
        mode = bundle.getChar("mode");
        if( bundle.getBoolean("wakeLock") )
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        clickCounter = 0;
        timer = 0;
        firstClick = true;
        coords = new ArrayList<>(4);

        dbHelper = new ScoresDbHelper(this);
        db = dbHelper.getWritableDatabase();

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timer += 1000;
                Integer seconds = (int) timer/1000;
                Integer minutes = seconds/60;
                seconds %= 60;

                if ( seconds < 10 )
                    timerTxt.setText(minutes.toString() +":0"+ seconds.toString());
                else
                    timerTxt.setText(minutes.toString() +":"+ seconds.toString());

                timerHandler.postDelayed(this, 1000);
            }
        };

        checkPairsHandler = new Handler();
        checkPairsRunnable = new Runnable() {
            @Override
            public void run() {
                if( controller.checkPairs(coords) ) {
                    currentPlayer.addPoint();
                    updatePointsTxt(currentPlayer);
                }
                else {
                    if( mode == 'm' ) {
                        changePlayer();
                    }
                }

                coords.clear();
                clickCounter = 0;

                if( controller.isGameOver() ){
                    timerHandler.removeCallbacks(timerRunnable);

                    if ( mode == 's' ) {
                        endGameTxt.setText("Congratulations!");
                        insertIntoDb();
                    }
                    else endGameTxt.setText("The winner is... "+ getWinner());
                    endGameLayout.setVisibility(View.VISIBLE);
                }
            }
        };

        cardField.post(new Runnable() {
            @Override
            public void run() {
                //set up card grid
                int gridWidth = cardField.getWidth();
                int i = 4;

                while(gridWidth/i > 300 && i<8){
                    i += 2;
                }

                if ( getAspectRatio() < 1.6 ) gridRows = i;
                else gridRows = i+1;
                gridCols = i;

                cardField.setRowCount(gridRows);
                cardField.setColumnCount(gridCols);

                cardWidth = Math.round(gridWidth/i);
                controller = new BoardController(getApplicationContext(), gridRows, gridCols);

                drawCards();

                getPlayerNames();
            }
        });
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    public void drawCards() {
        for( rowIndex=0; rowIndex < gridRows; rowIndex++ ) {
            for( colIndex=0; colIndex < gridCols; colIndex++ ) {
                Card card = controller.getCard(rowIndex, colIndex);
                cardField.addView(card.getView());

                card.getView().setOnClickListener(new View.OnClickListener() {
                    int row = rowIndex;
                    int col = colIndex;

                    @Override
                    public void onClick(View view) {
                        if( firstClick ) {
                            timerHandler.postDelayed(timerRunnable, 0);
                            firstClick = false;
                        }

                        controller.getCard(row, col).switchImage();
                        coords.add(row);
                        coords.add(col);
                        clickCounter++;

                        if( clickCounter == 1 ) {
                            controller.getCard(row, col).getView().setClickable(false);
                        }
                        if( clickCounter == 2 ) {
                            controller.disableListeners();

                            checkPairsHandler.postDelayed(checkPairsRunnable, 1300);
                        }
                    }
                });
            }
        }
    }

    public void getPlayerNames() {
        LinearLayout ll = new LinearLayout(getApplicationContext());
        ll.setOrientation(LinearLayout.VERTICAL);

        ShapeDrawable border = new ShapeDrawable(new RectShape());
        border.getPaint().setColor(Color.BLACK);
        border.getPaint().setStyle(Paint.Style.STROKE);
        border.getPaint().setStrokeWidth(3);

        final EditText player1Input = new EditText(getApplicationContext());
        player1Input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        player1Input.setTextColor(Color.BLACK);
        player1Input.setBackground(border);

        TextView player1NameTxt = new TextView(getApplicationContext());
        player1NameTxt.setTextColor(Color.BLACK);

        if (mode == 's') {
            player1NameTxt.setText("Player name:");
            ll.addView(player1NameTxt);
            ll.addView(player1Input);

            alert.setView(ll);
            alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String name;
                    if(player1Input.getText().toString().isEmpty()){
                        name = "Anon";
                    }
                    else name = player1Input.getText().toString();

                    player1 = new Player(name);
                    currentPlayer = player1;

                    fillInfoFrame();
                }
            });
        }
        else if(mode == 'm') {
            final EditText player2Input = new EditText(getApplicationContext());
            player2Input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            player2Input.setTextColor(Color.BLACK);
            player2Input.setBackground(border);

            player1NameTxt.setText("Player 1 name:");

            TextView player2NameTxt = new TextView(getApplicationContext());
            player2NameTxt.setTextColor(Color.BLACK);
            player2NameTxt.setText("Player 2 name:");

            ll.addView(player1NameTxt);
            ll.addView(player1Input);
            ll.addView(player2NameTxt);
            ll.addView(player2Input);

            alert.setView(ll);
            alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String name1;
                    String name2;

                    if(player1Input.getText().toString().isEmpty()){
                        name1 = "Anon1";
                    }
                    else name1 = player1Input.getText().toString();

                    if(player2Input.getText().toString().isEmpty()){
                        name2 = "Anon2";
                    }
                    else name2 = player2Input.getText().toString();

                    player1 = new Player(name1);
                    player2 = new Player(name2);
                    currentPlayer = player1;

                    fillInfoFrame();
                }
            });
        }

        alert.show();
    }

    public void fillInfoFrame() {
        int frameHeight = gameFrame.getHeight();
        int gridHeight = Math.round(cardField.getHeight());
        int infoHeight = frameHeight - gridHeight;

        infoFrame.getLayoutParams().height = infoHeight;
        infoFrame.requestLayout();

        // add players and timer
        GridLayout namesLayout = new GridLayout(this);
        namesLayout.setId(View.generateViewId());
        namesLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, Math.round(infoHeight/2)
        ));
        namesLayout.setRowCount(1);

        infoFrame.addView(namesLayout);

        if(mode == 's') {
            namesLayout.setColumnCount(1);

            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            linearLayout.setGravity(Gravity.CENTER);
            namesLayout.addView(linearLayout);

            player1NameTxt = new TextView(this);
            player1NameTxt.setTextSize(dpToPx(8, this));
            player1NameTxt.setTextColor(Color.WHITE);
            player1NameTxt.setText(player1.getName()+ ": " +player1.getPoints());
            linearLayout.addView(player1NameTxt);

            player1NameTxt.setGravity(Gravity.CENTER);
        }
        else if(mode == 'm') {
            namesLayout.setColumnCount(2);

            LinearLayout leftName = new LinearLayout(this);
            leftName.setLayoutParams(new ViewGroup.LayoutParams(
                    infoFrame.getWidth()/2, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            leftName.setGravity(Gravity.CENTER);
            namesLayout.addView(leftName);

            player1NameTxt = new TextView(this);
            player1NameTxt.setTextSize(dpToPx(8, this));
            player1NameTxt.setTextColor(Color.parseColor("#006633"));
            player1NameTxt.setText(player1.getName()+ ": " +player1.getPoints());
            leftName.addView(player1NameTxt);
            player1NameTxt.setGravity(Gravity.CENTER);

            LinearLayout rightName = new LinearLayout(this);
            rightName.setLayoutParams(new ViewGroup.LayoutParams(
                    infoFrame.getWidth()/2, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            rightName.setGravity(Gravity.CENTER);
            namesLayout.addView(rightName);

            player2NameTxt = new TextView(this);
            player2NameTxt.setTextSize(dpToPx(8, this));
            player2NameTxt.setTextColor(Color.WHITE);
            player2NameTxt.setText(player2.getName()+ ": " +player2.getPoints());
            rightName.addView(player2NameTxt);
            player2NameTxt.setGravity(Gravity.CENTER);
        }

        ConstraintSet gridConstraints = new ConstraintSet();
        gridConstraints.clone(infoFrame);

        gridConstraints.connect(namesLayout.getId(), ConstraintSet.TOP, infoFrame.getId(), ConstraintSet.TOP, 0);
        gridConstraints.connect(namesLayout.getId(), ConstraintSet.LEFT, infoFrame.getId(), ConstraintSet.LEFT, 0);
        gridConstraints.connect(namesLayout.getId(), ConstraintSet.RIGHT, infoFrame.getId(), ConstraintSet.RIGHT, 0);
        gridConstraints.applyTo(infoFrame);

        timerTxt = new TextView(this);
        timerTxt.setId(View.generateViewId());
        timerTxt.setTextSize(dpToPx(14, this));
        timerTxt.setTextColor(Color.WHITE);
        timerTxt.setText("0:00");
        infoFrame.addView(timerTxt);

        ConstraintSet timerConstraints = new ConstraintSet();
        timerConstraints.clone(infoFrame);

        timerConstraints.connect(timerTxt.getId(), ConstraintSet.BOTTOM, infoFrame.getId(), ConstraintSet.BOTTOM);
        timerConstraints.connect(timerTxt.getId(), ConstraintSet.TOP, namesLayout.getId(), ConstraintSet.BOTTOM);
        timerConstraints.centerHorizontally(timerTxt.getId(), infoFrame.getId());
        timerConstraints.applyTo(infoFrame);
    }

    public void changePlayer() {
        if( currentPlayer == player1 ) {
            currentPlayer = player2;
            player2NameTxt.setTextColor(Color.parseColor("#006633"));
            player1NameTxt.setTextColor(Color.WHITE);
        }
        else {
            currentPlayer = player1;
            player1NameTxt.setTextColor(Color.parseColor("#006633"));
            player2NameTxt.setTextColor(Color.WHITE);
        }
    }

    public void updatePointsTxt(Player player) {
        if( player == player1 )
            player1NameTxt.setText(player1.getName()+ ": " +player1.getPoints());
        else
            player2NameTxt.setText(player2.getName()+ ": " +player2.getPoints());
    }

    public String getWinner() {
        if( player1.getPoints() > player2.getPoints() )
            return player1.getName()+"!";
        else if( player1.getPoints() < player2.getPoints() )
            return player2.getName()+"!";

        return "Nobody!";
    }

    public float getAspectRatio() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        float ratio = (float) metrics.heightPixels / (float) metrics.widthPixels;

        return ratio;
    }

    public void insertIntoDb() {
        ContentValues values = new ContentValues();
        values.put(ScoresDbContract.ScoresDb.COLUMN_NAME_PLAYER, player1.getName());
        values.put(ScoresDbContract.ScoresDb.COLUMN_NAME_TIME, timer);

        long newRowId = db.insert(ScoresDbContract.ScoresDb.TABLE_NAME, null, values);
    }

    public float dpToPx(float dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    public static int getCardWidth() {
        return cardWidth;
    }
}
