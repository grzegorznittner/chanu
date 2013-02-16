package com.chanapps.four.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridCursorAdapter extends AbstractBoardCursorAdapter {

    public BoardGridCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    public BoardGridCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to, boolean isWatchlist) {
        super(context, layout, viewBinder, from, to, isWatchlist);
    }

    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        if (ChanHelper.LAST_ITEM.equals(tag) && isWatchlist) {
            return mInflater.inflate(R.layout.board_grid_item_final_watchlist, parent, false);
        }
        else if (ChanHelper.LAST_ITEM.equals(tag)) {
            return mInflater.inflate(R.layout.board_grid_item_final, parent, false);
        }
        else if (ChanHelper.AD_ITEM.equals(tag)) {
            return mInflater.inflate(R.layout.board_grid_item_ad, parent, false);
        }
        else {
            return mInflater.inflate(R.layout.board_grid_item, parent, false);
        }
    }

    @Override
    protected int getThumbnailImageId() {
        return R.id.grid_item_image;
    }

}
