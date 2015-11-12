package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 3/31/13
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {

    boolean isChecked = false;
    public CheckableLinearLayout(Context context, AttributeSet attrs) {
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
            //setBackgroundColor(R.color.PaletteChanuGreenHalfOpacity);
            /*
            setBackgroundColor(R.color.blue_base);
            View v = getChildAt(0);
            if (v != null)
                v.setBackgroundColor(R.color.blue_base);
                */
            ;
        }
        else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                deprecatedSetBackgroundDrawable(null);
            else
                setBackground(null);
        }
    }

    @SuppressWarnings("deprecation")
    protected void deprecatedSetBackgroundDrawable(Drawable d) {
        setBackgroundDrawable(d);
    }

}