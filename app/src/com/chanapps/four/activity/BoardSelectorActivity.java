package com.chanapps.four.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.chanapps.four.component.AdComponent;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.ChanBoard;
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

    @Override
    protected void onNewIntent(Intent intent) { // for when we coming from a different class
        if (DEBUG) Log.i(TAG, "onNewIntent begin /" + intent.getStringExtra(ChanBoard.BOARD_CODE)
                + "/ q=" + intent.getStringExtra(SearchManager.QUERY));
        if (!intent.hasExtra(ChanBoard.BOARD_CODE)
                || intent.getStringExtra(ChanBoard.BOARD_CODE) == null
                || intent.getStringExtra(ChanBoard.BOARD_CODE).isEmpty()
                ) {
            if (DEBUG) Log.i(TAG, "onNewIntent empty board code, ignoring intent");
            return;
        }
        setIntent(intent);
        setFromIntent(intent);
        //setupStaticBoards();
        createAbsListView();
        setupBoardTitle();
        mDrawerAdapter.notifyDataSetInvalidated();
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/ q=" + query);
    }

    public void switchBoard(String boardCode, String query) { // for when we are already in this class
        if (DEBUG) Log.i(TAG, "switchBoard begin /" + boardCode + "/ q=" + query);
        this.boardCode = boardCode;
        this.query = query;
        //setupStaticBoards();
        createAbsListView();
        setupBoardTitle();
        startLoaderAsync();
        (new AdComponent(getApplicationContext(), findViewById(R.id.board_grid_advert))).hideOrDisplayAds();
        checkNSFW();
        mDrawerAdapter.notifyDataSetInvalidated();
        if (DEBUG) Log.i(TAG, "switchBoard end /" + boardCode + "/ q=" + query);
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

}
