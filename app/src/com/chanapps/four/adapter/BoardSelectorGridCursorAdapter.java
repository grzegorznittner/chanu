package com.chanapps.four.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSelectorGridCursorAdapter extends AbstractBoardCursorAdapter {

    private int columnWidth;
    private int columnHeight;

    public BoardSelectorGridCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to, int columnWidth, int columnHeight) {
        super(context, layout, viewBinder, from, to);
        this.columnWidth = columnWidth;
        this.columnHeight = columnHeight;
    }

    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        View view = mInflater.inflate(R.layout.board_selector_grid_item, parent, false);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && columnWidth > 0 && columnHeight > 0) {
            params.width = columnWidth;
            params.height = columnHeight;
        }
        return view;
    }

}
