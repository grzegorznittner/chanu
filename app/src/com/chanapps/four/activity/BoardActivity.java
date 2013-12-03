package com.chanapps.four.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardGridSmallCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.component.TutorialOverlay.Page;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardGridViewer;
import com.chanapps.four.viewer.ViewType;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class BoardActivity extends AbstractDrawerActivity implements ChanIdentifiedActivity
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = false;
    public static final String UPDATE_BOARD_ACTION = "updateBoardAction";

    protected static final int DRAWABLE_ALPHA_LIGHT = 0xc2;
    protected static final int DRAWABLE_ALPHA_DARK = 0xee;

    protected static Typeface titleTypeface;
    protected static final String TITLE_FONT = "fonts/Edmondsans-Bold.otf";

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected TextView emptyText;
    protected AbsListView absListView;
    protected int columnWidth;
    protected int columnHeight;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected Menu menu;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected ViewType viewType = ViewType.AS_GRID;
    protected int firstVisiblePosition = -1;
    protected int firstVisiblePositionOffset = -1;
    protected View boardTitleBar;
    protected View boardSearchResultsBar;
    protected int gridViewOptions;
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected int checkedPos = -1;
    protected BoardSortType boardSortType;

    public static void startDefaultActivity(Context from) {
        startActivity(from, ChanBoard.defaultBoardCode(from), "");
    }

    public static void startActivity(Context from, String boardCode, String query) {
        if (from.getClass() == BoardSelectorActivity.class && ChanBoard.isTopBoard(boardCode)) {
            ((BoardSelectorActivity)from).switchBoard(boardCode, query);
        }
        else {
            Intent intent = createIntent(from, boardCode, query);
            from.startActivity(intent);
        }
    }

    public static Intent createIntent(Context context, String boardCode, String query) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.ALL_BOARDS_BOARD_CODE : boardCode;
        Class activityClass = ChanBoard.isTopBoard(boardCode)
                ? BoardSelectorActivity.class
                : BoardActivity.class;
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(ChanBoard.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanBoard.PAGE, 0);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    public void addToFavorites(final Context context, final Handler handler, final String boardCode) {
        if (DEBUG) Log.i(TAG, "addToFavorites /" + boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanBoard.makeFavoritesThread(context, boardCode);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't add board /" + boardCode + "/ to favorites");
                        msgId = R.string.board_not_added_to_favorites;
                    }
                    else {
                        ChanFileStorage.addFavoriteBoard(context, thread);
                        refreshFavorites(context);
                        //setFavoritesMenuAsync();
                        msgId = R.string.board_added_to_favorites;
                        if (DEBUG) Log.i(TAG, "Added /" + boardCode + "/ to favorites");
                    }
                }
                catch (IOException e) {
                    msgId = R.string.board_not_added_to_favorites;
                    Log.e(TAG, "Exception adding /" + boardCode + "/ to favorites", e);
                }
                final int stringId = msgId;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }).start();
    }

    public static void addToFavoritesNoMenuUpdate(final Context context, final Handler handler, final String boardCode) {
        if (DEBUG) Log.i(TAG, "addToFavorites /" + boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanBoard.makeFavoritesThread(context, boardCode);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't add board /" + boardCode + "/ to favorites");
                        msgId = R.string.board_not_added_to_favorites;
                    }
                    else {
                        ChanFileStorage.addFavoriteBoard(context, thread);
                        refreshFavorites(context);
                        msgId = R.string.board_added_to_favorites;
                        if (DEBUG) Log.i(TAG, "Added /" + boardCode + "/ to favorites");
                    }
                }
                catch (IOException e) {
                    msgId = R.string.board_not_added_to_favorites;
                    Log.e(TAG, "Exception adding /" + boardCode + "/ to favorites", e);
                }
                final int stringId = msgId;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }).start();
    }

    public void removeFromFavorites(final Context context, final Handler handler, final String boardCode) {
        if (DEBUG) Log.i(TAG, "removeFromFavorites /" + boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanBoard.makeFavoritesThread(context, boardCode);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't remove board /" + boardCode + "/ from favorites");
                        msgId = R.string.favorites_not_deleted_board;
                    }
                    else {
                        ChanFileStorage.deleteFavoritesBoard(context, thread);
                        refreshFavorites(context);
                        //setFavoritesMenuAsync();
                        msgId = R.string.dialog_deleted_from_watchlist;
                        if (DEBUG) Log.i(TAG, "Removed /" + boardCode + "/ from favorites");
                    }
                }
                catch (IOException e) {
                    msgId = R.string.favorites_not_deleted_board;
                    Log.e(TAG, "Exception deleting /" + boardCode + "/ from favorites", e);
                }
                final int stringId = msgId;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }).start();
    }

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        if (boardAsMenu == null || boardAsMenu.isEmpty())
            return false;
        BoardType boardType = BoardType.valueOfDrawerString(this, boardAsMenu);
        if (boardType != null && boardType.boardCode().equals(boardCode))
            return true;
        if (boardAsMenu.matches("/" + boardCode + "/.*")
                && (query == null || query.isEmpty()))
            return true;
        return false;
    }

    @Override
    protected void createViews(Bundle bundle) {
        if (DEBUG) Log.i(TAG, "createViews /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (boardCode == null || boardCode.isEmpty())
            setBoardCodeToDefault();
        if (DEBUG) Log.i(TAG, "createViews /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        //setupStaticBoards();
        initGridViewOptions();
        initBoardSortTypeOptions();
        createAbsListView();
        setupBoardTitle();
        IntentFilter intentFilter = new IntentFilter(UPDATE_BOARD_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, intentFilter);
    }

    protected void initGridViewOptions() {
        if (ChanBoard.isVirtualBoard(boardCode)
                && !ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)) {
            gridViewOptions |= BoardGridViewer.SMALL_GRID;
        }
        else { // check for user pref
            boolean useCatalog = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(SettingsActivity.PREF_USE_CATALOG, false);
            if (useCatalog)
                gridViewOptions |= BoardGridViewer.SMALL_GRID;
            else
                gridViewOptions &= ~BoardGridViewer.SMALL_GRID;
        }
    }

    protected void initBoardSortTypeOptions() {
        boardSortType = BoardSortType.loadFromPrefs(this);
    }

    protected void setUseCatalogPref(boolean useCatalog) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(SettingsActivity.PREF_USE_CATALOG, useCatalog)
                .commit();
    }

    protected void setBoardCodeToDefault() {
        boardCode = ChanBoard.defaultBoardCode(this);
        if (DEBUG) Log.i(TAG, "defaulted board code to /" + boardCode + "/");
    }

    protected void setupBoardTitle() {
        boardTitleBar = findViewById(R.id.board_title_bar);
        if (DEBUG) Log.i(TAG, "createViews /" + boardCode + "/ found boardTitleBar=" + boardTitleBar);
        boardSearchResultsBar = findViewById(R.id.board_search_results_bar);
        if (ChanBoard.isVirtualBoard(boardCode))
            displayBoardTitle();
        else
            hideBoardTitle();
    }

    protected PullToRefreshAttacher.OnRefreshListener pullToRefreshListener
            = new PullToRefreshAttacher.OnRefreshListener() {
        @Override
        public void onRefreshStarted(View view) {
            if (DEBUG) Log.i(TAG, "pullToRefreshListener.onRefreshStarted()");
            onRefresh();
        }
    };

    public static void refreshAllBoards(Context context) {
        updateBoard(context, ChanBoard.ALL_BOARDS_BOARD_CODE);
    }

    public static void refreshWatchlist(Context context) {
        updateBoard(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static void refreshFavorites(Context context) {
        updateBoard(context, ChanBoard.FAVORITES_BOARD_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putString(SearchManager.QUERY, query);
        /*
        int pos = absListView == null ? -1 : absListView.getFirstVisiblePosition();
        View view = absListView == null ? null : absListView.getChildAt(0);
        int offset = view == null ? 0 : view.getTop();
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION, pos);
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION_OFFSET, offset);
        */
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/ q=" + query);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        if (bundle == null) {
            if (DEBUG) Log.i(TAG, "onRestoreInstanceState null bundle, ignoring");
            return;
        }
        if (!bundle.containsKey(ChanBoard.BOARD_CODE)) {
            if (DEBUG) Log.i(TAG, "onRestoreInstanceState bundle doesn't have board code, ignoring");
            return;
        }
        if (bundle.getString(ChanBoard.BOARD_CODE) == null
                || bundle.getString(ChanBoard.BOARD_CODE).isEmpty()) {
            if (DEBUG) Log.i(TAG, "onRestoreInstanceState null or missing board code, ignoring");
            return;
        }
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        query = bundle.getString(SearchManager.QUERY);
        boardSortType = BoardSortType.loadFromPrefs(this);
//firstVisiblePosition = bundle.getInt(FIRST_VISIBLE_POSITION);
        //firstVisiblePositionOffset = bundle.getInt(FIRST_VISIBLE_POSITION_OFFSET);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/ q=" + query);
    }

    public void setFromIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "setFromIntent intent=" + intent);
        Uri data = intent.getData();
        if (data == null) {
            boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            query = intent.getStringExtra(SearchManager.QUERY);
            firstVisiblePosition = -1;
            firstVisiblePositionOffset = -1;
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from intent");
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                query = "";
                firstVisiblePosition = -1;
                firstVisiblePositionOffset = -1;
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                boardCode = ChanBoard.POPULAR_BOARD_CODE;
                query = "";
                firstVisiblePosition = -1;
                firstVisiblePositionOffset = -1;
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/ q=" + query);
    }

    protected void forceGridViewOptions() {
        if (ChanBoard.isVirtualBoard(boardCode) && !(ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)))
            gridViewOptions |= BoardGridViewer.SMALL_GRID; // force meta boards to small
    }

    protected void createAbsListView() {
        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        forceGridViewOptions();
        int layoutId;
        if ((gridViewOptions & BoardGridViewer.SMALL_GRID) > 0)
            layoutId = R.layout.board_grid_layout_small;
        else if (query != null && !query.isEmpty())
            layoutId = R.layout.board_grid_layout_search;
        else if (ChanBoard.isVirtualBoard(boardCode))
            layoutId = R.layout.board_grid_layout;
        else
            layoutId = R.layout.board_grid_layout_no_title;
        layout = getLayoutInflater().inflate(layoutId, null);
        contentFrame.addView(layout);
        //int numColumns = (gridViewOptions & BoardGridViewer.SMALL_GRID) > 0
        //        ? R.integer.BoardGridViewSmall_numColumns
        //        : R.integer.BoardGridViewSmall_numColumns;
//                : R.integer.BoardGridView_numColumns;
        columnWidth = ChanGridSizer.getCalculatedWidth(getResources().getDisplayMetrics(),
                getResources().getInteger(R.integer.BoardGridViewSmall_numColumns),
                getResources().getDimensionPixelSize(R.dimen.BoardGridView_spacing));
        columnHeight = 2 * columnWidth;
        adapter = (gridViewOptions & BoardGridViewer.SMALL_GRID) > 0
                ? new BoardGridSmallCursorAdapter(this, viewBinder)
                : new BoardGridCursorAdapter(this, viewBinder);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                boolean abbrev = getResources().getBoolean(R.bool.BoardGridView_abbrev);
                String search = constraint == null ? "" : constraint.toString();
                BoardCursorLoader filteredCursorLoader =
                        new BoardCursorLoader(getApplicationContext(), boardCode, search, abbrev, true, boardSortType);
                Cursor filteredCursor = filteredCursorLoader.loadInBackground();
                return filteredCursor;
            }
        });
        absListView = (GridView)findViewById(R.id.board_grid_view);
        absListView.setAdapter(adapter);
        absListView.setSelector(android.R.color.transparent);
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        if ((gridViewOptions & BoardGridViewer.SMALL_GRID) > 0) {
            mPullToRefreshAttacher = null; // doesn't work well with grids
        }
        else if (ChanBoard.isPopularBoard(boardCode)
                || ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)
                || !ChanBoard.isVirtualBoard(boardCode)) {
            //try {
            PullToRefreshAttacher.Options options = new PullToRefreshAttacher.Options();
            options.refreshScrollDistance = 0.1f;
            mPullToRefreshAttacher = new PullToRefreshAttacher(this, options);
            mPullToRefreshAttacher.setRefreshableView(absListView, pullToRefreshListener);
            //}
            //catch (Error e) {
            //    Log.e(TAG, "createAbsListView() error creating pull to refresh attacher", e);
            //    mPullToRefreshAttacher = null;
            //}
        }
        else {
            mPullToRefreshAttacher = null;
        }
        emptyText = (TextView)findViewById(R.id.board_grid_empty_text);
    }

    protected class LoggingPauseOnScrollListener extends PauseOnScrollListener {
        public LoggingPauseOnScrollListener(ImageLoader imageLoader, boolean pauseOnScroll, boolean pauseOnFling) {
            super(imageLoader, pauseOnScroll, pauseOnFling, null);
        }
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (handler == null)
            handler = new Handler();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        forceGridViewOptions();
        startLoaderAsync();
        AnalyticsComponent.onStart(this);
    }

    protected void startLoaderAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanBoard board = null;
                try {
                    board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
                }
                catch (Exception e) {
                    Log.e(TAG, "startLoaderAsync() exception loading board", e);
                }
                if (board == null) {
                    Log.e(TAG, "startLoaderAsync() couldn't load board /" + boardCode + "/");
                    return;
                }
                updateThreads(board);
                final ChanBoard finalBoard = board;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            startLoader(finalBoard);
                        }
                    });
            }
        }).start();
    }

    protected void startLoader(final ChanBoard board) {
        NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
        if (board.isVirtualBoard() && !board.isPopularBoard()) { // always ready, start loading
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ non-popular virtual board, loading immediately");
            if (adapter == null || adapter.getCount() == 0) {
                if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ adapter empty, initializing loader");
                getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
            }
        }
        else if (board.hasData() && board.isCurrent()) {
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ board has current data, loading immediately");
            if (adapter == null || adapter.getCount() == 0) {
                if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ adapter empty, initializing loader");
                getSupportLoaderManager().restartLoader(0, null, loaderCallbacks); // data is ready, load it
            }
        }
        else if (board.hasData() &&
                (health == NetworkProfile.Health.NO_CONNECTION
                //        || health == NetworkProfile.Health.BAD
                //        || health == NetworkProfile.Health.VERY_SLOW
                //        || health == NetworkProfile.Health.SLOW
                ))
        {
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ board has old data but connection " + health + ", loading immediately");
            if (adapter == null || adapter.getCount() == 0) {
                if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ adapter empty, initializing loader");
                getSupportLoaderManager().restartLoader(0, null, loaderCallbacks); // data is ready, load it
            }
        }
        else if (health == NetworkProfile.Health.NO_CONNECTION) {
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ no board data and connection is down");
            Toast.makeText(getApplicationContext(), R.string.board_no_connection_load, Toast.LENGTH_SHORT).show();
            if (emptyText != null) {
                emptyText.setText(R.string.board_no_connection_load);
                emptyText.setVisibility(View.VISIBLE);
            }
            setProgress(false);
        }
        else {
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ non-current board data, manual refreshing");
            onRefresh();
        }
    }

    @Override
	protected void onResume() {
		super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        if (handler == null)
            handler = new Handler();
        //invalidateOptionsMenu();
        activityChangeAsync();
    }

    protected void updateThreads(ChanBoard board) { // WARNING don't call on UI thread
        if (board.shouldSwapThreads())
            board.swapLoadedThreads();
        //handleUpdatedThreads(board);
        /*
        if (handler != null)
        handler.post(new Runnable() {
            @Override
            public void run() {
                if ((adapter == null || adapter.getCount() == 0)
                        && board.hasData()
                        && board.isCurrent())
                    getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
            }
        });
        */
    }

    protected void activityChangeAsync() {
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "onResume() activityChange to /" + boardCode + "/");
                    NetworkProfileManager.instance().activityChange(activity);
                }
            }
        }).start();
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        handler = null;
    }

    @Override
    protected void onStop () {
    	super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        getLoaderManager().destroyLoader(0);
        closeSearch();
    	handler = null;
        AnalyticsComponent.onStop(this);
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        if (cursorLoader != null)
            getLoaderManager().destroyLoader(0);
		handler = null;
        IntentFilter intentFilter = new IntentFilter(UPDATE_BOARD_ACTION);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
	}

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            OnClickListener overflow = ChanBoard.META_BOARD_CODE.equals(boardCode) ? null : overflowListener;
            return BoardGridViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight,
                    overlayListener, overflow, gridViewOptions, null);
        }
    };

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ q=" + query + " id=" + id);
            setProgress(true);
            boolean abbrev = getResources().getBoolean(R.bool.BoardGridView_abbrev);
            cursorLoader = new BoardCursorLoader(getApplicationContext(), boardCode, "", abbrev, true, boardSortType);
            return cursorLoader;
        }
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/ q=" + query + " id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()));
            if (absListView == null)
                createAbsListView();
            adapter.swapCursor(data);

            // retry load if maybe data wasn't there yet
            if (boardCode.equals(ChanBoard.WATCHLIST_BOARD_CODE)
                    || boardCode.equals(ChanBoard.FAVORITES_BOARD_CODE)) {
                if (data == null || data.getCount() < 1)
                    showEmptyText();
                else
                    hideEmptyText();
            }
            else if (query != null && !query.isEmpty()) {
                displaySearchTitle();
                hideEmptyText();
                adapter.getFilter().filter(query);
            }
            else if ((data == null || data.getCount() < 1) && handler != null) {
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                    String msg = String.format(getString(R.string.mobile_profile_health_status),
                            health.toString().toLowerCase().replaceAll("_", " "));
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
                showEmptyText();
            }
            else {
                hideEmptyText();
            }
            setProgress(false);
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ q=" + query + " id=" + loader.getId());
            if (adapter != null)
                adapter.swapCursor(null);
        }
    };

    protected void showEmptyText() {
        if (DEBUG) Log.i(TAG, "showEmptyText /" + boardCode + "/");
        if (emptyText == null)
            return;
        BoardType boardType = BoardType.valueOfBoardCode(boardCode);
        int emptyStringId = (boardType != null) ? boardType.emptyStringId(): R.string.board_empty_default;
        emptyText.setText(emptyStringId);
        emptyText.setVisibility(View.VISIBLE);
    }

    protected void hideEmptyText() {
        if (emptyText == null)
            return;
        emptyText.setVisibility(View.GONE);
    }

    protected void onRefresh() {
        if (!ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)
                && ChanBoard.isVirtualBoard(boardCode)
                && !ChanBoard.isPopularBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "manual refresh skipped for non-popular virtual board /" + boardCode + "/");
            return;
        }
        setProgress(true);
        final ChanIdentifiedActivity activity = this;
        if (DEBUG) Log.i(TAG, "starting manual refresh for /" + boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkProfileManager.instance().manualRefresh(activity);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                onRefresh();
                return true;
            case R.id.scroll_to_bottom_menu:
                int n = adapter.getCount() - 1;
                if (DEBUG) Log.i(TAG, "jumping to item n=" + n);
                absListView.setSelection(n);
                return true;
            case R.id.new_thread_menu:
                ChanBoard board = ChanBoard.getBoardByCode(this, boardCode);
                if (board == null || board.isVirtualBoard())
                    new PickNewThreadBoardDialogFragment(handler)
                            .show(getFragmentManager(), PickNewThreadBoardDialogFragment.TAG);
                else
                    PostReplyActivity.startActivity(this, boardCode, 0, 0, "", "");
                return true;
            case R.id.offline_chan_view_menu:
                GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.global_rules_menu:
                (new StringResourceDialog(this,
                        R.layout.board_rules_dialog,
                        R.string.global_rules_header,
                        R.string.global_rules_detail))
                        .show();
                return true;
            case R.id.offline_board_view_menu:
                GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.web_menu:
                String url = ChanBoard.boardUrl(this, boardCode);
                ActivityDispatcher.launchUrlInBrowser(this, url);
                return true;
            case R.id.clear_watchlist_menu:
                (new WatchlistClearDialogFragment()).show(getFragmentManager(), WatchlistClearDialogFragment.TAG);
                return true;
            case R.id.clear_favorites_menu:
                (new FavoritesClearDialogFragment()).show(getFragmentManager(), FavoritesClearDialogFragment.TAG);
                return true;
            case R.id.board_add_to_favorites_menu:
                board = ChanBoard.getBoardByCode(this, boardCode);
                if (board == null || board.isVirtualBoard())
                    new PickFavoritesBoardDialogFragment()
                            .show(getFragmentManager(), PickFavoritesBoardDialogFragment.TAG);
                else
                    addToFavorites(BoardActivity.this, handler, boardCode);
                return true;
            case R.id.favorites_remove_board_menu:
                removeFromFavorites(BoardActivity.this, handler, boardCode);
                return true;
            case R.id.view_as_grid_menu:
                Cursor c = adapter.getCursor();
                gridViewOptions |= BoardGridViewer.SMALL_GRID;
                setUseCatalogPref(true);
                createAbsListView();
                setupBoardTitle();
                adapter.swapCursor(c);
                return true;
            case R.id.view_as_list_menu:
                c = adapter.getCursor();
                gridViewOptions &= ~BoardGridViewer.SMALL_GRID;
                setUseCatalogPref(false);
                createAbsListView();
                setupBoardTitle();
                adapter.swapCursor(c);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        int boardRulesId = R.string.global_rules_detail;
        try {
            boardRulesId = R.string.class.getField("board_" + boardCode + "_rules").getInt(null);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find rules for board:" + boardCode);
        }
        (new StringResourceDialog(this, R.layout.board_rules_dialog, R.string.board_rules_header, boardRulesId)).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        createSearchView(menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    public void createSearchView(Menu menu) {
        searchMenuItem = menu.findItem(R.id.search_menu);
        if (searchMenuItem != null)
            SearchActivity.createSearchView(this, searchMenuItem);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ChanBoard board = ChanBoard.getBoardByCode(this, boardCode);
        if (board == null) {
            ; // ignore
        }
        else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)) {
            menu.findItem(R.id.clear_watchlist_menu).setVisible(true);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            //menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            //menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible((gridViewOptions & BoardGridViewer.SMALL_GRID) == 0);
            menu.findItem(R.id.view_as_list_menu).setVisible((gridViewOptions & BoardGridViewer.SMALL_GRID) > 0);
        }
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)) {
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(true);
            //menu.findItem(R.id.board_add_to_favorites_menu).setVisible(true);
            //menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(false);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
        }
        else if (board.isPopularBoard()) {
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            //menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            //menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.global_rules_menu).setVisible(true);
            menu.findItem(R.id.web_menu).setVisible(true);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
        }
        else if (board.isVirtualBoard()) {
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            //menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            //menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(false);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.global_rules_menu).setVisible(true);
            menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
        }
        else {
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            //menu.findItem(R.id.board_add_to_favorites_menu).setVisible(true);
            //menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(true);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(true);
            //menu.findItem(R.id.board_rules_menu).setVisible(true);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible((gridViewOptions & BoardGridViewer.SMALL_GRID) == 0);
            menu.findItem(R.id.view_as_list_menu).setVisible((gridViewOptions & BoardGridViewer.SMALL_GRID) > 0);
            //setFavoritesMenuAsync();
        }
        menu.findItem(R.id.purchase_menu).setVisible(!BillingComponent.getInstance(this).hasProkey());

        return super.onPrepareOptionsMenu(menu);
    }

    protected void handleUpdatedThreads(final ChanBoard board) {
        final View refreshLayout = this.findViewById(R.id.board_refresh_bar);
        if (refreshLayout == null)
            return;
        final StringBuffer msg = new StringBuffer();
        if ((board.newThreads > 0)// || board.updatedThreads > 0)
                && (query == null || query.isEmpty())) { // display update button
            msg.append("" + board.newThreads + " new");
            msg.append(" thread");
            if (board.newThreads > 1) { // + board.updatedThreads > 1) {
                msg.append("s");
            }
            msg.append(" available");
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
                        refreshText.setText(msg.toString());
                        ImageButton refreshButton = (ImageButton)refreshLayout.findViewById(R.id.board_refresh_button);
                        refreshButton.setClickable(true);
                        refreshButton.setOnClickListener(boardRefreshListener);
                        ImageButton ignoreButton = (ImageButton)refreshLayout.findViewById(R.id.board_ignore_button);
                        ignoreButton.setClickable(true);
                        ignoreButton.setOnClickListener(boardRefreshListener);

                        refreshLayout.setVisibility(LinearLayout.VISIBLE);
                    }
                });
        } else { // don't display menu
            if (board.defData || board.lastFetched == 0) {
                msg.append("not yet fetched");
            } else if (Math.abs(board.lastFetched - new Date().getTime()) < 60000) {
                msg.append("fetched just now");
            } else {
                msg.append("fetched ").append(DateUtils.getRelativeTimeSpanString(
                        board.lastFetched, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
            }
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
                        if (refreshText != null)
                            refreshText.setText("Board is up to date");
                        refreshLayout.setVisibility(LinearLayout.GONE);
                    }
                });
        }
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode, query);
	}

	@Override
	public Handler getChanHandler() {
        return handler;
	}

    @Override
    public void refresh() {
        refresh(null);
    }

    public void refresh(final String refreshMessage) {
        if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ msg=" + refreshMessage);
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        if (handler != null && (board.newThreads == 0 || board.isVirtualBoard())) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu(); // in case spinner needs to be reset
                    if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ msg=" + refreshMessage + " restarting loader");
                    getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
                    if (refreshMessage != null)
                        Toast.makeText(getApplicationContext(), refreshMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
        else {
            setProgress(false);
        }
    }

    public void backgroundRefresh() {
        if (DEBUG) Log.i(TAG, "backgroundRefresh() /" + boardCode + "/");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) Log.i(TAG, "backgroundRefresh() /" + boardCode + "/ refreshing on UI thread");
                if (getSupportLoaderManager() != null)
                    getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
            }
        });
    }

    @Override
    public void closeSearch() {
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

    protected OnClickListener boardRefreshListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.board_refresh_button) {
                setProgress(true);
                View refreshLayout = BoardActivity.this.findViewById(R.id.board_refresh_bar);
                if (refreshLayout == null)
                    return;
                refreshLayout.setVisibility(LinearLayout.GONE);
                ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
                board.swapLoadedThreads();
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
                        }
                    });
            }
            else if (v.getId() == R.id.board_ignore_button) {
                View refreshLayout = BoardActivity.this.findViewById(R.id.board_refresh_bar);
                if (refreshLayout == null)
                    return;
                refreshLayout.setVisibility(LinearLayout.GONE);
            }
        }
    };

    @Override
    public void setProgress(boolean on) {
        if (DEBUG) Log.i(TAG, "setProgress(" + on + ")");
        if (handler != null)
            setProgressBarIndeterminateVisibility(on);
        if (mPullToRefreshAttacher != null && !on) {
            if (DEBUG) Log.i(TAG, "mPullToRefreshAttacher.setRefreshComplete()");
            mPullToRefreshAttacher.setRefreshComplete();
        }
    }

    @Override
    public boolean onSearchRequested() {
        if (DEBUG) Log.i(TAG, "onSearchRequested /" + boardCode + "/ q=" + query);
        getActionBar().setDisplayUseLogoEnabled(true);
        return super.onSearchRequested();
    }

    protected void displayBoardTitle() {
        if (DEBUG) Log.i(TAG, "displayBoardTitle /" + boardCode + "/");
        String title = "";
        int lightIconId = 0;
        int darkIconId = 0;
        BoardType type = BoardType.valueOfBoardCode(boardCode);
        if (type != null) {
            title = getString(type.displayStringId());
            lightIconId = type.drawableId();
            darkIconId = type.darkDrawableId();
        }
        else {
            String rawTitle = ChanBoard.getName(getApplicationContext(), boardCode);
            title = rawTitle == null ? "/" + boardCode + "/" : rawTitle.toLowerCase();
        }
        displayTitleBar(title, lightIconId, darkIconId);
    }

    protected void displaySearchTitle() {
        if (DEBUG) Log.i(TAG, "displaySearchTitle /" + boardCode + "/ q=" + query);
        displayTitleBar(getString(R.string.search_results_title), R.drawable.search, R.drawable.search_light);
        int resultsId = adapter != null && adapter.getCount() > 0
                ? R.string.board_search_results
                : R.string.board_search_no_results;
        String results = String.format(getString(resultsId), query);
        if (boardSearchResultsBar != null) {
            TextView searchResultsTextView = (TextView)boardSearchResultsBar.findViewById(R.id.board_search_results_text);
            if (searchResultsTextView != null) {
                searchResultsTextView.setText(results);
                boardSearchResultsBar.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void displayTitleBar(String title, int lightIconId, int darkIconId) {
        if (DEBUG) Log.i(TAG, "displayTitleBar /" + boardCode + "/ title=" + title + " boardTitleBar=" + boardTitleBar);
        if (boardTitleBar == null)
            return;

        TextView boardTitle = (TextView)boardTitleBar.findViewById(R.id.board_title_text);
        ImageView boardIcon = (ImageView)boardTitleBar.findViewById(R.id.board_title_icon);
        if (DEBUG) Log.i(TAG, "displayTitleBar /" + boardCode + "/ title=" + title + " boardTitle=" + boardTitle + " boardIcon=" + boardIcon);
        if (boardTitle == null || boardIcon == null)
            return;

        try {
            if (titleTypeface == null)
                titleTypeface = Typeface.createFromAsset(getAssets(), TITLE_FONT);
            boardTitle.setTypeface(titleTypeface);
        }
        catch (Exception e) {
            Log.e(TAG, "displayTitleBar() exception making typeface", e);
        }
        boardTitle.setText(title);

        boolean isDark = ThemeSelector.instance(getApplicationContext()).isDark();
        int drawableId = isDark ? lightIconId : darkIconId;
        int alpha = isDark ? DRAWABLE_ALPHA_DARK : DRAWABLE_ALPHA_LIGHT;
        if (drawableId > 0) {
            boardIcon.setImageResource(drawableId);
            boardIcon.setAlpha(alpha);
        }

        boardTitleBar.setVisibility(View.VISIBLE);
        if (DEBUG) Log.i(TAG, "displayBoardTitle /" + boardCode + "/ title=" + title + " set to visible");
    }

    protected void hideBoardTitle() {
        if (DEBUG) Log.i(TAG, "hideBoardTitle /" + boardCode + "/");
        if (boardTitleBar != null)
            boardTitleBar.setVisibility(View.GONE);
        if (boardSearchResultsBar != null)
            boardSearchResultsBar.setVisibility(View.GONE);
    }

    protected View.OnClickListener overflowListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (absListView == null || v == null)
                return;
            try {
                checkedPos = absListView.getPositionForView(v);
            }
            catch (NullPointerException e) {
                Log.e(TAG, "Exception getting view position v=" + v, e);
                return;
            }
            if (adapter == null)
                return;
            Cursor cursor = adapter.getCursor();
            if (cursor == null)
                return;
            if (!cursor.moveToPosition(checkedPos))
                return;
            final String groupBoardCode = boardCode;
            final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));

            final PopupMenu popup = new PopupMenu(BoardActivity.this, v);
            int menuId;
            if (DEBUG) Log.i(TAG, "overflowListener /" + boardCode + "/ group=/" + groupBoardCode + "/");
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode) || ChanBoard.WATCHLIST_BOARD_CODE.equals(groupBoardCode))
                menuId = R.menu.watchlist_context_menu;
            else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode) || ChanBoard.FAVORITES_BOARD_CODE.equals(groupBoardCode))
                menuId = R.menu.favorites_context_menu;
            else if (ChanBoard.isMetaBoard(boardCode) || ChanBoard.isMetaBoard(groupBoardCode))
                menuId = R.menu.meta_board_context_menu;
            else if ((flags & ChanThread.THREAD_FLAG_HEADER) > 0)
                menuId = R.menu.board_header_context_menu;
            else
                menuId = R.menu.board_context_menu;
            popup.inflate(menuId);

            if (menuId == R.menu.board_context_menu) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        showOverflowMenuAsync(popup, boardCode, threadNo);
                    }
                }).start();
            }
            else if (menuId == R.menu.board_header_context_menu) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        showMetaOverflowMenuAsync(popup, boardCode);
                    }
                }).start();
            }
            else if (menuId == R.menu.meta_board_context_menu) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        showMetaOverflowMenuAsync(popup, boardCode);
                    }
                }).start();
            }
            else {
                popup.setOnMenuItemClickListener(popupListener);
                popup.setOnDismissListener(popupDismissListener);
                popup.show();
            }
        }
    };

    protected void showMetaOverflowMenuAsync(final PopupMenu popup, String boardCode) {
        final ChanBoard favoritesBoard = ChanFileStorage.loadBoardData(BoardActivity.this, ChanBoard.FAVORITES_BOARD_CODE);
        final ChanThread thread = ChanBoard.makeFavoritesThread(BoardActivity.this, boardCode);
        final boolean favorited = ChanFileStorage.isFavoriteBoard(favoritesBoard, thread);
        if (DEBUG) Log.i(TAG, "setMetaOverflowMenuAsync() /" + boardCode + "/ favorited=" + favorited
                + " handler=" + handler + " menu=" + popup.getMenu());
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Menu menu = popup.getMenu();
                    if (menu == null)
                        return;
                    if (menu == null)
                        return;
                    MenuItem item;
                    if ((item = menu.findItem(R.id.board_add_to_favorites_menu)) != null)
                        item.setVisible(!favorited);
                    if ((item = menu.findItem(R.id.favorites_remove_board_menu)) != null)
                        item.setVisible(favorited);
                    popup.setOnMenuItemClickListener(popupListener);
                    popup.setOnDismissListener(popupDismissListener);
                    popup.show();
                }
            });
    }

    protected void showOverflowMenuAsync(final PopupMenu popup, String boardCode, long threadNo) {
        final ChanThread thread = ChanFileStorage.loadThreadData(BoardActivity.this, boardCode, threadNo);
        final boolean watched = ChanFileStorage.isThreadWatched(BoardActivity.this, thread);
        final boolean isHeader = threadNo == 0;
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Menu menu = popup.getMenu();
                    if (menu == null)
                        return;
                    MenuItem item;
                    if ((item = menu.findItem(R.id.board_thread_watch_menu)) != null)
                        item.setVisible(!watched);
                    if ((item = menu.findItem(R.id.board_thread_watch_remove_menu)) != null)
                        item.setVisible(watched);
                    popup.setOnMenuItemClickListener(popupListener);
                    popup.setOnDismissListener(popupDismissListener);
                    popup.show();
                }
            });
    }

    protected PopupMenu.OnDismissListener popupDismissListener = new PopupMenu.OnDismissListener() {
        @Override
        public void onDismiss(PopupMenu menu) {
            checkedPos = -1;
        }
    };

    protected PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int pos = checkedPos;
            checkedPos = -1; // clear selection
            Cursor cursor = adapter.getCursor();
            if (!cursor.moveToPosition(pos)) {
                Toast.makeText(BoardActivity.this, R.string.board_no_threads_selected, Toast.LENGTH_SHORT).show();
                return false;
            }
            String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            switch (item.getItemId()) {
                case R.id.board_thread_watch_menu:
                    ThreadFragment.addToWatchlist(BoardActivity.this, handler, boardCode, threadNo);
                    return true;
                case R.id.board_thread_watch_remove_menu:
                    ThreadFragment.removeFromWatchlist(BoardActivity.this, handler, boardCode, threadNo);
                    return true;
                case R.id.board_thread_gallery_menu:
                    FetchChanDataService.scheduleThreadFetch(BoardActivity.this, boardCode, threadNo, true, false);
                    GalleryViewActivity.startAlbumViewActivity(BoardActivity.this, boardCode, threadNo);
                    return true;
                case R.id.board_add_to_favorites_menu:
                    addToFavorites(BoardActivity.this, handler, boardCode);
                    return true;
                case R.id.board_thread_remove_menu:
                    ThreadFragment.removeFromWatchlist(BoardActivity.this, handler, boardCode, threadNo);
                    /*
                    ChanThread thread = ChanFileStorage.loadThreadData(BoardActivity.this, boardCode, threadNo);
                    if (thread != null) {
                        WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, thread);
                        d.show(getSupportFragmentManager(), WatchlistDeleteDialogFragment.TAG);
                    }
                    else {
                        Toast.makeText(BoardActivity.this, R.string.watch_thread_not_found, Toast.LENGTH_SHORT).show();
                    }
                    */
                    return true;
                case R.id.favorites_remove_board_menu:
                    removeFromFavorites(BoardActivity.this, handler, boardCode);
                    return true;
                case R.id.offline_board_view_menu:
                    GalleryViewActivity.startOfflineAlbumViewActivity(BoardActivity.this, boardCode);
                    return true;
                case R.id.sort_order_menu:
                    (new BoardSortOrderDialogFragment(boardSortType))
                            .setNotifySortOrderListener(new BoardSortOrderDialogFragment.NotifySortOrderListener() {
                                @Override
                                public void onSortOrderChanged(BoardSortType boardSortType) {
                                    if (boardSortType != null) {
                                        BoardActivity.this.boardSortType = boardSortType;
                                        BoardSortType.saveToPrefs(BoardActivity.this, boardSortType);
                                        getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
                                    }
                                }
                            })
                            .show(getSupportFragmentManager(), TAG);
                    return true;
                case R.id.board_rules_menu:
                    displayBoardRules();
                    return true;
                case R.id.web_menu:
                    String url = ChanBoard.boardUrl(BoardActivity.this, boardCode);
                    ActivityDispatcher.launchUrlInBrowser(BoardActivity.this, url);
                    return true;
                default:
                    return false;
            }
        }
    };

    protected OnClickListener overlayListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == null)
                return;
            if (absListView == null)
                return;
            int pos;
            try {
                pos = absListView.getPositionForView(view);
            }
            catch (Exception e) {
                Log.e(TAG, "overlayListener:onClick() unable to determine position, exiting");
                return;
            }
            if (DEBUG) Log.i(TAG, "overlayListener pos=" + pos);
            Cursor cursor = adapter.getCursor();
            if (!cursor.moveToPosition(pos))
                return;
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                String[] clickUrls = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL))
                        .split(ChanThread.AD_DELIMITER);
                String clickUrl = viewType == ViewType.AS_GRID ? clickUrls[0] : clickUrls[1];
                ActivityDispatcher.launchUrlInBrowser(BoardActivity.this, clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), BoardActivity.TAG);
                return;
            }
            else if ((flags & ChanThread.THREAD_FLAG_BUTTON) > 0) {
                PostReplyActivity.startActivity(BoardActivity.this, boardCode, 0, 0, "", "");
                return;
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                startActivity(BoardActivity.this, boardLink, "");
            }
            else {
                String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                long postNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_JUMP_TO_POST_NO));
                ThreadActivity.startActivity(BoardActivity.this, boardLink, threadNo, postNo, "");
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.i(TAG, "onBackPressed() /" + boardCode + "/ q=" + query);
        if (query != null && !query.isEmpty()) {
            finish();
        }
        else {
            navigateUp();
        }
    }

    public void navigateUp() { // either pop off stack, or go up to all boards
        if (DEBUG) Log.i(TAG, "navigateUp() /" + boardCode + "/");
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        String upBoardCode = ChanBoard.defaultBoardCode(this);
        if (task != null
                && task.baseActivity != null
                && task.baseActivity.getClassName().equals(BoardSelectorActivity.class.getName()))
        {
            if (DEBUG) Log.i(TAG, "navigateUp() tasks.size=" + numTasks + " top=" + task.topActivity + " base=" + task.baseActivity);
            if (DEBUG) Log.i(TAG, "navigateUp() using finish instead of intents with me="
                    + getClass().getName() + " base=" + task.baseActivity.getClassName());
            finish();
        }
        else {
            if (DEBUG) Log.i(TAG, "navigateUp() null task or not at top level, creating up intent");
            Intent intent = BoardActivity.createIntent(BoardActivity.this, upBoardCode, "");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DEBUG) Log.i(TAG, "dispatchKeyEvent event=" + event.toString());
        boolean handled = ListViewKeyScroller.dispatchKeyEvent(event, absListView);
        if (handled)
            return true;
        else
            return super.dispatchKeyEvent(event);
    }

    protected BroadcastReceiver onNotice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !UPDATE_BOARD_ACTION.equals(intent.getAction()) || !intent.hasExtra(ChanBoard.BOARD_CODE))
                return;
            String receivedBoardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            if (receivedBoardCode == null)
                return;
            if (receivedBoardCode.equals(ChanBoard.FAVORITES_BOARD_CODE)
                    || receivedBoardCode.equals(ChanBoard.WATCHLIST_BOARD_CODE))
                setAdapters();
            if (!receivedBoardCode.equals(boardCode))
                return;
            if (handler != null)
                refresh();
            else
                backgroundRefresh();
        }
    };

    public static void updateBoard(Context context, String boardCode) {
        Intent intent = new Intent(BoardActivity.UPDATE_BOARD_ACTION);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
