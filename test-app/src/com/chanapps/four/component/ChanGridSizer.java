package com.chanapps.four.component;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.widget.GridView;
import com.chanapps.four.test.BoardSelectorActivity;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/21/12
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanGridSizer {

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
        numColumns = width / 300 == 1 ? 2 : width / 300;
        columnWidth = (width - 15) / numColumns;
        Log.i(BoardSelectorActivity.TAG, "sizeGridToDisplay width: " + width + ", height: " + height + ", numCols: " + numColumns);
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
