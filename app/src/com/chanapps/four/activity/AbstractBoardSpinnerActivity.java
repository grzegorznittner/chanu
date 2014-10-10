package com.chanapps.four.activity;

import android.app.ActionBar;
import android.content.*;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;
import com.chanapps.four.component.SendFeedback;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.BoardType;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.viewer.BoardViewer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class
        AbstractBoardSpinnerActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity,
        ThemeSelector.ThemeActivity
{
    protected static final String TAG = AbstractBoardSpinnerActivity.class.getSimpleName();
    protected static final boolean DEBUG = false;
    protected static final boolean DEVELOPER_MODE = false;

    protected static final String THREAD_PATTERN = "/([a-z0-9]+)/([0-9]+).*";
    protected static final String BOARD_PATTERN = "/([a-z0-9]+)/.*";
    protected static final Pattern threadPattern = Pattern.compile(THREAD_PATTERN);
    protected static final Pattern boardPattern = Pattern.compile(BOARD_PATTERN);

    protected String boardCode;
    protected long threadNo = 0;
    protected int themeId;
    protected ThemeSelector.ThemeReceiver broadcastThemeReceiver;

    protected boolean mShowNSFW = false;
    protected boolean mIgnoreMode = false;

    protected ActionBar actionBar;
    protected String[] mSpinnerArray;
    protected ArrayAdapter<String> mSpinnerAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        if (DEBUG) Log.v(TAG, "onCreate");
        if (DEVELOPER_MODE) {
            if (DEBUG) Log.i(TAG, "onCreate enabling developer mode");
            // only enable in development for UI-thread / mem leak testing
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    //.detectDiskReads()
                    //.detectDiskWrites()
                    //.detectNetwork()   // or .detectAll() for all detectable problems
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    //.detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());
            if (DEBUG) Log.i(TAG, "onCreate developer mode enabled");
        }
        super.onCreate(bundle);

        BoardViewer.initStatics(getApplicationContext(), ThemeSelector.instance(getApplicationContext()).isDark());

        NetworkProfileManager.instance().ensureInitialized(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // for spinning action bar
        broadcastThemeReceiver = new ThemeSelector.ThemeReceiver(this);
        broadcastThemeReceiver.register();
        setContentView(activityLayout());
        mShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        createActionBar();
        createPreViews();
        createViews(bundle);
        if (DEBUG) Log.v(TAG, "onCreate complete");
    }

    @Override
    public int getThemeId() {
        return themeId;
    }

    @Override
    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    protected int activityLayout() {
        return R.layout.board_spinner_activity_layout;
    }

    protected void createActionBar() {
        actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        if (DEBUG) Log.i(TAG, "createActionBar()");
    }

    protected void createPreViews() {}

    abstract protected void createViews(Bundle bundle);

    protected void setAdapters() {
        if (DEBUG) Log.i(TAG, "setSpinnerAdapter() begin this=" + this);
        initSpinnerArray();
        mSpinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item, android.R.id.text1, mSpinnerArray);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mIgnoreMode = true;
        //selectActionBarNavigationItem();
        //bindSpinnerListener();
        //mIgnoreMode = false;
        //if (DEBUG) Log.i(TAG, "setSpinnerAdapter() before bind listener");
        //if (DEBUG) Log.i(TAG, "setSpinnerAdapter() after bind listener");
        if (DEBUG) Log.i(TAG, "setSpinnerAdapter() end");
    }

    protected boolean allAdaptersSet() {
        return mSpinnerAdapter != null;
    }

    protected void initSpinnerArray() {
        List<ChanBoard> boards = ChanBoard.getNewThreadBoardsRespectingNSFW(this);
        String[] boardsArray = new String[boards.size() + 1];
        int i = 0;
        boardsArray[i++] = getString(R.string.board_select);
        for (ChanBoard board : boards)
            boardsArray[i++] = " /" + board.link + "/ " + board.name;
        mSpinnerArray = boardsArray;
    }

    protected void bindSpinnerListener() {
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, spinnerNavigationListener);
    }

    protected void unbindSpinnerListener() {
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNSFW();
        mIgnoreMode = true;
        bindSpinnerListener();
        selectActionBarNavigationItem();
    }

    protected void checkNSFW() {
        boolean newShowNSFW = ChanBoard.showNSFW(getApplicationContext());
        if (newShowNSFW != mShowNSFW)
            mShowNSFW = newShowNSFW;
        setAdapters();
        /*
        if (newShowNSFW != mShowNSFW) {
            mShowNSFW = newShowNSFW;
            setAdapters();
        }
        else if (!allAdaptersSet()) {
            setAdapters();
        }
        else {
            setAdapters();
        }
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unbindSpinnerListener();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void closeSearch() {}

    @Override
    public void setProgress(final boolean on) {
        /*
        Handler handler = getChanHandler();
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setProgressBarIndeterminateVisibility(on);
                }
            });
            */
    }

    abstract public boolean isSelfDrawerMenu(String boardAsMenu);

    abstract protected void closeDrawer();

    private ActionBar.OnNavigationListener spinnerNavigationListener = new ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            if (mIgnoreMode) {
                mIgnoreMode = false;
                return true;
            }
            String item = mSpinnerAdapter.getItem(itemPosition);
            if (DEBUG) Log.i(TAG, "spinnerNavigationListener pos=" + itemPosition + " item=[" + item + "] this=" + this + " calling handleSelectItem");
            boolean handle = handleSelectItem(item);
            if (handle) {
                closeDrawer();
            }
            if (DEBUG) Log.i(TAG, "spinnerNavigationListener pos=" + itemPosition + " item=[" + item + "] returned handleSelectItem=" + handle);
            return handle;
        }
    };

    protected boolean handleSelectItem(String boardAsMenu) {
        boardAsMenu = boardAsMenu.trim();
        if (DEBUG) Log.i(TAG, "handleSelectItem boardAsMenu=" + boardAsMenu);
        if (isSelfDrawerMenu(boardAsMenu))
            return false;
        if (matchForMenu(boardAsMenu))
            return true;
        if (matchForBoardType(boardAsMenu))
            return true;
        if (matchForThread(boardAsMenu))
            return true;
        if (matchForBoard(boardAsMenu))
            return true;
        return false;
    }

    protected boolean matchForMenu(String boardAsMenu) {
        if (getString(R.string.board_select).equals(boardAsMenu))
            return false;
        if (getString(R.string.send_feedback_menu).equals(boardAsMenu))
            return SendFeedback.email(this);
        if (getString(R.string.settings_menu).equals(boardAsMenu))
            return SettingsActivity.startActivity(this);
        if (getString(R.string.about_activity).equals(boardAsMenu))
            return AboutActivity.startActivity(this);
        return false;
    }

    protected boolean matchForBoardType(String boardAsMenu) {
        BoardType boardType = BoardType.valueOfDrawerString(this, boardAsMenu);
        if (boardType == null)
            return false;
        String boardTypeCode = boardType.boardCode();
        if (boardTypeCode.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "matched existing board code, exiting");
            return false;
        }
        if (DEBUG) Log.i(TAG, "matched board type /" + boardTypeCode + "/ this=" + this + " switching board");
        switchBoard(boardTypeCode, "");
        return true;
    }

    protected boolean matchForThread(String boardAsMenu) {
        // try to match board
        Matcher m = threadPattern.matcher(boardAsMenu);
        if (!m.matches()) {
            if (DEBUG) Log.i(TAG, "thread matched nothing, bailing");
            return false;
        }
        String boardCodeForJump = m.group(1);
        long threadNoForJump;
        try {
            threadNoForJump = m.group(2) == null ? -1 : Long.valueOf(m.group(2));
        }
        catch (NumberFormatException e) {
            if (DEBUG) Log.i(TAG, "matched non-number thread, bailing");
            return false;
        }
        if (boardCodeForJump == null || boardCodeForJump.isEmpty()) {
            if (DEBUG) Log.i(TAG, "null thread board match, bailing");
            return false;
        }
        if (threadNoForJump <= 0) {
            if (DEBUG) Log.i(TAG, "bad thread match, bailing");
            return false;
        }
        if (boardCodeForJump.equals(boardCode) && threadNoForJump == threadNo) {
            if (DEBUG) Log.i(TAG, "matched same thread, no jump done");
            return false;
        }
        if (DEBUG) Log.i(TAG, "matched thread /" + boardCodeForJump + "/" + threadNoForJump + ", starting");

        if (DEBUG) Log.i(TAG, "starting /" + boardCodeForJump + "/" + threadNoForJump + " from this=" + this);
        ThreadActivity.startActivity(this, boardCodeForJump, threadNoForJump, "");
        //mIgnoreMode = false;
        return true;
    }

    protected boolean matchForBoard(String boardAsMenu) {
        // try to match board
        Matcher m = boardPattern.matcher(boardAsMenu);
        if (!m.matches()) {
            if (DEBUG) Log.i(TAG, "board matched nothing, bailing");
            return false;
        }
        String boardCodeForJump = m.group(1);
        if (boardCodeForJump == null || boardCodeForJump.isEmpty()) {
            if (DEBUG) Log.i(TAG, "null board match, bailing");
            return false;
        }
        if (boardCodeForJump.equals(boardCode) && !(this instanceof ThreadActivity)) {// && threadNo <= 0) { // && threadNo <= 0) {
            if (DEBUG) Log.i(TAG, "matched same board code, no jump done");
            return false;
        }
        /*
        if (!startActivityIfNeeded(intent, -1)) {
            if (DEBUG) Log.i(TAG, "startActivityIfNeeded /" + boardCodeForJump + "/ returned true, finishing current activity");
        }
        else {
            if (DEBUG) Log.i(TAG, "startActivityIfNeeded /" + boardCodeForJump + "/ returned false, switching board activity");
            switchBoard(boardCode, "");
        }
        */
        if (DEBUG) Log.i(TAG, "matched board /" + boardCodeForJump + "/ this=" + this + " switching board");
        switchBoard(boardCodeForJump, "");

        /*
        startActivity(intent);
        if ((this instanceof BoardSelectorActivity) || (this instanceof BoardActivity)) // don't finish single task actv
            return true;
        finish();
        */
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ this=" + this);
        if (DEBUG) Log.i(TAG, "onCreate isTaskRoot()=" + isTaskRoot() + " intent=" + getIntent());
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                    intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastThemeReceiver.unregister();
    }

    protected void selectActionBarNavigationItem() {
        if (DEBUG) Log.i(TAG, "selectActionBarNavigationItem /" + boardCode + "/ begin");
        int pos = -1;
        for (int i = 0; i < mSpinnerAdapter.getCount(); i++) {
            String boardText = mSpinnerAdapter.getItem(i);
            BoardType type = BoardType.valueOfDrawerString(this, boardText);
            if (type != null && type.boardCode().equals(boardCode)) {
                pos = i;
                break;
            }
            else if (boardText.matches(" ?/" + boardCode + "/.*")) {
                pos = i;
                break;
            }
        }
        if (pos >= 0) {
            String boardText = mSpinnerAdapter.getItem(pos);
            if (DEBUG) Log.i(TAG, "selectActionBarNavigationItem /" + boardCode + "/ found pos=" + pos + " text=" + boardText);
        }
        else {
            pos = 0;
            if (DEBUG) Log.i(TAG, "selectActionBarNavigationItem /" + boardCode + "/ not found defaulted pos=" + pos);
        }
        if (actionBar.getSelectedNavigationIndex() != pos) {
            actionBar.setSelectedNavigationItem(pos);
        }
        else {
            mIgnoreMode = false;
        }
        if (DEBUG) Log.i(TAG, "selectActionBarNavigationItem /" + boardCode + "/ set pos=" + pos + " end");
    }

    @Override
    public void switchBoard(String boardCode, String query) {}

    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= 18) {
            if (hasFocus) {
                int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
                int newUiOptions = uiOptions;
                newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                //newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
        }
    }
    */

}
