package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/21/12
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSelectorAdapter extends BoardCursorAdapter {

    private static final String TAG = BoardCursorAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    public BoardSelectorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        if (cursor == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        String tag;
        int imageId = cursor.getInt(cursor.getColumnIndex(ChanHelper.BOARD_IMAGE_RESOURCE_ID));
        if (imageId != 0)
            tag = ChanHelper.BOARD_IMAGE_RESOURCE_ID;
        else
            tag = ChanHelper.BOARD_NAME;

        View v;
        if (convertView == null || !tag.equals(convertView.getTag())) {
            v = newView(context, parent, tag, position);
            v.setTag(tag);
            if (ChanHelper.BOARD_IMAGE_RESOURCE_ID.equals(tag)) {
                ImageView imageView = (ImageView)v.findViewById(R.id.grid_item_image);
                imageView.setTag(imageId);
            }
        } else {
            if (DEBUG) Log.d(TAG, "Reusing existing " + tag + " layout for " + position);
            v = convertView;
        }
        bindView(v, context, cursor);
        return v;
    }

    @Override
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        return mInflater.inflate(R.layout.selector_grid_item, parent, false);
    }

}
