package com.chanapps.four.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 10/7/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSelectorActivity extends BoardActivity implements ChanIdentifiedActivity {

    public static final String TAG = BoardSelectorActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static void startActivity(Context from) {
        startActivity(from, ChanBoard.defaultBoardCode(from), "");
    }

    @Override
    public void switchBoard(String boardCode, String query) {
        if (ChanBoard.isTopBoard(boardCode)) {
            switchBoardInternal(boardCode, query);
        }
        else {
            Intent intent = BoardActivity.createIntent(this, boardCode, query);
            startActivity(intent);
        }
    }

    @Override
    protected void activityChangeAsync() {
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "boardSelector onResume() activityChange to /" + boardCode + "/");
                    NetworkProfileManager.instance().activityChange(activity);
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                new TutorialOverlay(layout, TutorialOverlay.Page.BOARD);
                            }
                        });
                }
            }
        }).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (absListView != null)
            viewPosition = absListView.getFirstVisiblePosition();
        switchBoardInternal(boardCode, "");
        //if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)

        //createAbsListView();
        //if (absListView != null && absListView instanceof GridView) {
        //    ((GridView)absListView).setNumColumns(R.integer.BoardGridViewSmall_numColumns);
        //}
        // the views handle this already
    }

    @Override
    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.BOARD_SELECTOR_ACTIVITY, boardCode, query);
    }

}
