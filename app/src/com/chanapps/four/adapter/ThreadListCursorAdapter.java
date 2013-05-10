package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanPost;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadListCursorAdapter extends AbstractThreadCursorAdapter {

    protected static final String TAG = ThreadListCursorAdapter.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected static final int TYPE = R.id.THREAD_VIEW_TYPE;
    protected static final String HEADER = "header";
    protected static final String ITEM = "item";

    public ThreadListCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (DEBUG) Log.i(TAG, "Getting view for pos=" + position);
        Cursor cursor = getCursor();
        if (cursor == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        String tag = convertView == null ? "" : (String)convertView.getTag(TYPE);
        if (tag == null)
            tag = "";
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        String newTag = (flags & ChanPost.FLAG_IS_HEADER) > 0 ? HEADER : ITEM;

        View v = (convertView == null || !tag.equals(newTag))
            ? newView(context, parent, newTag, position)
            : convertView;
        if (DEBUG && v == convertView)
            Log.i(TAG, "Reusing existing view=" + v);

        if (DEBUG) Log.i(TAG, "Binding view=" + v + " for pos=" + position);
        bindView(v, context, cursor);
        return v;
    }

    @Override
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        View v = HEADER.equals(tag)
            ? mInflater.inflate(R.layout.thread_list_header, parent, false)
            : mInflater.inflate(R.layout.thread_list_item, parent, false);
        v.setTag(TYPE, tag);
        return v;
    }

}
