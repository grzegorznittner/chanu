package com.chanapps.four.component;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.widget.Toast;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThreadStat;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.service.NetworkProfileManager;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/9/13
* Time: 10:59 AM
* To change this template use File | Settings | File Templates.
*/
public class ThreadImageOnClickListener implements View.OnClickListener {

    long postId = 0;
    String boardCode = "";
    long resto = 0;
    long threadNo = 0;
    int w = 0;
    int h = 0;
    int position = 0;

    public ThreadImageOnClickListener(Cursor cursor) {
        postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        threadNo = resto != 0 ? resto : postId;
        w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        position = cursor.getPosition();
    }

    @Override
    public void onClick(View v) {
        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity != null && activity instanceof Activity) {
            ChanHelper.simulateClickAnim(v.getContext(), v);
            incrementCounterAndAddToWatchlistIfActive(v);
            GalleryViewActivity.startActivity((Activity)activity, boardCode, threadNo, postId, position);
        }
    }

    public void incrementCounterAndAddToWatchlistIfActive(View v) {
        NetworkProfileManager.instance().getUserStatistics().threadUse(boardCode, threadNo);
        String key = boardCode + "/" + threadNo;
        ChanThreadStat stat = NetworkProfileManager.instance().getUserStatistics().boardThreadStats.get(key);
        if (stat != null
                && stat.usage >= ThreadActivity.WATCHLIST_ACTIVITY_THRESHOLD
                && !ChanWatchlist.isThreadWatched(v.getContext(), boardCode, threadNo)) {
            int stringId = ChanWatchlist.watchThread(v.getContext(), boardCode, threadNo);
            if (stringId == R.string.thread_added_to_watchlist)
                Toast.makeText(v.getContext(), R.string.thread_added_to_watchlist_activity_based, Toast.LENGTH_SHORT).show();
        }
    }

}
