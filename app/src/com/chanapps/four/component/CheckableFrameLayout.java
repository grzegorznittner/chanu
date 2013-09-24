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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.chanapps.four.activity.R;

public class CheckableFrameLayout extends FrameLayout implements Checkable {

    private static final String TAG = CheckableFrameLayout.class.getSimpleName();
    private static final boolean DEBUG = true;

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
        if (DEBUG) Log.i(TAG, "setBackground() checked=" + isChecked);
        if (isChecked) {
            for (int i = 0; i < getChildCount(); i++) {
                if (i > 0)
                    return;
                View v = getChildAt(i);
                if (v != null && v instanceof ViewGroup && ((ViewGroup)v).getChildCount() > 0) {
                    v = ((ViewGroup)v).getChildAt(0);
                    if (v != null && v instanceof ViewGroup && ((ViewGroup)v).getChildCount() > 1) {
                        v = ((ViewGroup)v).getChildAt(1);
                        if (v != null)
                            v.setVisibility(View.VISIBLE);
                    }
                }
            }
            //setForeground(getResources().getDrawable(R.color.PaletteSelectorFourth));
        }
        else {
            //setForeground(null);
            for (int i = 0; i < getChildCount(); i++) {
                if (i > 0)
                    return;
                View v = getChildAt(i);
                if (v != null && v instanceof ViewGroup && ((ViewGroup)v).getChildCount() > 0) {
                    v = ((ViewGroup)v).getChildAt(0);
                    if (v != null && v instanceof ViewGroup && ((ViewGroup)v).getChildCount() > 1) {
                        v = ((ViewGroup)v).getChildAt(1);
                        if (v != null)
                            v.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}