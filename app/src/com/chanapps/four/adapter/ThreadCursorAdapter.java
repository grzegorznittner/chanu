package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/21/12
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadCursorAdapter extends BoardCursorAdapter {

    private static final String TAG = BoardCursorAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    public ThreadCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
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
        String tag = null;
        String imageUrl = null;
        imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl != null && imageUrl.length() > 0) {
            tag = ChanHelper.POST_IMAGE_URL;
        } else {
            tag = ChanHelper.POST_SHORT_TEXT;
        }

        View v;
        if (convertView == null || !tag.equals(convertView.getTag())) {
            v = newView(context, parent, tag, position);
            v.setTag(tag);
            if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
                ImageView imageView = (ImageView)v.findViewById(R.id.grid_item_image);
                imageView.setTag(imageUrl);
            }
            else {
                //TextView textView = (TextView)v.findViewById(R.id.thread_grid_item_text);
                //textView.setTag(shortText);
            }
        } else {
            if (DEBUG) Log.d(TAG, "Reusing existing " + tag + " layout for " + position);
            /*
               if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
                   ImageView imageView = (ImageView)convertView.findViewById(R.id.board_activity_grid_item_image);
                   if (imageView != null && !imageUrl.equals(imageView.getTag())) {
                       //imageView.setImageResource(R.drawable.stub_image);
                   }
               }
               */
            v = convertView;
        }
        bindView(v, context, cursor);
        return v;
    }

    @Override
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
            return mInflater.inflate(R.layout.thread_grid_item, parent, false);
        } else {
            return mInflater.inflate(R.layout.thread_grid_item_no_image, parent, false);
        }
    }

}
