package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.chanapps.four.adapter.TabsAdapter;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.BoardSelectorTab;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.fragment.WatchlistClearDialogFragment;
import com.chanapps.four.service.NetworkProfileManager;

public class
        BoardSelectorActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity
{
    public static final String TAG = "BoardSelectorActivity";
    public static final boolean DEBUG = false;
    public static final String BOARD_SELECTOR_TAB = "boardSelectorTab";

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private int menuId;
    public static final BoardSelectorTab DEFAULT_BOARD_SELECTOR_TAB = BoardSelectorTab.BOARDLIST;

    public static void startActivity(Activity from, BoardSelectorTab tab) {
        from.startActivity(createIntentForActivity(from, tab));
    }

    public static Intent createIntentForActivity(Context context, BoardSelectorTab tab) {
        Intent intent = new Intent(context, BoardSelectorActivity.class);
        intent.putExtra(BOARD_SELECTOR_TAB, tab != null ? tab.toString() : DEFAULT_BOARD_SELECTOR_TAB);
        intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (DEBUG) Log.v(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar
        /*
        Intent intent = getIntent();
        if (!intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false)) {
        	if (DEBUG) Log.i(TAG, "Starting dispatch");
            DispatcherHelper.dispatchIfNecessaryFromPrefsState(this);
        }
        */
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        mViewPager.setOffscreenPageLimit(0); // keep all three tabs available
        setContentView(mViewPager);
        ensureTabsAdapter();
        if (bundle != null)
            setTabFromBundle(bundle);
        else
            setTabFromIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        final ActionBar bar = getActionBar();
        bar.setTitle(getString(R.string.application_name));
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    public TabsAdapter ensureTabsAdapter() {
        if (mTabsAdapter == null) { // create the board tabs
            mTabsAdapter
                    = new TabsAdapter(getApplicationContext(), getActionBar(), getSupportFragmentManager(), mViewPager);
            for (BoardSelectorTab tab : BoardSelectorTab.values())
                addTab(tab, -1);
        }
        return mTabsAdapter;
    }

    private void addTab(BoardSelectorTab tab, int position) {
        Bundle bundle = new Bundle();
        bundle.putString(BOARD_SELECTOR_TAB, tab.toString());
        String tabTitle = getString(tab.displayStringId());
        mTabsAdapter.addTab(getActionBar().newTab().setText(tabTitle),
                BoardGroupFragment.class, bundle, position);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        setTabFromIntent(intent);
    }

    protected void setTabFromIntent(Intent intent) {
        if (intent.hasExtra(BOARD_SELECTOR_TAB)) {
            String tabVal = getIntent().getStringExtra(BOARD_SELECTOR_TAB);
            BoardSelectorTab tab = BoardSelectorTab.valueOf(tabVal);
            setTab(tab);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        int i = mViewPager.getCurrentItem();
        String tab = BoardSelectorTab.values()[i].toString();
        bundle.putString(BOARD_SELECTOR_TAB, tab);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        setTabFromBundle(bundle);
    }

    protected void setTabFromBundle(Bundle bundle) {
        String tabVal = bundle.getString(BOARD_SELECTOR_TAB);
        BoardSelectorTab tab = BoardSelectorTab.valueOf(tabVal);
        setTab(tab);
    }

    public void setTab(BoardSelectorTab tab) {
        if (mViewPager.getCurrentItem() != tab.ordinal())
            mViewPager.setCurrentItem(tab.ordinal(), false);
    }

    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (DEBUG) Log.i(TAG, "onRestart");
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume");
        NetworkProfileManager.instance().activityChange(this);
        int newMenuId = ChanBoard.showNSFW(this) ? R.menu.board_selector_menu_adult : R.menu.board_selector_menu;
        if (menuId != newMenuId) {
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
    }

    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy");
    }

    protected String getSelectedBoardCode() {
        return getSelectedTab().boardCode();
    }

    protected BoardSelectorTab getSelectedTab() {
        int i = mViewPager.getCurrentItem();
        BoardSelectorTab tab = BoardSelectorTab.values()[i];
        return tab;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menuId = ChanBoard.showNSFW(this) ? R.menu.board_selector_menu_adult : R.menu.board_selector_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        ChanBoard.setupActionBarBoardSpinner(this, menu, getSelectedBoardCode());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                String boardCode = getSelectedBoardCode();
                if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)
                        || ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                        || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                        || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode))
                {
                    if (DEBUG) Log.i(TAG, "Refreshing tab code=" + boardCode);
                    setProgressBarIndeterminateVisibility(true);
                    NetworkProfileManager.instance().manualRefresh(this);
                    return true;
                }
                else {
                    return false;
                }
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.clear_watchlist_menu:
                BoardGroupFragment fragment = (BoardGroupFragment)mTabsAdapter
                        .getFragmentAtPosition(BoardSelectorTab.WATCHLIST.ordinal());
                new WatchlistClearDialogFragment()
                        .show(getFragmentManager(), WatchlistClearDialogFragment.TAG);
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about_menu:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.global_rules_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this,
                        R.layout.board_rules_dialog, R.raw.global_rules_header, R.raw.global_rules_detail);
                rawResourceDialog.show();
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_SELECTOR_ACTIVITY, getSelectedBoardCode());
	}

	@Override
	public Handler getChanHandler() {
        if (DEBUG) Log.i(TAG, "grabbing handler for boardCode=" + getSelectedBoardCode());
        BoardGroupFragment fragment = (BoardGroupFragment)mTabsAdapter.getFragmentAtPosition(mViewPager.getCurrentItem());
        if (DEBUG) Log.i(TAG, "found fragment=" + fragment + " tab=" + fragment.getArguments().getString(BOARD_SELECTOR_TAB) + " handler=" + fragment.getChanHandler());
        return fragment.getChanHandler();
	}

    @Override
    public void refresh() {
        // don't call this, instead you have to call the fragment
    }

    public BoardGroupFragment getFragment(BoardSelectorTab tab) {
        BoardGroupFragment fragment = (BoardGroupFragment)mTabsAdapter.getFragmentAtPosition(tab.ordinal());
        if (DEBUG) Log.i(TAG, "found fragment=" + fragment + " tab=" + fragment.getArguments().getString(BOARD_SELECTOR_TAB) + " handler=" + fragment.getChanHandler());
        return fragment;
    }

    @Override
    public void closeSearch() {}

    @Override
    public void startProgress() {
        Handler handler = getChanHandler();
        if (handler != null)
        setProgressBarIndeterminateVisibility(true);
    }

}
