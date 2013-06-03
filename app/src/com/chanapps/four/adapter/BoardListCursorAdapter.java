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
public class BoardListCursorAdapter extends AbstractBoardCursorAdapter {

    protected BoardListCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    public BoardListCursorAdapter(Context context, ViewBinder viewBinder) {
        this(context,
                R.layout.board_list_item,
                viewBinder,
                new String[] {
                        ChanThread.THREAD_THUMBNAIL_URL,
                        ChanThread.THREAD_TITLE,
                        ChanThread.THREAD_TITLE,
                        ChanThread.THREAD_TITLE,
                        ChanThread.THREAD_SUBJECT,
                        ChanThread.THREAD_HEADLINE,
                        ChanThread.THREAD_TEXT,
                        ChanThread.THREAD_COUNTRY_FLAG_URL,
                        ChanThread.THREAD_FLAGS
                },
                new int[] {
                        R.id.list_item_thread_thumb,
                        R.id.list_item_thread_title,
                        R.id.list_item_thread_title_bar,
                        R.id.list_item_thread_button,
                        R.id.list_item_thread_subject,
                        R.id.list_item_thread_headline,
                        R.id.list_item_thread_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_thread_banner_ad
                });
    }

    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        return mInflater.inflate(R.layout.board_list_item, parent, false);
    }

}
