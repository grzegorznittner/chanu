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
import android.widget.RelativeLayout;
import com.chanapps.four.activity.R;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    boolean isChecked = false;
    int backgroundDrawable;

    public CheckableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        backgroundDrawable = attrs.getAttributeResourceValue(R.attr.background, R.color.PaletteSelector);
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
            setBackgroundResource(backgroundDrawable);
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