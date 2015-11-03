package com.chanapps.four.activity;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
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
import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.adapter.BoardNarrowCursorAdapter;
import com.chanapps.four.adapter.BoardSmallCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardViewer;
import com.chanapps.four.viewer.ViewType;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class BoardActivity extends AbstractDrawerActivity implements ChanIdentifiedActivity
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = false;

    protected static final String UPDATE_BOARD_ACTION = "updateBoardAction";
    protected static final String UPDATE_ABBREV_ACTION = "updateAbbrevAction";
    protected static final String UPDATE_FAST_SCROLL_ACTION = "updateFastScrollAction";
    protected static final String UPDATE_CATALOG_ACTION = "updateCatalogAction";
    protected static final String UPDATE_HIDE_LAST_REPLIES_ACTION = "updateHideLastRepliesAction";
    protected static final String OPTION_ENABLE = "optionEnable";
    protected static final String BACKGROUND_REFRESH = "backgroundRefresh";

    protected static final int DRAWABLE_ALPHA_LIGHT = 0xc2;
    protected static final int DRAWABLE_ALPHA_DARK = 0xee;
    protected static final int LOADER_ID = 0;

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
    protected View boardTitleBar;
    protected View boardSearchResultsBar;
    protected int gridViewOptions;
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected int checkedPos = -1;
    protected BoardSortType boardSortType;
    protected int viewPosition = -1;
    protected boolean scheduleRecreate = false;
    /*
    public static void startDefaultActivity(Context from) {
        startActivity(from, ChanBoard.defaultBoardCode(from), "");
    }
    */

    public static void startActivity(Context from, ChanActivityId aid) {
        startActivity(from, aid.boardCode, aid.text);
    }

    public static void startActivity(Context from, String boardCode, String query) {
        if (from instanceof ChanIdentifiedActivity) {
            ((ChanIdentifiedActivity)from).switchBoard(boardCode, query);
        }
        else {
            Intent intent = createIntent(from, boardCode, query);
            from.startActivity(intent);
        }
    }

    @Override
    public void switchBoard(String boardCode, String query) { // for when we are already in this class
        if (ChanBoard.isTopBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "board to board selector, finishing board activity");
            Intent intent = BoardActivity.createIntent(this, boardCode, query);
            startActivity(intent);
        }
        else {
            switchBoardInternal(boardCode, query);
        }
    }

    protected void switchBoardInternal(String boardCode, String query) { // for when we are already in this class
        if (DEBUG) Log.i(TAG, "switchBoardInternal begin /" + boardCode + "/ q=" + query);
        this.boardCode = boardCode;
        this.query = query;
        getSupportLoaderManager().destroyLoader(LOADER_ID); // clear out existing list
        //setupStaticBoards();
        loadDrawerArray();
        createAbsListView();
        setupBoardTitle();
        startLoaderAsync();
        checkNSFW();
        if (mDrawerAdapter != null)
            mDrawerAdapter.notifyDataSetInvalidated();

        /* move spinner to right board */
        mIgnoreMode = true;
        selectActionBarNavigationItem();

        if (DEBUG) Log.i(TAG, "switchBoard end /" + boardCode + "/ q=" + query);
    }

    public static Intent createIntent(Context context, String boardCode, String query) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.defaultBoardCode(context) : boardCode;
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

    public static void addToFavorites(final Context context, final Handler handler, final String boardCode) {
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
    public boolean isSelfDrawerMenu(String boardAsMenu) {
        if (boardAsMenu == null || boardAsMenu.isEmpty())
            return false;
        BoardType boardType = BoardType.valueOfDrawerString(this, boardAsMenu);
        if (boardType != null && boardType.boardCode().equals(boardCode))
            return true;
        if (boardAsMenu.matches("/" + boardCode + "/[^0-9].*")
                && (query == null || query.isEmpty()))
            return true;
        return false;
    }

    @Override
    protected void createViews(Bundle bundle) {
        if (DEBUG) Log.i(TAG, "createViews init class=" + this.getClass());
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
        setupReceivers();
    }

    protected void setupReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateBoardReceived, new IntentFilter(UPDATE_BOARD_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateAbbrevReceived, new IntentFilter(UPDATE_ABBREV_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateCatalogReceived, new IntentFilter(UPDATE_CATALOG_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateHideLastRepliesReceived, new IntentFilter(UPDATE_HIDE_LAST_REPLIES_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateFastScrollReceived, new IntentFilter(UPDATE_FAST_SCROLL_ACTION));
    }
    
    protected void teardownReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateBoardReceived);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateAbbrevReceived);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateCatalogReceived);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateHideLastRepliesReceived);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateFastScrollReceived);
    }

    protected void initGridViewOptions() {
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
            setSmallGridEnabled(false);
        else if (ChanBoard.isVirtualBoard(boardCode))
            setSmallGridEnabled(true);
        else
            setSmallGridEnabled(getBoolPref(SettingsActivity.PREF_USE_CATALOG));

        if (ChanBoard.ALL_BOARDS_BOARD_CODE.equals(boardCode)
                || ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode))
            setAbbrevBoardsEnabled(getBoolPref(SettingsActivity.PREF_USE_ABBREV_BOARDS));
        else
            setAbbrevBoardsEnabled(false);

        setHideLastRepliesEnabled(getBoolPref(SettingsActivity.PREF_HIDE_LAST_REPLIES));
    }

    protected void setSmallGridEnabled(boolean enabled) {
        if (enabled)
            gridViewOptions |= BoardViewer.CATALOG_GRID;
        else
            gridViewOptions &= ~BoardViewer.CATALOG_GRID;
    }

    protected void setAbbrevBoardsEnabled(boolean enabled) {
        if (enabled)
            gridViewOptions |= BoardViewer.ABBREV_BOARDS;
        else
            gridViewOptions &= ~BoardViewer.ABBREV_BOARDS;
    }

    protected void setHideLastRepliesEnabled(boolean enabled) {
        if (enabled)
            gridViewOptions |= BoardViewer.HIDE_LAST_REPLIES;
        else
            gridViewOptions &= ~BoardViewer.HIDE_LAST_REPLIES;
    }

    protected boolean getBoolPref(String preference) {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(preference, false);
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
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/ q=" + query);
    }

    public void setFromIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "setFromIntent intent=" + intent);
        Uri data = intent.getData();
        if (data == null) {
            boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            query = intent.getStringExtra(SearchManager.QUERY);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from intent");
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                query = "";
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                boardCode = ChanBoard.POPULAR_BOARD_CODE;
                query = "";
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/ q=" + query);
    }
    /*
    protected void forceGridViewOptions() {
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
            gridViewOptions &= ~BoardViewer.CATALOG_GRID; // force watchlist to full size
        else  if (ChanBoard.isVirtualBoard(boardCode))
            gridViewOptions |= BoardViewer.CATALOG_GRID; // force meta boards to small
    }
    */
    protected void createAbsListView() {
        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        initGridViewOptions();
        //forceGridViewOptions();
        int layoutId;
        if ((gridViewOptions & BoardViewer.CATALOG_GRID) > 0)
            layoutId = R.layout.board_grid_layout_small;
        else if (query != null && !query.isEmpty())
            layoutId = R.layout.board_grid_layout_search;
        else
            layoutId = R.layout.board_grid_layout;
        //else if (ChanBoard.isVirtualBoard(boardCode))
        //    layoutId = R.layout.board_grid_layout;
        //else
        //    layoutId = R.layout.board_grid_layout_no_title;
        layout = getLayoutInflater().inflate(layoutId, null);
        contentFrame.addView(layout);
        //int numColumns = (gridViewOptions & BoardViewer.CATALOG_GRID) > 0
        //        ? R.integer.BoardGridViewSmall_numColumns
        //        : R.integer.BoardGridViewSmall_numColumns;
//                : R.integer.BoardGridView_numColumns;
        columnWidth = ChanGridSizer.getCalculatedWidth(getResources().getDisplayMetrics(),
                getResources().getInteger(R.integer.BoardGridViewSmall_numColumns),
                getResources().getDimensionPixelSize(R.dimen.BoardGridView_spacing));
        columnHeight = 2 * columnWidth;
        if ((gridViewOptions & BoardViewer.CATALOG_GRID) > 0)
            adapter = new BoardSmallCursorAdapter(this, viewBinder);
        else if (getResources().getInteger(R.integer.BoardGridView_numColumns) > 1)
            adapter = new BoardNarrowCursorAdapter(this, viewBinder);
        else
            adapter = new BoardCursorAdapter(this, viewBinder);
        adapter.setGroupBoardCode(boardCode);
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
        absListView = (AbsListView)findViewById(R.id.board_grid_view);
        absListView.setAdapter(adapter);
        absListView.setSelector(android.R.color.transparent);
        //absListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        absListView.setFastScrollEnabled(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, false));
        emptyText = (TextView)findViewById(R.id.board_grid_empty_text);
        bindPullToRefresh();
        bindSwipeToDismiss();
        bindOnItemClick();
        bindPauseOnScrollListener();
    }

    protected void bindPauseOnScrollListener() {
        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        AbsListView.OnScrollListener customListener =
                absListView != null && absListView instanceof EnhancedListView
                ? ((EnhancedListView)absListView).makeScrollListener()
                : null;
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true, customListener));
    }

    protected void bindOnItemClick() {
        absListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                overlayListener.onClick(view);
            }
        });
    }

    protected static final int UNDO_DELAY_MS = 3000;

    protected void bindSwipeToDismiss() {
        // Set the callback that handles dismisses.
        if (DEBUG) Log.i(TAG, "bindSwipeToDismiss absListView=" + absListView);
        if (!(absListView instanceof EnhancedListView))
            return;
        final EnhancedListView mListView = (EnhancedListView)absListView;
        EnhancedListView.OnDismissCallback callback;
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
            callback = swipeDismissWatchedCallback;
        else if (!ChanBoard.isVirtualBoard(boardCode))
            callback = swipeDismissCallback;
        else
            callback = null;
        if (callback == null) {
            mListView.disableSwipeToDismiss();
            return;
        }
        mListView.setDismissCallback(callback);
        mListView.enableSwipeToDismiss();
        mListView.setSwipeDirection(EnhancedListView.SwipeDirection.END);
        mListView.setUndoHideDelay(UNDO_DELAY_MS);
        mListView.setRequireTouchBeforeDismiss(false);
        mListView.discardUndo();
    }

    protected EnhancedListView.OnDismissCallback swipeDismissCallback = new EnhancedListView.OnDismissCallback() {
        @Override
        public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {
            final Cursor c = adapter == null ? null : adapter.getCursor();
            if (c == null || !c.moveToPosition(position))
                return null;
            String board = c.getString(c.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            long no = c.getLong(c.getColumnIndex(ChanThread.THREAD_NO));
            if (no <= 0) // don't allow swiping away of board header
                return null;
            final String uniqueId = ChanThread.uniqueId(board, no, 0);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ChanBlocklist.add(BoardActivity.this, ChanBlocklist.BlockType.THREAD, uniqueId);
                    if (adapter != null && handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                }
            }).start();
            return new EnhancedListView.Undoable() {
                @Override
                public void undo() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ChanBlocklist.remove(BoardActivity.this, ChanBlocklist.BlockType.THREAD, uniqueId);
                            refresh();
                        }
                    }).start();
                }
            };
        }
    };

    protected EnhancedListView.OnDismissCallback swipeDismissWatchedCallback = new EnhancedListView.OnDismissCallback() {
        @Override
        public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {
            final Cursor c = adapter == null ? null : adapter.getCursor();
            if (c == null || !c.moveToPosition(position))
                return null;
            final String board = c.getString(c.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            final long no = c.getLong(c.getColumnIndex(ChanThread.THREAD_NO));
            final String uniqueId = ChanThread.uniqueId(board, no, 0);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ThreadFragment.removeFromWatchlist(BoardActivity.this, handler, board, no);
                    if (c == null || adapter != null)
                        adapter.notifyDataSetChanged();
                    //refresh();
                }
            }).start();
            return new EnhancedListView.Undoable() {
                @Override
                public void undo() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ThreadFragment.addToWatchlist(BoardActivity.this, handler, board, no);
                            refresh();
                        }
                    }).start();
                }
            };
        }
    };

    protected void bindPullToRefresh() {
        if (mPullToRefreshAttacher == null) {
            PullToRefreshAttacher.Options options = new PullToRefreshAttacher.Options();
            mPullToRefreshAttacher = new PullToRefreshAttacher(this, options);
        }
        mPullToRefreshAttacher.setRefreshableView(absListView, pullToRefreshListener);
        //if ((gridViewOptions & BoardViewer.CATALOG_GRID) > 0) {
        //    mPullToRefreshAttacher = null; // doesn't work well with grids
        //}
        //else
        boolean enabled;
        if (ChanBoard.isPopularBoard(boardCode))
            enabled = true;
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode))
            enabled = true;
        else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
            enabled = true;
        else if (!ChanBoard.isVirtualBoard(boardCode))
            enabled = true;
        else
            enabled = false;
        mPullToRefreshAttacher.setEnabled(enabled);
    }

    @Override
    protected void onNewIntent(Intent intent) { // for when we coming from a different class
        if (DEBUG) Log.i(TAG, "onNewIntent begin /" + intent.getStringExtra(ChanBoard.BOARD_CODE)
                + "/ q=" + intent.getStringExtra(SearchManager.QUERY));
        if (!intent.hasExtra(ChanBoard.BOARD_CODE)
                || intent.getStringExtra(ChanBoard.BOARD_CODE) == null
                || intent.getStringExtra(ChanBoard.BOARD_CODE).isEmpty()
                ) {
            if (DEBUG) Log.i(TAG, "onNewIntent empty board code, ignoring intent");
            return;
        }
        setIntent(intent);
        setFromIntent(intent);
        getSupportLoaderManager().destroyLoader(LOADER_ID); // clear out existing list
        loadDrawerArray();
        createAbsListView();
        setupBoardTitle();
        checkNSFW();
        if (mDrawerAdapter != null)
            mDrawerAdapter.notifyDataSetInvalidated();
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/ q=" + query);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (handler == null)
            handler = new Handler();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
    }

    @Override
	protected void onResume() {
		super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        if (handler == null)
            handler = new Handler();
        /*
        int oldGridViewOptions = gridViewOptions;
        initGridViewOptions();
        if (gridViewOptions != oldGridViewOptions) {
            Cursor c = adapter.getCursor();
            createAbsListView();
            setupBoardTitle();
            adapter.swapCursor(c);
        }
        */

        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanBoard board = ChanFileStorage.loadBoardData(BoardActivity.this, boardCode);
                final boolean isCurrent = board != null && board.isCurrent();
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (scheduleRecreate) { // used to support configuration change when coming back to board from another activity
                                scheduleRecreate = false;
                                recreateListViewPreservingPosition();
                            }
                            if (isCurrent && isAlreadyLoaded()) {
                                if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " already loaded");
                            }
                            else {
                                if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " starting loader");
                                startLoaderAsync();
                            }
                            if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " starting activity change");
                            activityChangeAsync();
                            if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query + " complete");
                        }
                    });
            }
        }).start();
    }

    protected void startLoaderAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanBoard board = null;
                try {
                    if (DEBUG) Log.i(TAG, "startLoaderAsync /" + boardCode + "/");
                    board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
                }
                catch (Exception e) {
                    Log.e(TAG, "startLoaderAsync() exception loading board", e);
                }

                final boolean isCurrent = board != null && !board.defData && board.isCurrent();
                if (DEBUG) Log.i(TAG, "startLoaderAsync() /" + boardCode + "/ isCurrent=" + isCurrent);
                //if (board == null) {
                //    Log.e(TAG, "startLoaderAsync() couldn't load board /" + boardCode + "/");
                //}
                if (!isCurrent && !board.isTopBoard()) { // meta boards are always defdata
                    if (DEBUG) Log.i(TAG, "startLoaderAsync() defdata, waiting for load of board /" + boardCode + "/");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onRefresh();
                            }
                        });
                }
                else {
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
            }
        }).start();
    }

    protected void startLoader(final ChanBoard board) {
        if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/");
        NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
        if (board.isVirtualBoard() && !board.isPopularBoard()) { // always ready, start loading
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ non-popular virtual board, loading immediately");
            if (adapter == null || adapter.getCount() == 0) {
                if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ adapter empty, initializing loader");
                getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
            }
        }
        else if (board.hasData() && board.isCurrent()) {
            if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ board has current data, loading immediately");
            if (adapter == null || adapter.getCount() == 0) {
                if (DEBUG) Log.i(TAG, "startLoader /" + boardCode + "/ adapter empty, initializing loader");
                getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // data is ready, load it
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
                getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // data is ready, load it
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

    protected boolean isAlreadyLoaded() {
        if (DEBUG) Log.i(TAG, "isAlreadyLoaded() adapter=" + adapter);
        if (adapter == null)
            return false;
        if (DEBUG) Log.i(TAG, "isAlreadyLoaded() count=" + adapter.getCount());
        if (adapter.getCount() == 0)
            return false;
        Cursor cursor = adapter.getCursor();
        if (DEBUG) Log.i(TAG, "isAlreadyLoaded() cursor=" + cursor);
        if (cursor == null)
            return false;
        if (!cursor.moveToFirst())
            return false;
        String cursorBoardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (DEBUG) Log.i(TAG, "isAlreadyLoaded() cursorBoardCode=" + cursorBoardCode + " boardCode=" + boardCode);
        if (cursorBoardCode == null)
            return false;
        if (cursorBoardCode.isEmpty())
            return false;
        if (!cursorBoardCode.equals(boardCode))
            return false;
        return true;
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
                    getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
            }
        });
        */
    }

    protected void activityChangeAsync() {
        if (DEBUG) Log.i(TAG, "activityChangeAsync() /" + boardCode + "/ starting thread...");
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "onResume() async activityChange to /" + boardCode + "/");
                    NetworkProfileManager.instance().activityChange(activity);
                }
            }
        }).start();
    }

    /*
    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
    }
    */

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
        //getSupportLoaderManager().destroyLoader(LOADER_ID);
        closeSearch();
    	handler = null;
        if (absListView != null && absListView instanceof EnhancedListView)
            ((EnhancedListView)absListView).discardUndo();
        setProgress(false);
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy /" + boardCode + "/ q=" + query + " actual class=" + this.getClass());
        if (cursorLoader != null)
            getSupportLoaderManager().destroyLoader(LOADER_ID);
		handler = null;
        teardownReceivers();
	}

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            //OnClickListener overflow = ChanBoard.META_BOARD_CODE.equals(boardCode) ? null : overflowListener;
            //OnClickListener overflow = overflowListener;
            //return BoardViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight,
            //        overlayListener, overflow, gridViewOptions, null);
           // return BoardViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight,
           //         overlayListener, overflowListener, gridViewOptions, null);
            return BoardViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight,
                    null, overflowListener, gridViewOptions, null);
        }
    };

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ q=" + query + " id=" + id);
            if (args != null && args.getBoolean(BACKGROUND_REFRESH, false)) {
                if (DEBUG) Log.i(TAG, "onCreateLoader background refresh, bypassing progress indicator");
            }
            else if (
                    ChanBoard.ALL_BOARDS_BOARD_CODE.equals(boardCode) ||
                    ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode) ||
                    ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)
            )
            {
                if (DEBUG) Log.i(TAG, "onCreateLoader foreground refresh non-loadable boards, bypassing progress indicator");
                setProgress(false);
            }
            //else if (ChanBoard.boardNeedsRefresh(BoardActivity.this, boardCode, false)) {
            //    setProgress(true);
            //}
            else {
                if (DEBUG) Log.i(TAG, "onCreateLoader foreground refresh, starting progress indicator");
                setProgress(true);
                //setProgress(false);
            }
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

            //adapter.swapCursor(data);
            adapter.changeCursor(data);

            // retry load if maybe data wasn't there yet
            if (boardCode.equals(ChanBoard.WATCHLIST_BOARD_CODE)
                    || boardCode.equals(ChanBoard.FAVORITES_BOARD_CODE)) {
                if (DEBUG) Log.i(TAG, "onLoadFinished showing empty text");
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

            if (viewPosition >= 0) {
                final int pos = viewPosition;
                viewPosition = -1;
                absListView.setSelection(pos);
            }
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ q=" + query + " id=" + loader.getId());
            if (adapter != null)
                adapter.changeCursor(null);
                //adapter.swapCursor(null);
        }
    };

    protected void showEmptyText() {
        if (DEBUG) Log.i(TAG, "showEmptyText /" + boardCode + "/");
        if (emptyText == null)
            return;
        BoardType boardType = BoardType.valueOfBoardCode(boardCode);
        int emptyStringId = (boardType != null) ? boardType.emptyStringId() : R.string.board_empty_default;
        emptyText.setText(emptyStringId);
        emptyText.setVisibility(View.VISIBLE);
    }

    protected void hideEmptyText() {
        if (emptyText == null)
            return;
        emptyText.setVisibility(View.GONE);
    }

    protected void onRefresh() {
        if (!ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)
                && !ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)
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
                //if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode) && handler != null)
                 //   handler.post(new ToastRunnable(BoardActivity.this, R.string.refresh_favorites));
                //else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode) && handler != null)
                //    handler.post(new ToastRunnable(BoardActivity.this, R.string.refresh_watchlist));
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
            case R.id.scroll_to_top_menu:
                int n = 0;
                if (DEBUG) Log.i(TAG, "jumping to item n=" + n);
                if (adapter != null && adapter.getCount() > 0) {
                    absListView.setSelection(n);
                }
                return true;
            case R.id.scroll_to_bottom_menu:
                n = adapter.getCount() - 1;
                if (DEBUG) Log.i(TAG, "jumping to item n=" + n);
                if (adapter != null && adapter.getCount() > 0) {
                    absListView.setSelection(n);
                }
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
                        R.string.global_rules_menu,
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
            case R.id.clean_watchlist_menu:
                (new WatchlistCleanDialogFragment()).show(getFragmentManager(), WatchlistCleanDialogFragment.TAG);
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
                setSmallGridEnabled(true);
                setUseCatalogPref(true);
                createAbsListView();
                setupBoardTitle();
                //adapter.swapCursor(c);
                adapter.changeCursor(c);
                return true;
            case R.id.view_as_list_menu:
                c = adapter.getCursor();
                setSmallGridEnabled(false);
                setUseCatalogPref(false);
                createAbsListView();
                setupBoardTitle();
                //adapter.swapCursor(c);
                adapter.changeCursor(c);
                return true;
            case R.id.use_abbrev_boards_menu:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Boolean pref = prefs.getBoolean(SettingsActivity.PREF_USE_ABBREV_BOARDS, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_USE_ABBREV_BOARDS, pref).apply();
                updateAbbrev(this, pref);
                return true;
            case R.id.hide_last_replies_menu:
                prefs = PreferenceManager.getDefaultSharedPreferences(this);
                pref = prefs.getBoolean(SettingsActivity.PREF_HIDE_LAST_REPLIES, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_HIDE_LAST_REPLIES, pref).apply();
                updateHideLastReplies(this, pref);
                return true;
            case R.id.use_volume_scroll_menu:
                prefs = PreferenceManager.getDefaultSharedPreferences(this);
                pref = prefs.getBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, pref).apply();
                recreate();
                return true;
            case R.id.use_fast_scroll_menu:
                prefs = PreferenceManager.getDefaultSharedPreferences(this);
                pref = prefs.getBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, pref).apply();
                updateFastScroll(this, pref);
                return true;
            case R.id.nsfw_menu:
                new PreferenceDialogs(this).showNSFWDialog();
                return true;
            case R.id.theme_menu:
                new PreferenceDialogs(this).showThemeDialog();
                return true;
            case R.id.sort_order_menu:
                (new BoardSortOrderDialogFragment(boardSortType))
                        .setNotifySortOrderListener(new BoardSortOrderDialogFragment.NotifySortOrderListener() {
                            @Override
                            public void onSortOrderChanged(BoardSortType boardSortType) {
                                if (boardSortType != null) {
                                    BoardActivity.this.boardSortType = boardSortType;
                                    BoardSortType.saveToPrefs(BoardActivity.this, boardSortType);
                                    getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                                }
                            }
                        })
                        .show(getSupportFragmentManager(), TAG);
                return true;
            case R.id.show_hidden_threads_menu:
                /* debug
                Set<String> b = ChanBlocklist.getBlocklist(BoardActivity.this).get(ChanBlocklist.BlockType.THREAD);
                for (String s : b) {
                    Log.e(TAG, "block: " + s);
                }
                */
                final String uid = ChanPost.uniqueId(boardCode, 0, 0);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ChanBlocklist.removeMatching(BoardActivity.this, ChanBlocklist.BlockType.THREAD, uid);
                        refresh();
                    }
                }).start();
                return true;
            case R.id.blocklist_menu:
                List<Pair<String, ChanBlocklist.BlockType>> blocks = ChanBlocklist.getSorted(BoardActivity.this);
                (new BlocklistViewAllDialogFragment(blocks, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                    }
                })).show(getFragmentManager(),TAG);
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
            menu.findItem(R.id.clean_watchlist_menu).setVisible(true);
            menu.findItem(R.id.clear_watchlist_menu).setVisible(true);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            //menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
            menu.findItem(R.id.sort_order_menu).setVisible(false);
            menu.findItem(R.id.show_hidden_threads_menu).setVisible(false);
            menu.findItem(R.id.use_abbrev_boards_menu).setVisible(false);
            menu.findItem(R.id.hide_last_replies_menu).setVisible(false);
            menu.findItem(R.id.nsfw_menu).setVisible(true);
            menu.findItem(R.id.blocklist_menu).setVisible(true);
        }
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)) {
            menu.findItem(R.id.clean_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(true);
            menu.findItem(R.id.board_add_to_favorites_menu).setVisible(true);
            menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            //menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
            menu.findItem(R.id.sort_order_menu).setVisible(false);
            menu.findItem(R.id.show_hidden_threads_menu).setVisible(false);
            menu.findItem(R.id.use_abbrev_boards_menu).setVisible(true);
            menu.findItem(R.id.hide_last_replies_menu).setVisible(false);
            menu.findItem(R.id.nsfw_menu).setVisible(true);
            menu.findItem(R.id.blocklist_menu).setVisible(false);
        }
        else if (board.isPopularBoard()) {
            menu.findItem(R.id.clean_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            //menu.findItem(R.id.web_menu).setVisible(true);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
            menu.findItem(R.id.sort_order_menu).setVisible(false);
            menu.findItem(R.id.show_hidden_threads_menu).setVisible(false);
            menu.findItem(R.id.use_abbrev_boards_menu).setVisible(false);
            menu.findItem(R.id.hide_last_replies_menu).setVisible(false);
            menu.findItem(R.id.nsfw_menu).setVisible(true);
            menu.findItem(R.id.blocklist_menu).setVisible(true);
        }
        else if (board.isVirtualBoard()) {
            menu.findItem(R.id.clean_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            menu.findItem(R.id.board_add_to_favorites_menu).setVisible(false);
            menu.findItem(R.id.favorites_remove_board_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(false);
            menu.findItem(R.id.search_menu).setVisible(false);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            //menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(true);
            //menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible(false);
            menu.findItem(R.id.view_as_list_menu).setVisible(false);
            menu.findItem(R.id.sort_order_menu).setVisible(false);
            menu.findItem(R.id.show_hidden_threads_menu).setVisible(false);
            menu.findItem(R.id.use_abbrev_boards_menu).setVisible(true);
            menu.findItem(R.id.hide_last_replies_menu).setVisible(false);
            menu.findItem(R.id.nsfw_menu).setVisible(true);
            menu.findItem(R.id.blocklist_menu).setVisible(false);
        }
        else {
            menu.findItem(R.id.clean_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_watchlist_menu).setVisible(false);
            menu.findItem(R.id.clear_favorites_menu).setVisible(false);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(true);
            //menu.findItem(R.id.offline_board_view_menu).setVisible(true);
            //menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.board_rules_menu).setVisible(true);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
            //menu.findItem(R.id.web_menu).setVisible(false);
            menu.findItem(R.id.view_as_grid_menu).setVisible((gridViewOptions & BoardViewer.CATALOG_GRID) == 0);
            menu.findItem(R.id.view_as_list_menu).setVisible((gridViewOptions & BoardViewer.CATALOG_GRID) > 0);
            menu.findItem(R.id.sort_order_menu).setVisible(true);
            menu.findItem(R.id.use_abbrev_boards_menu).setVisible(false);
            menu.findItem(R.id.hide_last_replies_menu).setVisible(true);
            menu.findItem(R.id.nsfw_menu).setVisible(true);
            menu.findItem(R.id.blocklist_menu).setVisible(true);
            setHiddenThreadsMenuAsync(menu);
            setFavoritesMenuAsync(menu);
        }

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
    //public void refresh(final String refreshMessage) {
    public void refresh() {
        //if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ msg=" + refreshMessage);
        if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/");
        if (handler == null) { // background refresh
            if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ refreshing on ui thread in background");
            runOnUiThread(makeRefresher(true));
            return;
        }
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        if (handler == null)
            return;
        if (board.newThreads == 0 || board.isVirtualBoard()) {
            if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ restarting loader on handler");
            handler.post(makeRefresher(false));
        }
        else {
            setProgress(false);
        }
    }

    protected Runnable makeRefresher(final boolean backgroundRefresh) {
        return new Runnable() {
            @Override
            public void run() {
                if (DEBUG) Log.i(TAG, "refresh() /" + boardCode + "/ background=" + backgroundRefresh);
                if (getSupportLoaderManager() != null) {
                    Bundle bundle;
                    if (backgroundRefresh) {
                        bundle = new Bundle();
                        bundle.putBoolean(BACKGROUND_REFRESH, backgroundRefresh);
                    }
                    else {
                        bundle = null;
                    }
                    getSupportLoaderManager().restartLoader(LOADER_ID, bundle, loaderCallbacks);
                }
            }
        };
    }

    /*
    protected void refreshThreadInBoard(final long refreshThreadNo) {
        if (DEBUG) Log.i(TAG, "refreshThreadInBoard /" + boardCode + "/" + refreshThreadNo);
        Runnable refresher = refreshThreadInBoardMaker(refreshThreadNo);
        if (handler != null)
            handler.post(refresher);
        else
            runOnUiThread(refresher);
    }

    protected Runnable refreshThreadInBoardMaker(final long refreshThreadNo) {
        return new Runnable() {
            @Override
            public void run() {
                if (DEBUG) Log.i(TAG, "refreshThreadInBoard cursor=" + (adapter == null ? "null" : adapter.getCursor()));
                if (adapter == null)
                    return;
                Cursor cursor = adapter.getCursor();
                if (cursor == null || !(cursor instanceof MatrixCursor))
                    return;
                MatrixCursor m = (MatrixCursor)cursor;
                if (!m.moveToPosition(-1))
                    return;
                while (m.moveToNext()) {
                    if (m.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO)) == refreshThreadNo) {
                        m.
                        adapter.notifyDataSetChanged();
                        adapter.
                    }
                }
            }
        };
    }
    */
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
                            getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
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
        //if (handler != null)
        //    setProgressBarIndeterminateVisibility(on);
        if (mPullToRefreshAttacher != null) {
            if (DEBUG) Log.i(TAG, "mPullToRefreshAttacher.setRefreshing(" + on + ")");
            mPullToRefreshAttacher.setRefreshing(on);
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

    @TargetApi(16)
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                deprecatedSetAlpha(boardIcon, alpha);
            else
                boardIcon.setImageAlpha(alpha);
        }

        boardTitleBar.setVisibility(View.VISIBLE);
        if (DEBUG) Log.i(TAG, "displayBoardTitle /" + boardCode + "/ title=" + title + " set to visible");
    }

    @SuppressWarnings("deprecation")
    protected static void deprecatedSetAlpha(ImageView v, int a) {
        v.setAlpha(a);
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
            else if (ChanBoard.isPopularBoard(boardCode) || ChanBoard.isPopularBoard(groupBoardCode))
                menuId = R.menu.popular_context_menu;
            else if (ChanBoard.isMetaBoard(boardCode) || ChanBoard.isMetaBoard(groupBoardCode))
                menuId = R.menu.meta_board_context_menu;
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

    protected void setFavoritesMenuAsync(final Menu menu) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ChanBoard favoritesBoard = ChanFileStorage.loadBoardData(BoardActivity.this, ChanBoard.FAVORITES_BOARD_CODE);
                final ChanThread thread = ChanBoard.makeFavoritesThread(BoardActivity.this, boardCode);
                final boolean favorited = ChanFileStorage.isFavoriteBoard(favoritesBoard, thread);
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (menu == null)
                                return;
                            MenuItem item;
                            if ((item = menu.findItem(R.id.board_add_to_favorites_menu)) != null)
                                item.setVisible(!favorited);
                            if ((item = menu.findItem(R.id.favorites_remove_board_menu)) != null)
                                item.setVisible(favorited);
                        }
                    });
            }
        }).start();
    }

    protected void setHiddenThreadsMenuAsync(final Menu menu) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean hasHiddenThreads = ChanBlocklist.hasMatching(BoardActivity.this, ChanBlocklist.BlockType.THREAD, ChanThread.uniqueId(boardCode, 0, 0));
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (menu == null)
                                return;
                            MenuItem item;
                            if ((item = menu.findItem(R.id.show_hidden_threads_menu)) != null)
                                item.setVisible(hasHiddenThreads);
                        }
                    });
            }
        }).start();
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
                case R.id.board_thread_goto_menu:
                    FetchChanDataService.scheduleThreadFetch(BoardActivity.this, boardCode, threadNo, true, false);
                    ThreadActivity.startActivity(BoardActivity.this, boardCode, threadNo, "");
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
            if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
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
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "onBackPressed with query, refreshing activity");
            switchBoard(boardCode, "");
        }
        else {
            if (DEBUG) Log.i(TAG, "onBackPressed without query, navigating up");
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
        if (absListView == null) {
            if (DEBUG) Log.i(TAG, "dispatchKeyEvent absListView is null, ignoring");
            return super.dispatchKeyEvent(event);
        }
        boolean handled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, false)
                && ListViewKeyScroller.dispatchKeyEvent(event, absListView);
        if (handled)
            return true;
        else
            return super.dispatchKeyEvent(event);
    }

    protected BroadcastReceiver onUpdateBoardReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.getAction().equals(UPDATE_BOARD_ACTION))
                return;
            if (!intent.hasExtra(ChanBoard.BOARD_CODE) || intent.getStringExtra(ChanBoard.BOARD_CODE) == null) {
                //setAdapters();
                refresh();
                return;
            }
            String receivedBoardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            //long receivedThreadNo = intent.hasExtra(ChanThread.THREAD_NO)
            //        ? intent.getLongExtra(ChanThread.THREAD_NO, -1)
            //        : -1;
            //if (DEBUG) Log.i(TAG, "onUpdateBoardReceived /" + boardCode + "/ received=/" + receivedBoardCode + "/"
            //        + (receivedThreadNo >= 0 ? receivedThreadNo : ""));
            //setAdapters();
            if (!receivedBoardCode.equals(boardCode))
                return;
            //if (receivedThreadNo > 0)
            //    refreshThreadInBoard(receivedThreadNo);
            //else
                refresh();
        }
    };

    protected BroadcastReceiver onUpdateAbbrevReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean receivedAbbrevEnable = intent != null && intent.getAction().equals(UPDATE_ABBREV_ACTION) && intent.hasExtra(OPTION_ENABLE)
                    ? intent.getBooleanExtra(OPTION_ENABLE, false)
                    : false;
            if (DEBUG) Log.i(TAG, "onUpdateAbbrevReceived /" + boardCode + "/ received=/" + receivedAbbrevEnable + "/");
            if (receivedAbbrevEnable)
                gridViewOptions |= BoardViewer.ABBREV_BOARDS;
            else
                gridViewOptions &= ~BoardViewer.ABBREV_BOARDS;
            final Handler gridHandler = handler != null ? handler : new Handler();
            if (gridHandler != null)
                gridHandler.post(refreshAbsListView);
        }
    };

    protected BroadcastReceiver onUpdateFastScrollReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean receivedEnable = intent != null
                    && intent.getAction().equals(UPDATE_FAST_SCROLL_ACTION)
                    && intent.hasExtra(OPTION_ENABLE)
                    ? intent.getBooleanExtra(OPTION_ENABLE, false)
                    : true;
            if (DEBUG) Log.i(TAG, "onUpdateFastScrollReceived /" + boardCode + "/ received=/" + receivedEnable + "/");
            final Handler gridHandler = handler != null ? handler : new Handler();
            if (gridHandler != null)
                gridHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (absListView != null) {
                            absListView.setFastScrollEnabled(receivedEnable);
                        }
                    }
                });
        }
    };

    protected BroadcastReceiver onUpdateCatalogReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean receivedcatalogEnable = intent != null && intent.getAction().equals(UPDATE_CATALOG_ACTION) && intent.hasExtra(OPTION_ENABLE)
                    ? intent.getBooleanExtra(OPTION_ENABLE, false)
                    : false;
            if (DEBUG) Log.i(TAG, "onUpdateCatalogReceived /" + boardCode + "/ received=/" + receivedcatalogEnable + "/");
            if (receivedcatalogEnable)
                gridViewOptions |= BoardViewer.CATALOG_GRID;
            else
                gridViewOptions &= ~BoardViewer.CATALOG_GRID;
            final Handler gridHandler = handler != null ? handler : new Handler();
            if (gridHandler != null)
                gridHandler.post(refreshAbsListView);
        }
    };

    protected BroadcastReceiver onUpdateHideLastRepliesReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean receivedEnable = intent != null && intent.getAction().equals(UPDATE_HIDE_LAST_REPLIES_ACTION)
                    && intent.hasExtra(OPTION_ENABLE)
                    ? intent.getBooleanExtra(OPTION_ENABLE, false)
                    : false;
            if (DEBUG) Log.i(TAG, "onUpdateHideLastRepliesReceived /" + boardCode + "/ received=/" + receivedEnable + "/");
            if (receivedEnable)
                gridViewOptions |= BoardViewer.HIDE_LAST_REPLIES;
            else
                gridViewOptions &= ~BoardViewer.HIDE_LAST_REPLIES;
            final Handler gridHandler = handler != null ? handler : new Handler();
            if (gridHandler != null)
                gridHandler.post(refreshAbsListView);
        }
    };

    public static void updateAbbrev(Context context, boolean enabled) {
        Intent intent = new Intent(BoardActivity.UPDATE_ABBREV_ACTION);
        intent.putExtra(OPTION_ENABLE, enabled);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateFastScroll(Context context, boolean enabled) {
        Intent intent = new Intent(BoardActivity.UPDATE_FAST_SCROLL_ACTION);
        intent.putExtra(OPTION_ENABLE, enabled);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateCatalog(Context context, boolean enabled) {
        Intent intent = new Intent(BoardActivity.UPDATE_CATALOG_ACTION);
        intent.putExtra(OPTION_ENABLE, enabled);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateHideLastReplies(Context context, boolean enabled) {
        Intent intent = new Intent(BoardActivity.UPDATE_HIDE_LAST_REPLIES_ACTION);
        intent.putExtra(OPTION_ENABLE, enabled);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateBoard(Context context, String boardCode) {
        Intent intent = new Intent(BoardActivity.UPDATE_BOARD_ACTION);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateBoard(Context context, String boardCode, long threadNo) {
        Intent intent = new Intent(BoardActivity.UPDATE_BOARD_ACTION);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateBoard(Context context) {
        Intent intent = new Intent(BoardActivity.UPDATE_BOARD_ACTION);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected Runnable refreshAbsListView = new Runnable() {
        @Override
        public void run() {
            Cursor c = adapter.getCursor();
            createAbsListView();
            setupBoardTitle();
            //adapter.swapCursor(c);
            adapter.changeCursor(c);
            startLoaderAsync();
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int orientation = getResources().getConfiguration().orientation;
        int newOrientation = newConfig.orientation;
        if (DEBUG) Log.i(TAG, "onConfigurationChanged orientation=" + orientation + " newOrientation=" + orientation
                + " handler=" + handler + " absListView=" + absListView);
        super.onConfigurationChanged(newConfig);
        if (handler == null || absListView == null)
            scheduleRecreate = true;
        else
            recreateListViewPreservingPosition();
    }

    protected void recreateListViewPreservingPosition() {
        if (handler == null || absListView == null)
            return;
        final int pos = absListView.getFirstVisiblePosition();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Cursor c = adapter.getCursor();
                createAbsListView();
                setupBoardTitle();
                //adapter.swapCursor(c);
                adapter.changeCursor(c);
                absListView.setSelection(pos);
            }
        });
    }

}
