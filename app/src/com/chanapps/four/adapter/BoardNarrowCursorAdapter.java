package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.viewer.BoardViewHolder;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardNarrowCursorAdapter extends AbstractBoardCursorAdapter {

    protected static final int TYPE_GRID_HEADER = 0;
    protected static final int TYPE_GRID_ITEM = 1;
    protected static final int TYPE_MAX_COUNT = 2;

    public BoardNarrowCursorAdapter(Context context, ViewBinder viewBinder) {
        super(context, viewBinder);
    }

    @Override
    public int getItemViewType(int position) {
        Cursor c = getCursor();
        if (c != null
                && c.moveToPosition(position)
                && (c.getInt(c.getColumnIndex(ChanThread.THREAD_FLAGS)) & ChanThread.THREAD_FLAG_HEADER) > 0)
            return TYPE_GRID_HEADER;
        else
            return TYPE_GRID_ITEM;
    }

    @Override
    protected View newView(ViewGroup parent, int tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        int layoutId = getItemViewType(position) == TYPE_GRID_HEADER
                ? R.layout.board_grid_header_narrow
                : R.layout.board_grid_item_narrow;
        View v = mInflater.inflate(layoutId, parent, false);
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
