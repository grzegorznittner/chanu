package com.chanapps.four.component;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/21/12
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanGridSizer {

    private static final String TAG = ChanGridSizer.class.getSimpleName();
    private static final boolean DEBUG = false;

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
        return getCalculatedWidth(d.widthPixels, numColumns, requestedHorizontalSpacing);
    }

    static public int getCalculatedWidth(int widthPixels, int numColumns, int requestedHorizontalSpacing) {
        int availableSpace = widthPixels - requestedHorizontalSpacing * (numColumns + 1);
        int columnWidth = availableSpace / numColumns;
        if (DEBUG) Log.i(TAG, "sizeGridToDisplay availableSpace=" + availableSpace
                + " numColumns=" + numColumns
                + " columnWidth=" + columnWidth);
        return columnWidth;
    }

}
