package com.chanapps.four.activity;

import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.graphics.Typeface;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.component.TutorialOverlay.Page;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.UserStatistics.ChanFeature;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardViewer;
import com.chanapps.four.viewer.ViewType;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class BoardActivity
        extends FragmentActivity
        implements ChanIdentifiedActivity,
        RefreshableActivity,
        OnClickListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = false;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 250;
    private static final int THUMB_WIDTH_PX = 150;
    private static final int THUMB_HEIGHT_PX = 150;

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int scrollOnNextLoaderFinished = -1;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected Menu menu;
    protected SharedPreferences prefs;
    protected long tim;
    protected String boardCode;
    protected String query = "";
    protected int columnWidth = 0;
    protected int columnHeight = 0;
    protected MenuItem searchMenuItem;
    protected ViewType viewType = ViewType.AS_GRID;
    protected Typeface subjectTypeface = null;
    protected int padding4DP = 0;
    protected int padding8DP = 0;

    public static void startActivity(Activity from, String boardCode) {
        from.startActivity(createIntentForActivity(from, boardCode));
    }

    public static void startActivityForSearch(Activity from, String boardCode, String query) {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.SEARCH_BOARD);
        from.startActivity(createIntentForActivity(from, boardCode, query));
    }

    public static Intent createIntentForActivity(Context context, String boardCode) {
        return createIntentForActivity(context, boardCode, "");
    }

    public static Intent createIntentForActivity(Context context, String boardCode, String query) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.DEFAULT_BOARD_CODE : boardCode;
        Intent intent = new Intent(context, BoardActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanHelper.PAGE, 0);
        intent.putExtra(SearchManager.QUERY, query);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, 0);
        intent.putExtra(ChanHelper.FROM_PARENT, true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, 0); // reset it
        editor.commit();
        return intent;
    }

    protected void initImageLoader() {
        imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        resetImageOptions(new ImageSize(THUMB_WIDTH_PX, THUMB_HEIGHT_PX)); // view pager needs micro images
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        query = getIntent().hasExtra(SearchManager.QUERY)
                ? getIntent().getStringExtra(SearchManager.QUERY)
                : "";
        initImageLoader();
        createAbsListView();
        ensureHandler();
        ensureSubjectTypeface();
        initPaddings();
        LoaderManager.enableDebugLogging(true);
    }

    protected void initPaddings() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        padding4DP = ChanGridSizer.dpToPx(metrics, 4);
        padding8DP = ChanGridSizer.dpToPx(metrics, 8);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
    }

    protected void initAdapter() {
        if (absListView instanceof GridView)
            adapter = new BoardGridCursorAdapter(this, this, columnWidth, columnHeight);
        else
            adapter = new BoardListCursorAdapter(this, this);
        absListView.setAdapter(adapter);
    }

    protected int getLayoutId() {
        if (GridView.class.equals(absListViewClass))
            return R.layout.board_grid_layout;
        else
            return R.layout.board_list_layout;
    }

    protected void setAbsListViewClass() { // override to change
        if (hasQuery()) viewType = ViewType.AS_LIST;
        absListViewClass = viewType == ViewType.AS_LIST ? ListView.class : GridView.class;
    }

    protected boolean hasQuery() {
        String searchQuery = getIntent() == null ? null : getIntent().getStringExtra(SearchManager.QUERY);
        return searchQuery != null && !searchQuery.isEmpty();
    }

    protected void resetImageOptions(ImageSize imageSize) {
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc()
                .cacheInMemory()
                .resetViewBeforeLoading()
                .build();
    }

    protected void initAbsListView() {
        if (GridView.class.equals(absListViewClass)) {
            absListView = (GridView)findViewById(R.id.board_grid_view);
            sizeGridToDisplay();
            resetImageOptions(new ImageSize(columnWidth, columnHeight));
        }
        else {
            absListView = (ListView)findViewById(R.id.board_list_view);
        }
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        layout = View.inflate(getApplicationContext(), getLayoutId(), null);
        setContentView(layout);
        initAbsListView();
        initAdapter();
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(false);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null && ChanHelper.onUIThread()) {
        	handler = new LoaderHandler();
        }
        return handler;
    }

    @Override
    protected void onStart() {
        super.onStart();
        ensureHandler();
		if (DEBUG) Log.v(TAG, "onStart query=" + query);
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume query=" + query);
        ensureHandler();
        restoreInstanceState();
		NetworkProfileManager.instance().activityChange(this);
		getSupportLoaderManager().restartLoader(0, null, this);
        new TutorialOverlay(layout, tutorialPage());
	}

    protected Page tutorialPage() {
        return Page.BOARD;
    }

    protected String getLastPositionName() {
        return ChanHelper.LAST_BOARD_POSITION;
    }

    protected void scrollToLastPosition() {
        String intentExtra = getLastPositionName();
        int lastPosition = getIntent().getIntExtra(intentExtra, -1);
        if (lastPosition == -1) {
            lastPosition = ensurePrefs().getInt(intentExtra, -1);
        }
        if (lastPosition == -1) {
            lastPosition = 0;
        }
        if (lastPosition <= absListView.getCount()) { // we can scroll now
            absListView.smoothScrollToPosition(lastPosition);
            absListView.smoothScrollToPosition(0);
            scrollOnNextLoaderFinished = -1;
        }
        else { // scroll next time when list is loaded
            scrollOnNextLoaderFinished = lastPosition;
        }
        if (DEBUG) Log.v(TAG, "Scrolling to:" + lastPosition);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.v(TAG, "onPause query=" + query);
        saveInstanceState();
        handler = null;
    }

    protected SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs;
    }

    protected void loadFromIntentOrPrefs() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, ignoring");
            }
        }
        else if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from board code intent");
        }
        else {
            boardCode = ensurePrefs().getString(ChanHelper.BOARD_CODE, ChanBoard.DEFAULT_BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from prefs or default");
        }
        query = intent.hasExtra(SearchManager.QUERY)
                ? intent.getStringExtra(SearchManager.QUERY)
                : ensurePrefs().getString(SearchManager.QUERY, "");
        // backup in case we are missing stuff
        if (boardCode == null || boardCode.isEmpty()) {
            Log.e(TAG, "Empty board code, redirecting to board selector");
            Intent selectorIntent = new Intent(this, BoardSelectorActivity.class);
            selectorIntent.putExtra(ChanHelper.BOARD_TYPE, BoardType.JAPANESE_CULTURE.toString());
            selectorIntent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
            selectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(selectorIntent);
            finish();
        }
    }

    protected void restoreInstanceState() {
        if (DEBUG) Log.i(TAG, "Restoring instance state... query=" + query);
        loadFromIntentOrPrefs();
        handleUpdatedThreads();
        setActionBarTitle();
        //scrollToLastPosition();
        invalidateOptionsMenu(); // for correct spinner display
    }

    protected void saveInstanceState() {
        if (DEBUG) Log.i(TAG, "Saving instance state...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, 0);
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, absListView.getFirstVisiblePosition());
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	if (DEBUG) Log.v(TAG, "onStop query=" + query);
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		if (DEBUG) Log.v(TAG, "onDestroy query=" + query);
		getLoaderManager().destroyLoader(0);
		handler = null;
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return BoardViewer.setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions,
                boardCode, viewType, subjectTypeface, padding4DP);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.d(TAG, ">>>>>>>>>>> onCreateLoader boardCode=" + boardCode);
        setProgressBarIndeterminateVisibility(true);
        cursorLoader = new BoardCursorLoader(this, boardCode, query);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished count=" + (data == null ? 0 : data.getCount()));
		adapter.swapCursor(data);
        if (DEBUG) Log.v(TAG, "listview count=" + absListView.getCount());
        if (absListView != null) {
            if (scrollOnNextLoaderFinished > -1) {
                absListView.setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = -1;
            }
        }

        // retry load if maybe data wasn't there yet
        if ((data == null || data.getCount() < 1) && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                stopProgressBarIfLoadersDone();
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
            else {
                handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
            }
        }
        else {
            handleUpdatedThreads(); // see if we need to update
            setActionBarTitle(); // to reflect updated time
            stopProgressBarIfLoadersDone();
        }
    }

    protected void stopProgressBarIfLoadersDone() {
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        ChanHelper.simulateClickAnim(this, view);
        if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
            String[] clickUrls = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL))
                    .split(ChanThread.AD_DELIMITER);
            String clickUrl = viewType == ViewType.AS_GRID ? clickUrls[0] : clickUrls[1];
            ChanHelper.launchUrlInBrowser(this, clickUrl);
        }
        else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
            String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            startActivity(this, boardLink);
        }
        else {
            String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            ThreadActivity.startActivity(this, boardLink, threadNo);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                intent.putExtra(ChanHelper.BOARD_TYPE, ChanBoard.getBoardByCode(this, boardCode).boardType.toString());
                intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
                //if (!NavUtils.shouldUpRecreateTask(this, intent)) {
                    NavUtils.navigateUpTo(this, intent);
                //}
                return true;
            case R.id.refresh_menu:
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.toggle_view_type_menu:
            	NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_LIST_VIEW);
                viewType = viewType == ViewType.AS_GRID ? ViewType.AS_LIST : ViewType.AS_GRID;
                invalidateOptionsMenu();
                createAbsListView();
                getSupportLoaderManager().restartLoader(0, null, this);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, 0);
                replyIntent.putExtra(ChanPost.POST_NO, 0);
                replyIntent.putExtra(ChanHelper.TIM, 0);
                replyIntent.putExtra(ChanHelper.TEXT, "");
                startActivity(replyIntent);
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_RULES);
        int boardRulesId = R.raw.global_rules_detail;
        try {
            boardRulesId = R.raw.class.getField("board_" + boardCode + "_rules").getInt(null);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find rules for board:" + boardCode);
        }
        RawResourceDialog rawResourceDialog
                = new RawResourceDialog(this, R.layout.board_rules_dialog, R.raw.board_rules_header, boardRulesId);
        rawResourceDialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuId = ChanBoard.showNSFW(this) ? R.menu.board_menu_adult : R.menu.board_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        setupSearch(menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.toggle_view_type_menu);
        if (item != null) {
            item.setIcon(viewType == ViewType.AS_GRID ? R.drawable.collections_view_as_list : R.drawable.collections_view_as_grid);
            item.setTitle(viewType == ViewType.AS_GRID ? R.string.view_as_list_menu : R.string.view_as_grid_menu);
            item.setVisible(!hasQuery()); // force to list view when has query
        }
        return true;
    }

    protected void setupSearch(Menu menu) {
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.search_menu);
        SearchView searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }

    public void setActionBarTitle() {
        final ActionBar a = getActionBar();
        if (a == null)
            return;
        if (DEBUG) Log.i(TAG, "about to load board data for action bar board=" + boardCode);
        ChanBoard board = loadBoard();
        String title = (board == null ? "Board" : board.name) + " /" + boardCode + "/";
        a.setTitle(title);
        a.setDisplayShowTitleEnabled(true);
        a.setDisplayHomeAsUpEnabled(true);
    }

    protected ChanBoard loadBoard() {
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        return board;
    }

    protected void handleUpdatedThreads() {
        LinearLayout refreshLayout = (LinearLayout)this.findViewById(R.id.board_refresh_bar);
        if (refreshLayout == null)
            return;
        ChanBoard board = loadBoard();
        StringBuffer msg = new StringBuffer();
        if (board.shouldSwapThreads()) { // auto-update if we have no threads to show, don't display menu
            if (DEBUG) Log.i(TAG, "auto-updating threads since empty");
            board.swapLoadedThreads();
            refreshLayout.setVisibility(LinearLayout.GONE);
        } else if ((board.newThreads > 0 || board.updatedThreads > 0)
                && (query == null || query.isEmpty())) { // display update button
            if (board.newThreads > 0) {
                msg.append("" + board.newThreads + " new");
            }
            if (board.updatedThreads > 0) {
                if (board.newThreads > 0) {
                    msg.append(", ");
                }
                msg.append("" + board.updatedThreads + " updated");
            }
            msg.append(" thread");
            if (board.newThreads + board.updatedThreads > 1) {
                msg.append("s");
            }
            msg.append(" available");

            TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
            refreshText.setText(msg.toString());
            //ImageButton refreshButton = (ImageButton)refreshLayout.findViewById(R.id.board_refresh_button);
            //refreshButton.setClickable(true);
            //refreshButton.setOnClickListener(this);
            ImageButton ignoreButton = (ImageButton)refreshLayout.findViewById(R.id.board_ignore_button);
            ignoreButton.setClickable(true);
            ignoreButton.setOnClickListener(this);

            refreshLayout.setVisibility(LinearLayout.VISIBLE);
        } else { // don't display menu
            if (board.defData || board.lastFetched == 0) {
                msg.append("not yet fetched");
            } else if (Math.abs(board.lastFetched - new Date().getTime()) < 60000) {
                msg.append("fetched just now");
            } else {
                msg.append("fetched ").append(DateUtils.getRelativeTimeSpanString(
                        board.lastFetched, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
            }
            TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
            refreshText.setText("Board is up to date");

            refreshLayout.setVisibility(LinearLayout.GONE);
        }
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode);
	}

	@Override
	public Handler getChanHandler() {
        return handler;
	}

    @Override
    public void refresh() {
        handleUpdatedThreads();
        setActionBarTitle(); // for update time
        invalidateOptionsMenu(); // in case spinner needs to be reset
        //if (absListView == null || absListView.getCount() < 1)
        //    createAbsListView();
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        if (board.newThreads == 0 && board.updatedThreads == 0) {
	        if (handler != null) {
	        	handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
	        }
        }
    }

    @Override
    public void closeSearch() {
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

    protected Typeface ensureSubjectTypeface() {
        if (subjectTypeface == null)
            subjectTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Condensed.ttf");
        return subjectTypeface;
    }

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.board_ignore_button) {
	        LinearLayout refreshLayout = (LinearLayout)this.findViewById(R.id.board_refresh_bar);
	        refreshLayout.setVisibility(LinearLayout.GONE);
		}
	}

    protected class LoaderHandler extends Handler {
        public LoaderHandler() {}
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        if (DEBUG) Log.i(TAG, ">>>>>>>>>>> restart message received restarting loader");
                        getSupportLoaderManager().restartLoader(1, null, BoardActivity.this);
                        break;

                    default:
                        if (DEBUG) Log.i(TAG, ">>>>>>>>>>> restart message received restarting loader");
                        getSupportLoaderManager().restartLoader(0, null, BoardActivity.this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }
}
