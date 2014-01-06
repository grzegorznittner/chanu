package com.chanapps.four.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.viewer.BoardViewHolder;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSmallCursorAdapter extends AbstractBoardCursorAdapter {

    protected static final int TYPE_GRID_ITEM = 0;
    protected static final int TYPE_MAX_COUNT = 1;

    public BoardSmallCursorAdapter(Context context, ViewBinder viewBinder) {
        super(context, viewBinder);
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_GRID_ITEM;
    }

    @Override
    protected View newView(ViewGroup parent, int tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        View v = mInflater.inflate(R.layout.board_grid_item_small, parent, false);
        BoardViewHolder viewHolder = new BoardViewHolder(v);
        v.setTag(R.id.VIEW_TAG_TYPE, tag);
        v.setTag(R.id.VIEW_HOLDER, viewHolder);
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }
}
