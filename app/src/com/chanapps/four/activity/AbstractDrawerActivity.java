package com.chanapps.four.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.NetworkProfileManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class
        AbstractDrawerActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity,
        ListView.OnItemClickListener
{
    public static final String TAG = AbstractDrawerActivity.class.getSimpleName();
    public static final boolean DEBUG = false;
    protected static final String BOARD_CODE_PATTERN = "/([^/]*)/.*";

    protected Handler handler;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected int mBoardArrayId;
    protected String[] mBoardArray;
    protected ListView mDrawerList;
    protected DrawerLayout mDrawerLayout;
    protected ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (DEBUG) Log.v(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar
        setContentView(R.layout.drawer_activity_layout);
        createDrawer();
        createViews(bundle);
    }

    abstract protected void createViews(Bundle bundle);

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        mDrawerToggle.onConfigurationChanged(config);
    }

    protected void createDrawer() {
        mBoardArrayId = ChanBoard.showNSFW(getApplicationContext())
                ? R.array.long_board_array
                : R.array.long_board_array_worksafe;
        mBoardArray = getResources().getStringArray(mBoardArrayId);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mBoardArray);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }


    @Override
    protected void onStart() {
        super.onStart();
        handler = new Handler();
        setSelfChecked();
    }

    protected void setSelfChecked() {
        for (int i = 0; i < mDrawerList.getAdapter().getCount(); i++) {
            String s = (String)mDrawerList.getItemAtPosition(i);
            if (isSelfBoard(s)) {
                mDrawerList.setItemChecked(i, true);
                break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop");
        handler = null;
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
        handler = new Handler();
        NetworkProfileManager.instance().activityChange(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
        handler = null;
    }

    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about_menu:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Handler getChanHandler() {
        return handler;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void closeSearch() {}

    @Override
    public void startProgress() {
        Handler handler = getChanHandler();
        if (handler != null)
            setProgressBarIndeterminateVisibility(true);
    }

    abstract public boolean isSelfBoard(String boardAsMenu);

    public boolean isBoardList(String boardAsMenu) {
        return (boardAsMenu.equals(getString(R.string.board_select))
                || boardAsMenu.equals(getString(R.string.board_select_abbrev)));
    }
    public boolean isPopular(String boardAsMenu) {
        return (boardAsMenu.equals(getString(R.string.board_popular))
                || boardAsMenu.equals(getString(R.string.board_popular_abbrev)));
    }
    public boolean isWatchlist(String boardAsMenu) {
        return (boardAsMenu.equals(getString(R.string.board_watch))
                || boardAsMenu.equals(getString(R.string.board_watch_abbrev)));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDrawerLayout.closeDrawer(mDrawerList);
        String boardAsMenu = (String) parent.getItemAtPosition(position);
        if (DEBUG) Log.i(BoardSelectorActivity.TAG, "onItemClick boardAsMenu=" + boardAsMenu);
        if (isSelfBoard(boardAsMenu))
            return;
        if (isBoardList(boardAsMenu)) {
            Intent intent = BoardListActivity.createIntent(this);
            startActivity(intent);
            return;
        }
        if (isPopular(boardAsMenu)) {
            Intent intent = PopularActivity.createIntent(this);
            startActivity(intent);
            return;
        }
        if (isWatchlist(boardAsMenu)) {
            Intent intent = WatchlistActivity.createIntent(this);
            startActivity(intent);
            return;
        }
        Pattern p = Pattern.compile(BOARD_CODE_PATTERN);
        Matcher m = p.matcher(boardAsMenu);
        if (!m.matches())
            return;
        String boardCodeForJump = m.group(1);
        if (boardCodeForJump == null || boardCodeForJump.isEmpty() || isSelfBoard(boardCodeForJump))
            return;
        Intent intent = BoardActivity.createIntentForActivity(this, boardCodeForJump, "");
        startActivity(intent);
    }

}
