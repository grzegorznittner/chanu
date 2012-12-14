package com.chanapps.four.activity;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.adapter.TabsAdapter;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import com.chanapps.four.data.ChanWatchlist;

public class BoardSelectorActivity extends FragmentActivity {
    public static final String TAG = "BoardSelectorActivity";

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    public ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        if (mTabsAdapter == null) {
            mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
            for (ChanBoard.Type type : ChanBoard.Type.values()) {
                addTab(type);
            }
        }
    }

    private void addTab(ChanBoard.Type type) {
        Bundle bundle = new Bundle();
        bundle.putString(ChanHelper.BOARD_TYPE, type.toString());
        String boardTypeName = ChanBoard.getBoardTypeName(this, type);
        mTabsAdapter.addTab(getActionBar().newTab().setText(boardTypeName),
                BoardGroupFragment.class, bundle);
    }

    private void removeTab(ChanBoard.Type type) {
        mTabsAdapter.removeTab(type);
    }

    private void setTabFromPrefs() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        setTabToSelectedType(false);
    }

    private void setTabToSelectedType(boolean force) {
        Log.i(TAG, "onStart selectedBoardType: " + selectedBoardType);

        int selectedTab = 0;
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
            if (type == selectedBoardType) {
                break;
            }
            selectedTab++;
        }
        getActionBar().setSelectedNavigationItem(selectedTab);
        if (force) {
            int beforeTab = (selectedTab + 1) % ChanBoard.Type.values().length;
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
        setTabFromPrefs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_selector_menu, menu);
        return true;
    }
    /*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.clear_favorites_menu);
        if (menuItem != null) {
            if (selectedBoardType == ChanBoard.Type.FAVORITES) {
                menuItem.setVisible(true);
            }
            else {
                menuItem.setVisible(false);
            }
        }
        return true;
    }
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_favorites_menu:
                Log.d(TAG, "Clearing favorites...");
                ChanBoard.clearFavorites(this);
                selectedBoardType = ChanBoard.Type.FAVORITES;
                setTabToSelectedType(true);
                Log.d(TAG, "Favorites cleared.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BoardSelectorActivity.this, R.string.board_favorites_cleared, Toast.LENGTH_SHORT);
                    }
                });
                return true;
            case R.id.clear_watchlist_menu:
                Log.d(TAG, "Clearing watchlist...");
                ChanWatchlist.clearWatchlist(this);
                selectedBoardType = ChanBoard.Type.WATCHING;
                setTabToSelectedType(true);
                Log.d(TAG, "Watchlist cleared.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BoardSelectorActivity.this, R.string.thread_watchlist_cleared, Toast.LENGTH_SHORT);
                    }
                });
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

}
