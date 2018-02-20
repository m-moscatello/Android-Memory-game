package hr.uniri.diplomski.memory;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Matija on 1.10.2017..
 */

public class BoardController {
    protected Context context;

    protected Card[][] cardField;
    protected List<Integer> imageIdsArray;

    protected int numCols;
    protected int numRows;
    protected int pairsLeft;

    protected int rowIndex;
    protected int colIndex;

    BoardController(Context context, int numRows, int numCols) {
        this.context = context;
        this.numRows = numRows;
        this.numCols = numCols;
        this.pairsLeft = (numRows*numCols)/2;

        cardField = new Card[numRows][numCols];
        imageIdsArray = new ArrayList<>(numRows*numCols);

        getImageIds();

        int id = 0;
        for( rowIndex=0; rowIndex<numRows; rowIndex++ ) {
            for( colIndex=0; colIndex<numCols; colIndex++ ) {
                cardField[rowIndex][colIndex] = new Card(context, imageIdsArray.get(id));
                id++;
            }
        }
    }

    public boolean checkPairs(ArrayList<Integer> coords) {
        final int x1 = coords.get(0);
        final int y1 = coords.get(1);
        final int x2 = coords.get(2);
        final int y2 = coords.get(3);

        if( cardField[x1][y1].getId() == cardField[x2][y2].getId() ) {
            pairsLeft -= 1;

            cardField[x1][y1].getView().setVisibility(View.INVISIBLE);
            cardField[x2][y2].getView().setVisibility(View.INVISIBLE);

            enableListeners();

            return true;
        }
        else {
            cardField[x1][y1].switchImage();
            cardField[x2][y2].switchImage();

            enableListeners();

            return false;
        }
    }

    public boolean isGameOver() {
        return pairsLeft == 0;
    }

    public void enableListeners() {
        for( Card[] row : cardField ) {
            for( Card card : row ) {
                card.getView().setClickable(true);
            }
        }
    }

    public void disableListeners() {
        for( Card[] row : cardField ) {
            for( Card card : row ) {
                card.getView().setClickable(false);
            }
        }
    }

    public Card getCard(int row, int col) {
        return cardField[row][col];
    }

    public void getImageIds() {
        List<Integer> tmpIdsList = new ArrayList<>();

        for( Integer i=1; i<35; i++ ) {
            String num = i.toString();
            int imageId = context.getResources().getIdentifier("card"+num, "drawable", context.getPackageName());
            tmpIdsList.add(imageId);
        }

        Collections.shuffle(tmpIdsList);

        for( int i=0, j=0; i<numRows*numCols; i++ ) {
            if(i < pairsLeft) {
                imageIdsArray.add( tmpIdsList.get(i) );
            }
            else {
                imageIdsArray.add( imageIdsArray.get(j) );
                j++;
            }
        }

        Collections.shuffle(imageIdsArray);
    }
}
