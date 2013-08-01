package com.chanapps.four.activity;

import android.app.ActionBar;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.BoardType;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.service.NetworkProfileManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class
        AbstractBoardSpinnerActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity
{
    protected static final String TAG = AbstractBoardSpinnerActivity.class.getSimpleName();
    protected static final boolean DEBUG = false;
    protected static final String BOARD_CODE_PATTERN = "/([^/]*)/.*";

    protected String boardCode;
    protected long threadNo = 0;
    protected int themeId;

    protected boolean mShowNSFW = false;

    protected ActionBar actionBar;
    protected int mSpinnerArrayId;
    protected String[] mSpinnerArray;
    protected ArrayAdapter<String> mSpinnerAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (DEBUG) Log.v(TAG, "onCreate");
        NetworkProfileManager.instance().ensureInitialized(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar
        themeId = ThemeSelector.instance(getApplicationContext()).setThemeIfNeeded(this, themeId);
        setContentView(activityLayout());
        mShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        createActionBar();
        createPreViews();
        createViews(bundle);
        IntentFilter intentFilter = new IntentFilter(ThemeSelector.ACTION_THEME_CHANGED);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastThemeReceiver, intentFilter);
    }

    protected BroadcastReceiver broadcastThemeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || intent.getAction() == null
                    || !intent.getAction().equals(ThemeSelector.ACTION_THEME_CHANGED)
                    || !intent.hasExtra(ThemeSelector.EXTRA_THEME_ID))
                return;
            int newThemeId = intent.getIntExtra(ThemeSelector.EXTRA_THEME_ID, ThemeSelector.DEFAULT_THEME);
            if (themeId != newThemeId)
                recreate();
        }
    };

    protected int activityLayout() {
        return R.layout.board_spinner_activity_layout;
    }

    protected void createActionBar() {
        actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        setSpinnerAdapter();
    }

    protected void createPreViews() {}

    abstract protected void createViews(Bundle bundle);

    protected void setAdapters() {
        setSpinnerAdapter();
    }

    protected void setSpinnerAdapter() {
        mSpinnerArrayId = mShowNSFW
                ? R.array.long_board_array
                : R.array.long_board_array_worksafe;
        mSpinnerArray = getResources().getStringArray(mSpinnerArrayId);
        mSpinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item, android.R.id.text1, mSpinnerArray);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, spinnerNavigationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean newShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        if (newShowNSFW != mShowNSFW) {
            mShowNSFW = newShowNSFW;
            setAdapters();
        }
        ThemeSelector.instance(getApplicationContext()).recreateIfNeeded(this, themeId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                ActivityDispatcher.exitApplication(this);
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
    public void setProgress(boolean on) {
        Handler handler = getChanHandler();
        if (handler != null)
            setProgressBarIndeterminateVisibility(on);
    }

    abstract public boolean isSelfBoard(String boardAsMenu);

    protected ActionBar.OnNavigationListener spinnerNavigationListener = new ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            String item = mSpinnerAdapter.getItem(itemPosition);
            return handleSelectItem(item);
        }
    };

    protected boolean handleSelectItem(String boardAsMenu) {
        if (isSelfBoard(boardAsMenu)) {
            if (DEBUG) Log.i(TAG, "self board, returning");
            return false;
        }
        if (getString(R.string.board_select).equals(boardAsMenu))
            return false;
        if (getString(R.string.about_menu).equals(boardAsMenu)) {
            Intent intent = AboutActivity.createIntent(this);
            startActivity(intent);
            return true;
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

    @Override
    protected void onResume() {
        super.onResume();
        selectActionBarNavigationItem();
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
