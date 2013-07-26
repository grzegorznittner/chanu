package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.viewer.BoardGridViewHolder;
import com.chanapps.four.viewer.ThreadViewHolder;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridCursorAdapter extends AbstractBoardCursorAdapter {

    protected static final int TYPE_GRID_ITEM = 0;
    protected static final int TYPE_MAX_COUNT = 1;

    public BoardGridCursorAdapter(Context context, ViewBinder viewBinder) {
        super(context,
                R.layout.board_grid_item,
                viewBinder,
                new String[]{
                        ChanThread.THREAD_SUBJECT,
                        ChanThread.THREAD_HEADLINE,
                        ChanThread.THREAD_COUNTRY_FLAG_URL,
                        ChanThread.THREAD_THUMBNAIL_URL
                },
                new int[]{
                        R.id.grid_item_thread_subject,
                        R.id.grid_item_thread_info,
                        R.id.grid_item_country_flag,
                        R.id.grid_item_thread_thumb
                }
        );
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_GRID_ITEM;
    }

    @Override
    protected View newView(ViewGroup parent, int tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        View v = mInflater.inflate(R.layout.board_grid_item, parent, false);
        BoardGridViewHolder viewHolder = new BoardGridViewHolder(v);
        v.setTag(R.id.VIEW_TAG_TYPE, tag);
        v.setTag(R.id.VIEW_HOLDER, viewHolder);
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }
}
