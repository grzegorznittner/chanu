package com.chanapps.four.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanThread;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridCursorAdapter extends AbstractBoardCursorAdapter {

    private int columnWidth;
    private int columnHeight;

    protected BoardGridCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to, int columnWidth, int columnHeight) {
        super(context, layout, viewBinder, from, to);
        this.columnWidth = columnWidth;
        this.columnHeight = columnHeight;
    }

    public BoardGridCursorAdapter(Context context, ViewBinder viewBinder, int columnWidth, int columnHeight) {
        this(context,
                R.layout.board_grid_item,
                viewBinder,
                new String[] {
                        ChanThread.THREAD_SUBJECT,
                        ChanThread.THREAD_HEADLINE,
                        ChanThread.THREAD_COUNTRY_FLAG_URL,
                        ChanThread.THREAD_THUMBNAIL_URL
                },
                new int[] {
                        R.id.grid_item_thread_subject,
                        R.id.grid_item_thread_info,
                        R.id.grid_item_country_flag,
                        R.id.grid_item_thread_thumb
                },
                columnWidth,
                columnHeight
        );
    }

    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        View view = mInflater.inflate(R.layout.board_grid_item, parent, false);
        /*
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && columnWidth > 0) { // && columnHeight > 0) {
            params.width = columnWidth;
            params.height = columnHeight;
        }
        View image = view.findViewById(R.id.grid_item_thread_image_wrap);
        if (image != null) {
            ViewGroup.LayoutParams params2 = image.getLayoutParams();
            if (params2 != null && columnWidth > 0) {
                params.width = columnWidth;
                params.height = columnWidth; // force square images
            }
        }
        View image2 = view.findViewById(R.id.grid_item_thread_thumb);
        if (image2 != null) {
            ViewGroup.LayoutParams params2 = image.getLayoutParams();
            if (params2 != null && columnWidth > 0) {
                params.width = columnWidth;
                params.height = columnWidth; // force square images
            }
        }
        */
        return view;
    }

}
