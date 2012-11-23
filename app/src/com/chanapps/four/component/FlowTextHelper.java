package com.chanapps.four.component;

import android.graphics.Point;
import android.text.SpannableString;
import android.text.Spanned;
import android.widget.TextView;
import com.chanapps.four.component.MyLeadingMarginSpan2;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 10/19/12
 * Time: 5:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlowTextHelper {
    public static final int RIGHT_MARGIN = 10;
    public static final int BOTTOM_MARGIN = 0;

    public static void tryFlowText(String text, Point imageDimensions, TextView messageView) {
        int width = imageDimensions.x + RIGHT_MARGIN;
        int height = imageDimensions.y + BOTTOM_MARGIN;
        float textLineHeight = messageView.getPaint().getTextSize();
        int lines = (int)Math.round(height / textLineHeight);
        SpannableString ss = new SpannableString(text);
        Object what = new MyLeadingMarginSpan2(lines, width);
        ss.setSpan(what, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageView.setText(ss);
    }

}

