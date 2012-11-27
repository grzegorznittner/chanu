package com.chanapps.four.component;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.widget.GridView;
import com.chanapps.four.activity.BoardSelectorActivity;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/21/12
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanGridSizer {

    private static final String TAG = ChanGridSizer.class.getSimpleName();

    private static final int MAX_COLUMN_WIDTH = 240;

    private GridView g;
    private Display d;
    private int numColumns = 0;
    private int columnWidth = 0;

    public ChanGridSizer(GridView g, Display d) {
        this.g = g;
        this.d = d;
    }

    public void sizeGridToDisplay() {
        Point size = new Point();
        d.getSize(size);
        int width = size.x;
        int height = size.y;
        numColumns = width / MAX_COLUMN_WIDTH == 1 ? 2 : width / MAX_COLUMN_WIDTH;
        columnWidth = (width - 15) / numColumns;
        Log.i(TAG, "sizeGridToDisplay width: " + width + ", height: " + height + ", numCols: " + numColumns);
        g.setNumColumns(numColumns);
        g.setColumnWidth(columnWidth);
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public int getNumColumns() {
        return numColumns;
    }
}
