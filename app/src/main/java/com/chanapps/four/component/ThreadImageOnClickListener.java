package com.chanapps.four.component;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.data.ChanPost;
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
        threadNo = resto > 0 ? resto : postId;
        w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        position = cursor.getPosition();
    }

    @Override
    public void onClick(View v) {
        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity != null && activity instanceof Activity) {
            GalleryViewActivity.startActivity((Activity)activity, boardCode, threadNo, postId);
        }
    }

}
