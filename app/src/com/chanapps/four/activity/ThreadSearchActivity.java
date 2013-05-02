package com.chanapps.four.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/1/13
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadSearchActivity extends Activity {

    private static final String TAG = ThreadSearchActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

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
        if (activity instanceof ThreadActivity)
            ((ThreadActivity)activity).closeSearch();
        String boardCode = activity.getChanActivityId().boardCode;
        long threadNo = activity.getChanActivityId().threadNo;
        if (boardCode == null || boardCode.isEmpty() || threadNo <= 0) {
            Log.e(TAG, "BoardCode and threadNo not supplied with activity id, exiting search");
            finish();
            return;
        }
        ThreadActivity.startActivityForSearch(this, boardCode, threadNo, query);
        finish();
    }
}
