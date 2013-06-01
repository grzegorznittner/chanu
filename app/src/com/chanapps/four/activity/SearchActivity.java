package com.chanapps.four.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
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

    protected static final String TAG = SearchActivity.class.getSimpleName();
    protected static final boolean DEBUG = true;

    protected String query;

    public static void createSearchView(final Activity activity, MenuItem searchMenuItem) {
        SearchManager searchManager = (SearchManager)activity.getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (DEBUG) android.util.Log.i(TAG, "SearchView.onQueryTextSubmit");
                Intent intent = new Intent(activity, SearchActivity.class);
                intent.putExtra(SearchManager.QUERY, query);
                intent.setAction(Intent.ACTION_SEARCH);
                activity.startActivity(intent);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (DEBUG) android.util.Log.i(TAG, "SearchView.onQueryTextChange");
                return false;
            }
        });
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null)
            searchPlate.setBackgroundResource(R.drawable.textfield_search_view_holo_dark);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.i(TAG, "onCreate query=" + getIntent().getStringExtra(SearchManager.QUERY));
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "onNewIntent");
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        query = intent.getStringExtra(SearchManager.QUERY);
        finish();
        if (DEBUG) Log.i(TAG, "handleIntent action=" + intent.getAction() + " q=" + query);
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Log.e(TAG, "handleIntent invalid action");
            return;
        }
        if (DEBUG) Log.i(TAG, "handleIntent q=" + query);
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity == null || activity.getChanActivityId() == null) {
            Log.e(TAG, "Null chan activity or activity id, exiting search");
            return;
        }
        if (DEBUG) Log.i(TAG, "handleIntent closing search");
        activity.closeSearch();
        doSearch();
        return;
    }

    protected void doSearch() {
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        String boardCode = activity.getChanActivityId().boardCode;
        if (boardCode == null || boardCode.isEmpty()) {
            Log.e(TAG, "No boardCode supplied with activity id, exiting search");
            return;
        }

        long threadNo = activity.getChanActivityId().threadNo;
        if (threadNo <= 0) {
            if (DEBUG) Log.i(TAG, "handleIntent start search /" + boardCode + "/" + " q=" + query);
            BoardActivity.startActivity(this, boardCode, query);
            return;
        }

        if (DEBUG) Log.i(TAG, "handleIntent start search /" + boardCode + "/" + threadNo + " q=" + query);
        ThreadActivity.startActivity(this, boardCode, threadNo, query);
    }
}

