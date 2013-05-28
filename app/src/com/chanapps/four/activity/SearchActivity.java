package com.chanapps.four.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/1/13
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchActivity extends Activity {

    private static final String TAG = SearchActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Log.e(TAG, "Invalid intent received:" + intent.getAction());
            finish();
            return;
        }
        final String query = intent.getStringExtra(SearchManager.QUERY);
        if (DEBUG) Log.i(TAG, "Received query: " + query);
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity == null || activity.getChanActivityId() == null) {
            Log.e(TAG, "Null chan activity or activity id, exiting search");
            finish();
            return;
        }
        activity.closeSearch();
        String boardCode = activity.getChanActivityId().boardCode;
        if (boardCode == null || boardCode.isEmpty()) {
            Log.e(TAG, "No boardCode supplied with activity id, exiting search");
            finish();
            return;
        }
        long threadNo = activity.getChanActivityId().threadNo;
        if (threadNo > 0)
            ThreadActivity.startActivity(this, boardCode, threadNo, query);
        else
            BoardActivity.startActivity(this, boardCode, query);
        finish();
    }
}
