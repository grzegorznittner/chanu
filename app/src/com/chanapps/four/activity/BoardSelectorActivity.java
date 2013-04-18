package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import com.chanapps.four.adapter.TabsAdapter;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.BoardSelectorTab;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.service.NetworkProfileManager;

public class BoardSelectorActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity
{
    public static final String TAG = "BoardSelectorActivity";
    public static final boolean DEBUG = false;
    public static final String BOARD_SELECTOR_TAB = "boardSelectorTab";

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    private int menuId;
    public static final BoardSelectorTab DEFAULT_BOARD_SELECTOR_TAB = BoardSelectorTab.BOARDLIST;
    public BoardSelectorTab selectedBoardTab = DEFAULT_BOARD_SELECTOR_TAB;

    public static void startActivity(Activity from, BoardSelectorTab tab) {
        Intent intent = new Intent(from, BoardSelectorActivity.class);
        intent.putExtra(BOARD_SELECTOR_TAB, tab != null ? tab.toString() : DEFAULT_BOARD_SELECTOR_TAB);
        intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar loader

        Intent intent = getIntent();
        if (!intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false)) {
        	if (DEBUG) Log.i(TAG, "Starting dispatch");
            DispatcherHelper.dispatchIfNecessaryFromPrefsState(this);
        }
        if (intent.hasExtra(BOARD_SELECTOR_TAB)) {
            selectedBoardTab = BoardSelectorTab.valueOf(intent.getStringExtra(BOARD_SELECTOR_TAB));
            SharedPreferences.Editor ed = ensurePrefs().edit();
            ed.putString(BOARD_SELECTOR_TAB, selectedBoardTab.toString());
            ed.commit();
        }

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final ActionBar bar = getActionBar();
        bar.setTitle(getString(R.string.application_name) + ": " + getString(R.string.application_tagname));
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
    }

    public TabsAdapter ensureTabsAdapter() {
        if (mTabsAdapter == null) { // create the board tabs
            mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
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

    private int getPositionOfTab(BoardSelectorTab tab) {
        return tab.ordinal(); 
    }

    private SharedPreferences ensurePrefs() {
        if (prefs == null) {
            synchronized (this) {
                if (prefs == null)
                    prefs = PreferenceManager.getDefaultSharedPreferences(this);
            }
        }
        return prefs;
    }

    private void restoreInstanceState() {
        ensureTabsAdapter();
        selectedBoardTab = BoardSelectorTab.valueOf(
                ensurePrefs().getString(BOARD_SELECTOR_TAB,
                        DEFAULT_BOARD_SELECTOR_TAB.toString()));
        if (getSelectedTabIndex() == -1) { // reset if board is no longer visible
            selectedBoardTab = DEFAULT_BOARD_SELECTOR_TAB;
            saveselectedBoardTab();
        }
        setTabToSelectedType(false);
    }

    private int getSelectedTabIndex() {
        return selectedBoardTab.ordinal();
    }

    public void setTabToSelectedType(boolean force) {
        if (DEBUG) Log.i(TAG, "setTabToSelectedType selectedBoardTab: " + selectedBoardTab);
        int selectedTab = getSelectedTabIndex();
        getActionBar().setSelectedNavigationItem(selectedTab);
        if (force) {
            int beforeTab = (selectedTab + 1) % BoardSelectorTab.values().length;
            mViewPager.setCurrentItem(beforeTab, false);
            mViewPager.setCurrentItem(selectedTab, false);
        }
        else if (mViewPager.getCurrentItem() != selectedTab) {
            mViewPager.setCurrentItem(selectedTab, true);
        }
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
        restoreInstanceState();
        NetworkProfileManager.instance().activityChange(this);
        int newMenuId = ChanBoard.showNSFW(this) ? R.menu.board_selector_menu_adult : R.menu.board_selector_menu;
        if (menuId != newMenuId)
            invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
        saveInstanceState();
    }

    private void saveselectedBoardTab() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(BOARD_SELECTOR_TAB, selectedBoardTab.toString());
        editor.commit();
        if (DEBUG) Log.i(TAG, "Saved selected board tab=" + selectedBoardTab);
    }

    public void saveInstanceState() {
        saveselectedBoardTab();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (DEBUG) Log.i(TAG, "Activity-level onCreateOptionsMenu called selectedBoardTab="+selectedBoardTab);
        menuId = ChanBoard.showNSFW(this) ? R.menu.board_selector_menu_adult : R.menu.board_selector_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        String boardCode = selectedBoardTab == BoardSelectorTab.WATCHLIST
                ? ChanBoard.WATCH_BOARD_CODE
                : "";
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case R.id.clean_watchlist_menu:
                WatchlistCleanDialogFragment cleanWatchlistFragment = new WatchlistCleanDialogFragment(getWatchlistFragment());
                cleanWatchlistFragment.show(getSupportFragmentManager(), cleanWatchlistFragment.TAG);
                return true;
             */
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.global_rules_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.board_rules_dialog, R.raw.global_rules_header, R.raw.global_rules_detail);
                rawResourceDialog.show();
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog2 = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_board_selector);
                rawResourceDialog2.show();
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_SELECTOR_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return null;
	}

    @Override
    public void refresh() {}

}
