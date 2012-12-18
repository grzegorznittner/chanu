package com.chanapps.four.activity;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.chanapps.four.component.DispatcherHelper;
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

public class BoardSelectorActivity extends FragmentActivity {
    public static final String TAG = "BoardSelectorActivity";

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    public ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (!getIntent().getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false))
            DispatcherHelper.dispatchIfNecessaryFromPrefsState(this);
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

    public BoardGroupFragment getFavoritesFragment() {
        return (BoardGroupFragment)mTabsAdapter.getItem(ChanBoard.Type.FAVORITES.ordinal());
    }

    private void removeTab(ChanBoard.Type type) {
        mTabsAdapter.removeTab(type);
    }

    private void restoreInstanceState() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        setTabToSelectedType(false);
    }

    public void setTabToSelectedType(boolean force) {
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
        restoreInstanceState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveInstanceState();
    }

    protected void saveInstanceState() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_TYPE, selectedBoardType.toString());
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
