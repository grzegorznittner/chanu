package com.chanapps.four.component;

import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.GridView;

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

    private static final int MAX_COLUMN_WIDTH = 110;
    private static final int[] MAX_COLUMN_WIDTHS = {
            ServiceType.SELECTOR.ordinal(), MAX_COLUMN_WIDTH,
            ServiceType.BOARD.ordinal(), MAX_COLUMN_WIDTH,
            ServiceType.THREAD.ordinal(), MAX_COLUMN_WIDTH,
            ServiceType.WATCHLIST.ordinal(), MAX_COLUMN_WIDTH
    };

    private static final int MAX_COLUMN_WIDTH_LARGE = 140;
    private static final int[] MAX_COLUMN_WIDTHS_LARGE = {
            ServiceType.SELECTOR.ordinal(), MAX_COLUMN_WIDTH_LARGE,
            ServiceType.BOARD.ordinal(), MAX_COLUMN_WIDTH_LARGE,
            ServiceType.THREAD.ordinal(), MAX_COLUMN_WIDTH_LARGE,
            ServiceType.WATCHLIST.ordinal(), MAX_COLUMN_WIDTH_LARGE
    };

    private static final int MAX_COLUMN_WIDTH_XLARGE = 300;
    private static final int[] MAX_COLUMN_WIDTHS_XLARGE = {
            ServiceType.SELECTOR.ordinal(), MAX_COLUMN_WIDTH_XLARGE,
            ServiceType.BOARD.ordinal(), MAX_COLUMN_WIDTH_XLARGE,
            ServiceType.THREAD.ordinal(), MAX_COLUMN_WIDTH_XLARGE,
            ServiceType.WATCHLIST.ordinal(), MAX_COLUMN_WIDTH_XLARGE
    };

    private GridView g;
    private Display d;
    private int numColumns = 0;
    private int columnWidth = 0;
    private int columnHeight = 0;
    private int maxColumnWidth = 200;
    private int paddingTop = 0;
    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingBottom = 0;

    public ChanGridSizer(View g, Display d, ServiceType serviceType) {
    	if (g instanceof GridView) {
    		this.g = (GridView)g;
    	}
        else if (g != null) { // we support padding only on container views, not on the grid view itself
            paddingTop = g.getPaddingTop();
            paddingLeft = g.getPaddingLeft();
            paddingRight = g.getPaddingRight();
            paddingBottom = g.getPaddingBottom();
        }
        this.d = d;
        int layoutMask = g.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        int[] columnWidthArray;
        switch (layoutMask) {
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                columnWidthArray = MAX_COLUMN_WIDTHS_XLARGE;
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                columnWidthArray = MAX_COLUMN_WIDTHS_LARGE;
                break;
            default:
                columnWidthArray = MAX_COLUMN_WIDTHS;
        }
        for (int i = 0; i < columnWidthArray.length; i += 2) {
            if (serviceType.ordinal() == columnWidthArray[i]) {
                int dp = columnWidthArray[i + 1];
                maxColumnWidth = dpToPx(d, dp);
                return;
            }
        }
    }

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

    public void sizeGridToDisplay() {
        Point size = new Point();
        d.getSize(size);
        int width = size.x - paddingLeft - paddingRight;
        int height = size.y - paddingTop - paddingBottom;
        numColumns = width / maxColumnWidth == 1 ? 2 : width / maxColumnWidth;
        columnWidth = width / numColumns;
        columnHeight = columnWidth;
		if (DEBUG) Log.i(TAG, "sizeGridToDisplay width: " + width + ", height: " + height + ", numCols: " + numColumns);
        if (g != null) {
        	g.setNumColumns(numColumns);
        	g.setColumnWidth(columnWidth);
        }
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public int getColumnHeight() {
        return columnHeight;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public enum ServiceType {
        SELECTOR,
        BOARD,
        THREAD,
        WATCHLIST
    }
}
