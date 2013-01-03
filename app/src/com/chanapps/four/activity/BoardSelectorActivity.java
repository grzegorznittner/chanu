package com.chanapps.four.activity;

import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.ExtendedImageDownloader;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.data.SmartCache;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.adapter.TabsAdapter;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import com.chanapps.four.fragment.FavoritesClearDialogFragment;
import com.chanapps.four.fragment.WatchlistCleanDialogFragment;
import com.chanapps.four.fragment.WatchlistClearDialogFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BoardSelectorActivity extends FragmentActivity implements ChanIdentifiedActivity {
    public static final String TAG = "BoardSelectorActivity";

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    private boolean showNSFWBoards = false;
    private boolean useFavoritesBoard = true;
    public List<ChanBoard.Type> activeBoardTypes = new ArrayList<ChanBoard.Type>();
    public ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
    public Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        if (ensurePrefs().getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true))
            (new ChanWatchlist.CleanWatchlistTask(this, null, false)).execute();
        SmartCache.fillCache(this);
        Intent intent = getIntent();
        if (!intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false)) {
            Log.i(TAG, "Starting dispatch");
            DispatcherHelper.dispatchIfNecessaryFromPrefsState(this);
        }
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ActionBar bar = getActionBar();
        bar.setTitle(getString(R.string.app_name));
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
    }

    private void ensureTabsAdapter() {
        showNSFWBoards = ensurePrefs().getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        useFavoritesBoard = prefs.getBoolean(SettingsActivity.PREF_USE_FAVORITES, true);

        if (mTabsAdapter == null) { // create the board tabs
            activeBoardTypes.clear();
            mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
            for (ChanBoard.Type type : ChanBoard.Type.values()) {
                if (type == ChanBoard.Type.FAVORITES) {
                    if (useFavoritesBoard)
                        addTab(type, -1);
                }
                else if (showNSFWBoards || !ChanBoard.isNSFWBoardType(type)) {
                    addTab(type, -1);
                }
            }
            return;
        }

        /* can't get adding / removing 0th tab to work
        if (useFavoritesBoard && getPositionOfTab(ChanBoard.Type.FAVORITES) == -1) { // need to add
            addTab(ChanBoard.Type.FAVORITES, 0);
        }
        else if (!useFavoritesBoard && getPositionOfTab(ChanBoard.Type.FAVORITES) >= 0) { // need to remove
            int pos = getPositionOfTab(ChanBoard.Type.FAVORITES);
            removeTab(ChanBoard.Type.FAVORITES);
        }
        */

        if (showNSFWBoards && getPositionOfTab(ChanBoard.Type.ADULT) == -1) { // need to add
            addTab(ChanBoard.Type.ADULT, -1);
            addTab(ChanBoard.Type.MISC, -1);
        }
        else if (!showNSFWBoards && getPositionOfTab(ChanBoard.Type.ADULT) >= 0) { // need to remove
            removeTab(ChanBoard.Type.ADULT);
            removeTab(ChanBoard.Type.MISC);
            selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
            if (selectedBoardType == ChanBoard.Type.ADULT || selectedBoardType == ChanBoard.Type.MISC) {
                selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
                saveSelectedBoardType();
            }
        }
    }

    private void removeTab(ChanBoard.Type type) {
        mTabsAdapter.removeTab(getPositionOfTab(type));
        activeBoardTypes.remove(type);
    }

    private void addTab(ChanBoard.Type type, int position) {
        Bundle bundle = new Bundle();
        bundle.putString(ChanHelper.BOARD_TYPE, type.toString());
        String boardTypeName = ChanBoard.getBoardTypeName(this, type);
        if (position == -1) {
            activeBoardTypes.add(type);
        }
        else {
            activeBoardTypes.add(position, type);
        }
        mTabsAdapter.addTab(getActionBar().newTab().setText(boardTypeName),
                BoardGroupFragment.class, bundle, position);
    }

    private int getPositionOfTab(ChanBoard.Type type) {
        int position = -1;
        for (int i = 0; i < activeBoardTypes.size(); i++)
            if (activeBoardTypes.get(i) == type) {
                position = i;
                break;
            }
        return position;
    }

    public BoardGroupFragment getFavoritesFragment() {
        int favoritesPos = getPositionOfTab(ChanBoard.Type.FAVORITES);
        if (favoritesPos >= 0)
            return (BoardGroupFragment)mTabsAdapter.getItem(favoritesPos);
        else
            return null;
    }

    public BoardGroupFragment getWatchlistFragment() {
        int favoritesPos = getPositionOfTab(ChanBoard.Type.WATCHLIST);
        if (favoritesPos >= 0)
            return (BoardGroupFragment)mTabsAdapter.getItem(favoritesPos);
        else
            return null;
    }

    private SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs;
    }

    private void restoreInstanceState() {
        ensureTabsAdapter();
        selectedBoardType = ChanBoard.Type.valueOf(ensurePrefs().getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        if (getSelectedTabIndex() == -1) { // reset if board is no longer visible
            selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
            saveSelectedBoardType();
        }
        setTabToSelectedType(false);
    }

    private int getSelectedTabIndex() {
        for (int i = 0; i < activeBoardTypes.size(); i++) {
            if (selectedBoardType == activeBoardTypes.get(i))
                return i;
        }
        return -1;
    }

    private BoardGroupFragment getSelectedFragment() {
        BoardGroupFragment fragment = null;
        int i = getSelectedTabIndex();
        if (i >= 0 && mTabsAdapter != null) {
            fragment = (BoardGroupFragment)mTabsAdapter.getItem(i);
        }
        return fragment;
    }

    public void setTabToSelectedType(boolean force) {
        Log.i(TAG, "setTabToSelectedType selectedBoardType: " + selectedBoardType);
        int selectedTab = getSelectedTabIndex();
        getActionBar().setSelectedNavigationItem(selectedTab);
        if (force) {
            int beforeTab = (selectedTab + 1) % activeBoardTypes.size();
            mViewPager.setCurrentItem(beforeTab, false);
            mViewPager.setCurrentItem(selectedTab, false);
        }
        else if (mViewPager.getCurrentItem() != selectedTab) {
            mViewPager.setCurrentItem(selectedTab, true);
        }
    }

    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreInstanceState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        saveInstanceState();
    }

    private void saveSelectedBoardType() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_TYPE, selectedBoardType.toString());
        editor.commit();
        Log.i(TAG, "Saved selected board type to " + selectedBoardType);
    }

    public void saveInstanceState() {
        saveSelectedBoardType();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Log.i(TAG, "Activity-level onCreateOptionsMenu called selectedBoardType="+selectedBoardType);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_selector_menu, menu);
        this.menu = menu;
        BoardGroupFragment fragment = getSelectedFragment();
        if (fragment != null)
            fragment.onPrepareOptionsMenu(menu, this, selectedBoardType);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_favorites_menu:
                FavoritesClearDialogFragment clearDialogFragment = new FavoritesClearDialogFragment(getFavoritesFragment());
                clearDialogFragment.show(getSupportFragmentManager(), clearDialogFragment.TAG);
                return true;
            case R.id.clean_watchlist_menu:
                WatchlistCleanDialogFragment cleanWatchlistFragment = new WatchlistCleanDialogFragment(getWatchlistFragment());
                cleanWatchlistFragment.show(getSupportFragmentManager(), cleanWatchlistFragment.TAG);
                return true;
            case R.id.clear_watchlist_menu:
                WatchlistClearDialogFragment clearWatchlistFragment = new WatchlistClearDialogFragment(getWatchlistFragment());
                clearWatchlistFragment.show(getSupportFragmentManager(), clearWatchlistFragment.TAG);
                return true;
            case R.id.settings_menu:
                Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_board_selector);
                rawResourceDialog.show();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this, R.raw.legal, R.raw.info);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_SELECTOR_ACTIVITY);
	}

	@Override
	public AsyncTaskLoader<Cursor> getChanCursorLoader() {
		return null;
	}
	
	@Override
	public Handler getChanHandler() {
		return null;
	}
}
