package com.chanapps.four.component;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.GridView;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/21/12
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanGridSizer {

    private static final String TAG = ChanGridSizer.class.getSimpleName();
    private static final boolean DEBUG = true;

    static public int dpToPx(Display d, int dp) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);
        return dpToPx(displayMetrics, dp);
    }
    static public int dpToPx(DisplayMetrics displayMetrics, int dp) {
        float dpf = dp;
        int pixels = (int)(displayMetrics.density * dpf + 0.5f);
        return pixels;
    }

    static public int getCalculatedWidth(DisplayMetrics d, int numColumns, int requestedHorizontalSpacing) {
        int availableSpace = d.widthPixels - requestedHorizontalSpacing * (numColumns + 1);
        int columnWidth = availableSpace / numColumns;
        if (DEBUG) Log.i(TAG, "sizeGridToDisplay availableSpace=" + availableSpace
                + " density=" + d.density
                + " numColumns=" + numColumns
                + " columnWidth=" + columnWidth);
        return columnWidth;
    }

}
