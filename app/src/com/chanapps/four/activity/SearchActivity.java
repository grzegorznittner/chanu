package com.chanapps.four.activity;

import android.app.ActionBar;
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
    protected static final boolean DEBUG = false;

    protected String query;

    public static void createSearchView(final Activity activity, MenuItem searchMenuItem) {
        try {
            SearchManager searchManager = (SearchManager)activity.getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView)searchMenuItem.getActionView();
            if (searchView == null)
                return;
            searchView.setSubmitButtonEnabled(true);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    //if (activity != null)
                    //    activity.disableAutoRefresh();
                    if (DEBUG) Log.i(TAG, "onFocusChange()");
                }
            });
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
        }
        catch (Exception e) {
            Log.e(TAG, "Exception creating search view", e);
        }
        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setIcon(R.drawable.app_icon_actionbar);
        actionBar.setLogo(R.drawable.app_icon_actionbar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.i(TAG, "onCreate query=" + getIntent().getStringExtra(SearchManager.QUERY));
        handleIntent(getIntent());
        getActionBar().setDisplayUseLogoEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "onNewIntent");
        handleIntent(intent);
        getActionBar().setDisplayUseLogoEnabled(true);
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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}

