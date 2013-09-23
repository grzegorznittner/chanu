package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 3/31/13
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.chanapps.four.activity.R;

public class CheckableFrameLayout extends FrameLayout implements Checkable {

    boolean isChecked = false;
    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setBackground();
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
        setBackground();
    }

    @Override
    public void toggle() {
        isChecked = !isChecked;
        setBackground();
    }

    protected void setBackground() {
        if (isChecked) {
            setForeground(getResources().getDrawable(R.color.PaletteSelectorFourth));
            /*
            //setBackgroundColor(R.color.PaletteSelectorHalf);
            View v = getChildAt(0);
            if (v != null)
                v.setBackgroundColor(R.color.blue_base);
                */
        }
        else {
            setForeground(null);
        }
    }
}