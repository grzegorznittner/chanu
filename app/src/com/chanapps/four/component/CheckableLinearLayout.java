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
import android.widget.Checkable;
import android.widget.LinearLayout;
import com.chanapps.four.activity.R;

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
        if (isChecked)
            //setBackgroundColor(R.color.PaletteChanuGreenHalfOpacity);
            ;//setBackgroundColor(R.color.blue_base);
        else
            setBackgroundDrawable(null);
    }
}