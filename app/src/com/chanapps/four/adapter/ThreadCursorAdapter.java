package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.ChanHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/21/12
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadCursorAdapter extends BoardCursorAdapter {

    private static final String TAG = ThreadCursorAdapter.class.getSimpleName();

    private long highlightPostNo = 0;
    private Set<Long> highlightPrevPostNos = new HashSet<Long>();
    private Set<Long> highlightNextPostNos = new HashSet<Long>();

    private static final boolean DEBUG = true;

    public ThreadCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    public void setHighlightPosts(long highlightPostNo, long[] prevPostNos, long[] nextPostNos) {
        this.highlightPostNo = highlightPostNo;
        highlightPrevPostNos.clear();
        highlightNextPostNos.clear();
        if (prevPostNos != null)
            for (long postNo : prevPostNos)
                highlightPrevPostNos.add(postNo);
        if (nextPostNos != null)
            for (long postNo : nextPostNos)
                highlightNextPostNos.add(postNo);
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
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        if (DEBUG) Log.d(TAG, "getView called for position="+position + " postNo=" + postNo);
        if (position == 0) { // thread header
            tag = ChanHelper.POST_RESTO;
        }
        else if (postNo == 0) { // null spacer to give room for thread header
            tag = ChanHelper.POST_OMITTED_POSTS;
        }
        else if (imageUrl != null && imageUrl.length() > 0) {
            tag = ChanHelper.POST_IMAGE_URL;
        } else {
            tag = ChanHelper.POST_SHORT_TEXT;
        }

        View v;
        if (convertView == null || !tag.equals(convertView.getTag())) {
            v = newView(context, parent, tag, position);
            v.setTag(tag);
            if (imageUrl != null && imageUrl.length() > 0) {
                ImageView imageView = (ImageView)v.findViewById(R.id.grid_item_image);
                if (imageView != null)
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

        if (v != null && v instanceof RelativeLayout && postNo != 0) {
            RelativeLayout r = (RelativeLayout)v;
            if (highlightPostNo == postNo) {
                v.findViewById(R.id.grid_item_self_highlight).setVisibility(View.VISIBLE);
                v.findViewById(R.id.grid_item_prev_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_next_highlight).setVisibility(View.INVISIBLE);
            }
            else if (highlightPrevPostNos.contains(postNo)) {
                v.findViewById(R.id.grid_item_self_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_prev_highlight).setVisibility(View.VISIBLE);
                v.findViewById(R.id.grid_item_next_highlight).setVisibility(View.INVISIBLE);
            }
            else if (highlightNextPostNos.contains(postNo)) {
                v.findViewById(R.id.grid_item_self_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_prev_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_next_highlight).setVisibility(View.VISIBLE);
            }
            else {
                v.findViewById(R.id.grid_item_self_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_prev_highlight).setVisibility(View.INVISIBLE);
                v.findViewById(R.id.grid_item_next_highlight).setVisibility(View.INVISIBLE);
            }
        }

        //if (postNo != 0) {
            bindView(v, context, cursor);
        //}
        return v;
    }

    private static final int GRID_ITEM_HEIGHT_DP = 200;

    @Override
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        if (ChanHelper.POST_RESTO.equals(tag)) { // first item is the post which started the thread
            RelativeLayout view = (RelativeLayout)mInflater.inflate(R.layout.thread_grid_item_header, parent, false);
            AbsListView.LayoutParams viewParams = (AbsListView.LayoutParams)view.getLayoutParams();
            if (viewParams == null) {
                int viewHeightPx = ChanGridSizer.dpToPx(context.getResources().getDisplayMetrics(), GRID_ITEM_HEIGHT_DP);
                viewParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, viewHeightPx);
            }
            viewParams.width = parent.getWidth();
            view.setLayoutParams(viewParams);
            return view;
        }
        else if (ChanHelper.POST_OMITTED_POSTS.equals(tag)) {
            return mInflater.inflate(R.layout.thread_grid_item_null, parent, false);
        }
        else if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
            return mInflater.inflate(R.layout.thread_grid_item, parent, false);
        } else {
            return mInflater.inflate(R.layout.thread_grid_item_no_image, parent, false);
        }
    }

}
