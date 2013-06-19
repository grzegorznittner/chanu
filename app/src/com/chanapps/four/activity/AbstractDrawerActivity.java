package com.chanapps.four.activity;

import android.app.ActionBar;
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
import com.chanapps.four.data.BoardType;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.NetworkProfileManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class
        AbstractDrawerActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity,
        ListView.OnItemClickListener,
        ActionBar.OnNavigationListener
{
    protected static final String TAG = AbstractDrawerActivity.class.getSimpleName();
    protected static final boolean DEBUG = true;
    protected static final String BOARD_CODE_PATTERN = "/([^/]*)/.*";
    protected static final String STATE_SELECTED_NAVIGATION_ITEM = "selectedNavigationItem";

    protected String boardCode;

    protected boolean mShowNSFW = false;

    protected int mDrawerArrayId;
    protected String[] mDrawerArray;
    protected ListView mDrawerList;
    protected DrawerLayout mDrawerLayout;
    protected ArrayAdapter<String> mDrawerAdapter;
    protected ActionBarDrawerToggle mDrawerToggle;

    protected ActionBar actionBar;
    protected int mSpinnerArrayId;
    protected String[] mSpinnerArray;
    protected ListView mSpinnerList;
    protected DrawerLayout mSpinnerLayout;
    protected ArrayAdapter<String> mSpinnerAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (DEBUG) Log.v(TAG, "onCreate");
        NetworkProfileManager.instance().ensureInitialized(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar
        setContentView(R.layout.drawer_activity_layout);
        mShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        createActionBar();
        createDrawer();
        createViews(bundle);
    }

    protected void createActionBar() {
        actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        setSpinnerAdapter();
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

    protected void setSpinnerAdapter() {
        mSpinnerArrayId = mShowNSFW
                ? R.array.long_board_array
                : R.array.long_board_array_worksafe;
        mSpinnerArray = getResources().getStringArray(mSpinnerArrayId);
        mSpinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item, android.R.id.text1, mSpinnerArray);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
    }

    protected void setDrawerAdapter() {
        mDrawerArrayId = mShowNSFW
                ? R.array.long_drawer_array
                : R.array.long_drawer_array_worksafe;
        mDrawerArray = getResources().getStringArray(mDrawerArrayId);
        mDrawerAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerArray);
        mDrawerList.setAdapter(mDrawerAdapter);
    }

    protected void createDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(this);
        setDrawerAdapter();

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
        boolean newShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        if (newShowNSFW != mShowNSFW) {
            mShowNSFW = newShowNSFW;
            setSpinnerAdapter();
            setDrawerAdapter();
        }
    }

    protected void selectDrawerItem() {
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDrawerLayout.closeDrawer(mDrawerList);
        String boardAsMenu = (String) parent.getItemAtPosition(position);
        if (DEBUG) Log.i(TAG, "onItemClick boardAsMenu=" + boardAsMenu);
        handleSelectItem(boardAsMenu);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        String item = mSpinnerAdapter.getItem(position);
        return handleSelectItem(item);
    }

    protected boolean handleSelectItem(String boardAsMenu) {
        if (isSelfBoard(boardAsMenu)) {
            if (DEBUG) Log.i(TAG, "self board, returning");
            return false;
        }
        BoardType boardType = BoardType.valueOfDrawerString(this, boardAsMenu);
        if (boardType != null) {
            String boardTypeCode = boardType.boardCode();
            if (boardTypeCode.equals(boardCode)) {
                if (DEBUG) Log.i(TAG, "matched existing board code, exiting");
                return false;
            }
            if (DEBUG) Log.i(TAG, "matched board type /" + boardTypeCode + "/, starting");
            Intent intent = BoardActivity.createIntent(this, boardTypeCode, "");
            startActivity(intent);
            return true;
        }
        Pattern p = Pattern.compile(BOARD_CODE_PATTERN);
        Matcher m = p.matcher(boardAsMenu);
        if (!m.matches()) {
            if (DEBUG) Log.i(TAG, "matched nothing, bailing");
            return false;
        }
        String boardCodeForJump = m.group(1);
        if (boardCodeForJump == null || boardCodeForJump.isEmpty() || isSelfBoard(boardCodeForJump)) {
            if (DEBUG) Log.i(TAG, "null match, bailing");
            return false;
        }
        if (boardCodeForJump.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "matched same board code, no jump done");
            return false;
        }
        Intent intent = BoardActivity.createIntent(this, boardCodeForJump, "");
        if (DEBUG) Log.i(TAG, "matched board /" + boardCodeForJump + "/, starting");
        startActivity(intent);
        return true;
    }
    /*
    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        if (bundle.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            int pos = bundle.getInt(STATE_SELECTED_NAVIGATION_ITEM);
            actionBar.setSelectedNavigationItem(pos);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        int pos = actionBar.getSelectedNavigationIndex();
        bundle.putInt(STATE_SELECTED_NAVIGATION_ITEM, pos);
    }
    */

    @Override
    protected void onResume() {
        super.onResume();
        selectActionBarNavigationItem();
        selectDrawerItem();
    }

    protected void selectActionBarNavigationItem() {
        int pos = 0;
        for (int i = 0; i < mSpinnerAdapter.getCount(); i++) {
            String boardText = mSpinnerAdapter.getItem(i);
            BoardType type = BoardType.valueOfDrawerString(this, boardText);
            if (type != null && type.boardCode().equals(boardCode)) {
                pos = i;
                break;
            }
            else if (boardText.matches("/" + boardCode + "/.*")) {
                pos = i;
                break;
            }
        }
        actionBar.setSelectedNavigationItem(pos);
    }

}
