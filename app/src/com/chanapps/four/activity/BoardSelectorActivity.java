package com.chanapps.four.activity;

import java.util.ArrayList;
import java.util.List;
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
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.adapter.TabsAdapter;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.fragment.WatchlistClearDialogFragment;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.widget.BoardWidgetProvider;

public class BoardSelectorActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity
{
    public static final String TAG = "BoardSelectorActivity";
    public static final boolean DEBUG = false;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    private boolean showNSFWBoards = false;
    public List<ChanBoard.Type> activeBoardTypes = new ArrayList<ChanBoard.Type>();
    public ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
    public Menu menu;

    public static void startActivity(Activity from, ChanBoard.Type boardType) {
        Intent intent = new Intent(from, BoardSelectorActivity.class);
        intent.putExtra(ChanHelper.BOARD_TYPE, boardType != null ? boardType.toString() : ChanBoard.Type.JAPANESE_CULTURE.toString());
        intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar loader

        NetworkProfileManager.instance().activityChange(this);
        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(this); // always check since state may have changed
        updateWidgets();
        scheduleGlobalAlarm();

        Intent intent = getIntent();
        if (!intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false)) {
        	if (DEBUG) Log.i(TAG, "Starting dispatch");
            DispatcherHelper.dispatchIfNecessaryFromPrefsState(this);
        }
        if (intent.hasExtra(ChanHelper.BOARD_TYPE)) {
            selectedBoardType = ChanBoard.Type.valueOf(intent.getStringExtra(ChanHelper.BOARD_TYPE));
            SharedPreferences.Editor ed = ensurePrefs().edit();
            ed.putString(ChanHelper.BOARD_TYPE, selectedBoardType.toString());
            ed.commit();
        }

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
    }

    protected void scheduleGlobalAlarm() { // will reschedule if not already scheduled
        Intent intent = new Intent(this, GlobalAlarmReceiver.class);
        intent.setAction(GlobalAlarmReceiver.GLOBAL_ALARM_RECEIVER_SCHEDULE_ACTION);
        startService(intent);
        if (DEBUG) Log.i(TAG, "Scheduled global alarm");
    }

    protected void updateWidgets() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BoardWidgetProvider.updateAll(getApplicationContext());
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final ActionBar bar = getActionBar();
        bar.setTitle(getString(R.string.application_name));
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
    }

    public void ensureTabsAdapter() {
        showNSFWBoards = ensurePrefs().getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);

        if (mTabsAdapter == null) { // create the board tabs
            activeBoardTypes.clear();
            mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
            for (ChanBoard.Type type : ChanBoard.Type.values()) {
            if (showNSFWBoards || !ChanBoard.isNSFWBoardType(type)) {
                    addTab(type, -1);
                }
            }
            return;
        }

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

    public BoardGroupFragment getWatchlistFragment() {
        int pos = getPositionOfTab(ChanBoard.Type.WATCHLIST);
        if (pos >= 0)
            return (BoardGroupFragment)mTabsAdapter.getItem(pos);
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
        if (menu != null)
            ChanBoard.resetActionBarSpinner(menu);
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
        if (DEBUG) Log.i(TAG, "setTabToSelectedType selectedBoardType: " + selectedBoardType);
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
        restoreInstanceState();
        NetworkProfileManager.instance().activityChange(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
        saveInstanceState();
    }

    private void saveSelectedBoardType() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_TYPE, selectedBoardType.toString());
        editor.commit();
        if (DEBUG) Log.i(TAG, "Saved selected board type to " + selectedBoardType);
    }

    public void saveInstanceState() {
        saveSelectedBoardType();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (DEBUG) Log.i(TAG, "Activity-level onCreateOptionsMenu called selectedBoardType="+selectedBoardType);
        int menuId = ChanBoard.showNSFW(this) ? R.menu.board_selector_menu_adult : R.menu.board_selector_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        this.menu = menu;
        //BoardGroupFragment fragment = getSelectedFragment();
        //if (fragment != null)
        //    fragment.onPrepareOptionsMenu(menu, this, selectedBoardType);
        String boardCode = selectedBoardType.equals(ChanBoard.Type.WATCHLIST) ? ChanBoard.WATCH_BOARD_CODE : "";
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

}
