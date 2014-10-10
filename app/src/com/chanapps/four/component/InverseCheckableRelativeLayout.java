package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 3/31/13
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.chanapps.four.activity.R;

public class InverseCheckableRelativeLayout extends RelativeLayout implements Checkable {

    boolean isChecked = false;
    int checkedBackgroundDrawable;
    int inverseBackgroundDrawable;
    int inverseForegroundDrawable;

    public InverseCheckableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InverseCheckableRelativeLayout,
                0,
                0
        );
        try {
            checkedBackgroundDrawable = a.getResourceId(R.styleable.InverseCheckableRelativeLayout_checkedBackground, 0);
            inverseBackgroundDrawable = a.getResourceId(R.styleable.InverseCheckableRelativeLayout_inverseBackground, 0);
            inverseForegroundDrawable = a.getResourceId(R.styleable.InverseCheckableRelativeLayout_inverseForeground, 0);
        }
        finally {
            a.recycle();
        }
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
        FrameLayout child = (FrameLayout)this.findViewById(R.id.frame_child);
        if (isChecked) {
            setBackgroundResource(checkedBackgroundDrawable);
            if (child != null)
                child.setForeground(getResources().getDrawable(R.color.PaletteDrawerUncheckedFg));
        }
        else {
            setBackgroundResource(inverseBackgroundDrawable);
            if (child != null)
                child.setForeground(getResources().getDrawable(inverseForegroundDrawable));
        }
    }
}