package hr.uniri.diplomski.memory;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

/**
 * Created by Matija on 1.10.2017..
 */

public class Card {
    protected final Context context;

    protected ImageSwitcher switcher;
    protected int front;
    protected int back;

    Card(Context pContext, int front) {
        this.context = pContext;
        this.front = front;
        this.back = R.drawable.back;

        switcher = new ImageSwitcher(context);
        switcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                //___________________________________________________
                // scale pics here if needed
                ImageView pic = new ImageView(context);
                pic.setLayoutParams(new ViewSwitcher.LayoutParams(
                        GameActivity.getCardWidth(), ViewSwitcher.LayoutParams.WRAP_CONTENT
                ));
                pic.setPadding(2, 2, 2, 2);
                pic.setImageResource(back);
                pic.setBackgroundColor(Color.BLACK);
                pic.setAdjustViewBounds(true);
                return pic;
                //____________________________________________________
            }
        });
        switcher.setContentDescription("back");
        switcher.setInAnimation(context, android.R.anim.slide_in_left);
    }

    public void switchImage() {
        if(switcher.getContentDescription() == "back") {
            switcher.setImageResource(front);
            switcher.setContentDescription("front");
        }
        else {
            switcher.setImageResource(back);
            switcher.setContentDescription("back");
        }
    }

    public int getId() {
        return front;
    }

    public ImageSwitcher getView() {
        return switcher;
    }
}
