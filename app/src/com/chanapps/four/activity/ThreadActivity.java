package com.chanapps.four.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.adapter.BoardNarrowCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardViewer;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */

public class ThreadActivity
        //extends AbstractBoardSpinnerActivity
        extends AbstractDrawerActivity
        implements ChanIdentifiedActivity
{

    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    protected static final int OFFSCREEN_PAGE_LIMIT = 1;
    protected static final int LOADER_ID = 1;
    protected static final String FIRST_VISIBLE_BOARD_POSITION = "firstVisibleBoardPosition";
    protected static final String FIRST_VISIBLE_BOARD_POSITION_OFFSET = "firstVisibleBoardPositionOffset";
    protected static final String UPDATE_FAST_SCROLL_ACTION = "updateFastScrollAction";
    protected static final String OPTION_ENABLE = "optionEnable";

    protected ThreadPagerAdapter mAdapter;
    protected ControllableViewPager mPager;
    protected Handler handler;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected long postNo; // for direct jumps from latest post / recent images
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected boolean narrowTablet;
    protected View layout;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected AbsListView boardGrid;
    protected int firstVisibleBoardPosition = -1;
    protected int firstVisibleBoardPositionOffset = -1;
    protected boolean tabletTestDone = false;
    protected int columnWidth = 0;
    protected int columnHeight = 0;

    public static void startActivity(Context from, ChanActivityId aid) {
        startActivity(from, aid.boardCode, aid.threadNo, aid.postNo, aid.text);
    }

    public static void  startActivity(Context from, String boardCode, long threadNo, String query) {
        startActivity(from, boardCode, threadNo, 0, query);
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postNo, String query) {
        if (DEBUG) Log.i(TAG, "startActivity /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        if (threadNo <= 0) {
            BoardActivity.startActivity(from, boardCode, query);
            return;
        }
        if (from instanceof ThreadActivity) { // switch thread instead of launching activity
            ((ThreadActivity)from).switchThreadInternal(boardCode, threadNo, postNo, query);
            return;
        }
        if (postNo <= 0 || postNo == threadNo)
            from.startActivity(createIntent(from, boardCode, threadNo, query));
        else
            from.startActivity(createIntent(from, boardCode, threadNo, postNo, query));
    }

    public static Intent createIntent(Context context, final String boardCode, final long threadNo, String query) {
        return createIntent(context, boardCode, threadNo, 0, query);
    }

    public static Intent createIntent(Context context, final String boardCode,
                                      final long threadNo, final long postNo, String query) {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(BOARD_CODE, boardCode);
        intent.putExtra(THREAD_NO, threadNo);
        intent.putExtra(POST_NO, postNo);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    public boolean isSelfDrawerMenu(String boardAsMenu) {
        if (boardAsMenu == null || boardAsMenu.isEmpty())
            return false;
        if (boardAsMenu.matches("/" + boardCode + "/" + threadNo + ".*") && (query == null || query.isEmpty()))
            return true;
        return false;
    }

    @Override
    protected void createViews(Bundle bundle) {
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (DEBUG) Log.i(TAG, "createViews() /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.defaultBoardCode(this);
        if (threadNo <= 0)
            redirectToBoard();

        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        layout = getLayoutInflater().inflate(R.layout.thread_activity_layout, null);
        contentFrame.addView(layout);

        try {
            mPullToRefreshAttacher = new PullToRefreshAttacher(this, new PullToRefreshAttacher.Options());
        }
        catch (OutOfMemoryError e) {
            Log.e(TAG, "createViews() couldn't load pull to refresh, out of memory");
            mPullToRefreshAttacher = null;
        }
        ThreadViewer.initStatics(getApplicationContext(), ThemeSelector.instance(getApplicationContext()).isDark());

        narrowTablet = getResources().getBoolean(R.bool.narrow_tablet);
        setupReceivers();
    }

    protected void setupReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateFastScrollReceived,
                new IntentFilter(UPDATE_FAST_SCROLL_ACTION));
    }
    
    protected void teardownReceivers() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(onUpdateFastScrollReceived);
    }

    protected void createPager(final ChanBoard board) { // must be called on UI thread
        if (DEBUG) Log.i(TAG, "createPager /" + (board == null ? null : board.link) + "/ q=" + query);
        if (onTablet())
            getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
        if (query != null && !query.isEmpty()) {
            if (mPager != null)
                mPager.removeAllViews();
            mPager = null;
            mAdapter = null;
        }
        else if (mPager != null && mAdapter != null && mAdapter.getBoardCode() != null
                && mAdapter.getBoardCode().equals(board.link)) {
            if (DEBUG) Log.i(TAG, "createPager() pager already exists, exiting");
            return;
        }
        if (mAdapter == null)
            mAdapter = new ThreadPagerAdapter(getSupportFragmentManager());
        mAdapter.setBoard(board);
        mAdapter.setQuery(query);
        mAdapter.notifyDataSetChanged();
        if (mPager == null)
            mPager = (ControllableViewPager) findViewById(R.id.pager);
        try {
            mPager.setAdapter(mAdapter);
            boolean pagingEnabled = query == null || query.isEmpty();
            mPager.setPagingEnabled(pagingEnabled);
        }
        catch (IllegalStateException e) {
            Log.e(TAG, "Error: pager state exception", e);
            Toast.makeText(ThreadActivity.this, R.string.thread_couldnt_create_pager, Toast.LENGTH_SHORT);
        }
    }

    public void showThread(long threadNo) {
        this.threadNo = threadNo;
        syncPagerAsync();
    }

    protected void syncPagerOnHandler(final ChanBoard board) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    syncPager(board);
                }
            });
    }

    protected void syncPager(final ChanBoard board) {
        int pos = board.getThreadIndex(boardCode, threadNo);
        if (mAdapter != null && pos >= 0 && pos < mAdapter.getCount()) { // found it
            if (DEBUG) Log.i(TAG, "syncPager /" + boardCode + "/" + threadNo + " setting pos=" + pos);
            if (pos == mPager.getCurrentItem()) // it's already selected, do nothing
                ;
            else
                mPager.setCurrentItem(pos, false); // select the item
        }
        else { // we didn't find it, default to 0th thread
            if (DEBUG) Log.i(TAG, "syncPager /" + boardCode + "/" + threadNo + " not found pos=" + pos + " defaulting to zero");
            pos = 0;
            if (mPager != null)
                mPager.setCurrentItem(pos, false); // select the item
            ThreadFragment fragment = getCurrentFragment();
            if (fragment != null) {
                ChanActivityId activityId = fragment.getChanActivityId();
                boardCode = activityId.boardCode;
                threadNo = activityId.threadNo;
                Toast.makeText(getActivityContext(), R.string.thread_not_found, Toast.LENGTH_SHORT).show();
            }
        }
        //setProgress(false);
    }

    protected void redirectToBoard() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board /" + boardCode + "/");
        Intent intent = BoardActivity.createIntent(this, boardCode, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putString(SearchManager.QUERY, query);
        int boardPos = !onTablet() ? -1 : boardGrid.getFirstVisiblePosition();
        View boardView = !onTablet() ? null : boardGrid.getChildAt(0);
        int boardOffset = boardView == null ? 0 : boardView.getTop();
        bundle.putInt(FIRST_VISIBLE_BOARD_POSITION, boardPos);
        bundle.putInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET, boardOffset);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        query = bundle.getString(SearchManager.QUERY);
        firstVisibleBoardPosition = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION);
        firstVisibleBoardPositionOffset = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "onNewIntent begin /" + intent.getStringExtra(ChanBoard.BOARD_CODE) + "/"
                + intent.getLongExtra(ChanThread.THREAD_NO, -1)
                + "#p" + intent.getLongExtra(ChanThread.POST_NO, -1)
                + " q=" + intent.getStringExtra(SearchManager.QUERY));
        switchThreadInternal(intent.getStringExtra(ChanBoard.BOARD_CODE),
                intent.getLongExtra(ChanThread.THREAD_NO, -1),
                intent.getLongExtra(ChanThread.POST_NO, -1),
                intent.getStringExtra(SearchManager.QUERY));
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/" + threadNo + " q=" + query);
    }

    public void setFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null || intent.hasExtra(ChanBoard.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
            postNo = intent.getLongExtra(ChanThread.POST_NO, 0);
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            String uriThreadNo = params.get(1);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null && uriThreadNo != null) {
                boardCode = uriBoardCode;
                threadNo = Long.valueOf(uriThreadNo);
                postNo = 0;
                query = "";
                if (DEBUG) Log.i(TAG, "loaded /" + boardCode + "/" + threadNo + " from url intent");
            }
            else {
                boardCode = ChanBoard.DEFAULT_BOARD_CODE;
                threadNo = 0;
                postNo = 0;
                query = "";
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (getIntent() != null) {
            if (DEBUG) Log.i(TAG, "onStart intent=/" + getIntent().getStringExtra(BOARD_CODE) + "/" + getIntent().getLongExtra(THREAD_NO, 0));
        }
        if (handler == null)
            handler = new Handler();
        NetworkProfileManager.instance().activityChange(this);
        if (onTablet())
            createAbsListView();
        createPagerAsync();
        loadDrawerArray();
    }

    protected void createPagerAsync() {
        final ChanIdentifiedActivity activity = this;
        if (mPager != null) {
            if (DEBUG) Log.i(TAG, "createPagerAsync() pager already exists, exiting");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                final ChanBoard board = BoardCursorLoader.loadBoardSorted(ThreadActivity.this, boardCode);
                if (mAdapter != null && mAdapter.getCount() > 0 && board.hasData() && board.isCurrent()) {
                    if (DEBUG) Log.i(TAG, "createPagerAsync() /" + boardCode + "/" + threadNo + " adapter already loaded, skipping");
                }
                else if (board.hasData()) { // && board.isCurrent()) {
                    if (DEBUG) Log.i(TAG, "createPagerAsync() /" + boardCode + "/" + threadNo + " board has current data, loading");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                createPager(board);
                                syncPager(board);
                            }
                        });
                }
                else if (board.hasData() &&
                        (health == NetworkProfile.Health.NO_CONNECTION
                        ))
                {
                    if (DEBUG) Log.i(TAG, "createPagerAsync() /" + boardCode + "/" + threadNo + " board has old data but connection " + health + ", loading");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                createPager(board);
                                syncPager(board);
                            }
                        });
                }
                else if (health == NetworkProfile.Health.NO_CONNECTION) {
                    if (DEBUG) Log.i(TAG, "createPagerAsync() /" + boardCode + "/" + threadNo + " no board data and connection is down");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.board_no_connection_load, Toast.LENGTH_SHORT).show();
                                //setProgress(false);
                            }
                        });
                }

                else {
                    if (DEBUG) Log.i(TAG, "createPagerAsync() /" + boardCode + "/" + threadNo + " no board data, priority refreshing board");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //setProgress(true);
                            }
                        });
                    FetchChanDataService.scheduleBoardFetch(ThreadActivity.this, boardCode, true, false);
                    refreshing = true;
                }
            }
        }).start();
    }

    public boolean refreshing = false;

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo + " q=" + query);
        if (handler == null)
            handler = new Handler();
        if (redisplayPagerOnResume) {
            redisplayPagerOnResume = false;
            redisplayPager();
        }
        else {
            resumePager();
        }
    }

    protected void resumePager() {
        /*
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "resumePager /" + boardCode + "/" + threadNo + " q=" + query + " performing query");
            redisplayPager(boardCode, threadNo, "");
        }
        else
        */
        /*
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "resumePager /" + boardCode + "/" + threadNo + " awaiting query callback");
        }
        else
        */
        if (mAdapter != null && mAdapter.getCount() > 0) {
            if (DEBUG) Log.i(TAG, "resumePager /" + boardCode + "/" + threadNo + " setting current item pager count=" + mAdapter.getCount());
            syncPagerAsync();
        }
        else {
            if (DEBUG) Log.i(TAG, "resumePager /" + boardCode + "/" + threadNo + " pager not loaded, awaiting callback");
        }
        if (onTablet()
                && !getSupportLoaderManager().hasRunningLoaders()
                && (adapterBoardsTablet == null || adapterBoardsTablet.getCount() == 0)) {
            if (DEBUG) Log.i(TAG, "resumePager calling restartLoader");
            getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
        }
    }

    protected void syncPagerAsync() {
        if (DEBUG) Log.i(TAG, "syncPagerAsync() /" + boardCode + "/" + threadNo + " selecting current item in pager");
        final ChanIdentifiedActivity activity = this;
        ThreadFragment fragment = getCurrentFragment();
        if (fragment != null) {
            ChanActivityId aid = fragment.getChanActivityId();
            boolean onThread = aid != null && boardCode != null && boardCode.equals(aid.boardCode) && threadNo == aid.threadNo;
            boolean sameQuery = aid != null && ((query == null && aid.text == null) || (query != null && query.equals(aid.text)));
            if (onThread && sameQuery) {
                if (DEBUG) Log.i(TAG, "syncPagerAsync() already on thread with idential query, exiting");
                return;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "syncPagerAsync() storing activity change");
                    NetworkProfileManager.instance().activityChange(activity);
                }
                ChanBoard board = BoardCursorLoader.loadBoardSorted(ThreadActivity.this, boardCode);
                int idx = board.getThreadIndex(boardCode, threadNo);
                if (idx == -1) {
                    if (DEBUG) Log.i(TAG, "syncPagerAsync() thread not in board, waiting for board refresh");
                }
                else {
                    if (DEBUG) Log.i(TAG, "syncPagerAsync() set current item to thread");
                    syncPagerOnHandler(board);
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/" + threadNo);
        handler = null;
        ThreadFragment fragment = getCurrentFragment();
        ChanActivityId activityId = fragment == null ? null : fragment.getChanActivityId();
        if (activityId != null
                && activityId.boardCode != null
                && !activityId.boardCode.isEmpty()
                && activityId.threadNo > 0
                ) { // different activity
            // only change if thread doesn't exist in board
            //if (board.getThreadIndex(boardCode, threadNo) == -1) {
                boardCode = activityId.boardCode;
                threadNo = activityId.threadNo;
                if (DEBUG) Log.i(TAG, "onPause save default thread to /" + boardCode + "/" + threadNo);
            //}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/" + threadNo);
        handler = null;
        if (onTablet()) {
            if (DEBUG) Log.i(TAG, "onStop calling destroyLoader");
            getSupportLoaderManager().destroyLoader(LOADER_ID);
        }
        setProgress(false);
        teardownReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // menu creation handled at fragment level instead
        return super.onCreateOptionsMenu(menu);
    }

    public void createSearchView(Menu menu) {
        searchMenuItem = menu.findItem(R.id.search_menu);
        if (searchMenuItem != null)
            SearchActivity.createSearchView(this, searchMenuItem);
    }

    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo, postNo, query);
    }

    public void setChanActivityId(ChanActivityId aid) {
        boardCode = aid.boardCode;
        threadNo = aid.threadNo;
        postNo = aid.postNo;
        query = aid.text;
    }

    @Override
    public void refresh() {
        refreshing = false;
        invalidateOptionsMenu(); // in case spinner needs to be reset
        ThreadFragment fragment = getCurrentFragment();
        if (fragment != null)
            fragment.onRefresh();
        if (handler != null && onTablet())
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "refreshBoard() restarting loader");
                    getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                }
            });
    }

    public void refreshFragment(final String boardCode, final long threadNo, final String message) {
        refreshing = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshFragmentSync(boardCode, threadNo, message);
            }
        }).start();
    }

    protected boolean redisplayPagerOnResume = false;

    protected void redisplayPager() {
        redisplayPager(boardCode, threadNo, query, "");
    }

    protected void redisplayPager(final String boardCode, final long threadNo, final String query, final String message) {
        if (DEBUG) Log.i(TAG, "redisplayPager() /" + boardCode + "/" + threadNo + " q=" + query + " handler=" + handler);
        final ChanBoard fragmentBoard = BoardCursorLoader.loadBoardSorted(ThreadActivity.this, boardCode);
        if (fragmentBoard.defData)
            return;
        if (handler == null) {
            redisplayPagerOnResume = true;
        }
        else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "redisplayPager() /" + boardCode + "/" + threadNo + " q=" + query + " recreating pager on handler");
                    if (onTablet()) {
                        createAbsListView();
                        getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
                    }
                    createPager(fragmentBoard);
                    syncPager(fragmentBoard);
                    refreshAllDisplayedFragments(boardCode, threadNo, query, message);
                }
            });
        }
    }

    public void restartLoader() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
    }

    public void refreshFragmentSync(final String boardCode, final long threadNo, final String message) {
        if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " message=" + message);
        if (hasValidPager())
            refreshAllDisplayedFragments(boardCode, threadNo, query, message);
        else
            redisplayPager(boardCode, threadNo, query, message);
    }

    protected boolean hasValidPager() {
        if (mPager == null) {
            if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null pager");
            return false;
        }
        if (mAdapter == null) {
            if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null adapter");
            return false;
        }
        if (!boardCode.equals(mAdapter.getBoardCode()))
            return false;
        return true;
    }

    protected void refreshAllDisplayedFragments(final String boardCode, final long threadNo, final String query, final String message) {
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "refreshAllDisplayedFragments() query present, exiting");
            return;
        }
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int current = mPager.getCurrentItem();
                    int delta = mPager.getOffscreenPageLimit();
                    boolean found = false;
                    for (int i = current - delta; i < current + delta + 1; i++) {
                        String msg = i == current ? message : null;
                        if (refreshFragmentAtPosition(boardCode, threadNo, i, msg))
                            found = true;
                    }
                    //if (!found)
                    //    setProgress(false);
                }
            });
    }

    protected void setCurrentItemAsync(final int pos, final boolean smooth) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mPager.setCurrentItem(pos, smooth);
                    ThreadFragment fragment2;
                    if ((fragment2 = getFragmentAtPosition(pos)) != null
                            && fragment2.getChanActivityId() != null
                            && fragment2.getChanActivityId().threadNo > 0)
                        fragment2.refreshThread(null);
                }
            });
    }


    protected boolean refreshFragmentAtPosition(String boardCode, long threadNo, int pos, String message) {
        ThreadFragment fragment;
        ChanActivityId data;
        if (pos < 0 || pos >= mAdapter.getCount()) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " out of bounds, skipping");
            return false;
        }
        if ((fragment = getFragmentAtPosition(pos)) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null fragment at position, skipping");
            return false;
        }
        if ((data = fragment.getChanActivityId()) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null getChanActivityId(), skipping");
            return false;
        }
        if (data.boardCode == null || !data.boardCode.equals(boardCode) || data.threadNo != threadNo) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " unmatching data=/" + data.boardCode + "/" + data.threadNo + ", skipping");
            return false;
        }
        fragment.setQuery("");
        if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos + " refreshing");
        fragment.refreshThread(message);
        return true;
    }

    @Override
    public void closeSearch() {
        if (DEBUG) Log.i(TAG, "closeSearch /" + boardCode + "/" + threadNo + " q=" + query);
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

    @Override
    public Handler getChanHandler() {
        ThreadFragment fragment = getCurrentFragment();
        return fragment == null ? handler : fragment.getHandler();
    }

    @Override
    protected void createActionBar() {
        super.createActionBar();
    }

    protected Activity getActivity() {
        return this;
    }

    protected Context getActivityContext() {
        return this;
    }

    protected ChanIdentifiedActivity getChanActivity() {
        return this;
    }

    public ThreadFragment getCurrentFragment() {
        if (mPager == null)
            return null;
        int i = mPager.getCurrentItem();
        return getFragmentAtPosition(i);
    }

    protected ThreadFragment getFragmentAtPosition(int pos) {
        if (mAdapter == null)
            return null;
        else
            return mAdapter.getCachedItem(pos);
    }

    public class ThreadPagerAdapter extends FragmentStatePagerAdapter {
        protected String boardCode;
        protected ChanBoard board;
        protected String query;
        protected int count;
        protected Map<Integer,WeakReference<ThreadFragment>> fragments
                = new HashMap<Integer, WeakReference<ThreadFragment>>();
        public ThreadPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        public void setBoard(ChanBoard board) {
            if (board == null || board.threads == null)
                throw new UnsupportedOperationException("can't start pager with null board or null threads");
            this.boardCode = board.link;
            this.board = board;
            this.count = board.threads == null ? 0 : board.threads.length;
            super.notifyDataSetChanged();
        }
        public String getBoardCode() {
            return this.boardCode;
        }
        public void setQuery(String query) {
            this.query = query;
        }
        @Override
        public void notifyDataSetChanged() {
            board = BoardCursorLoader.loadBoardSorted(ThreadActivity.this, boardCode);
            count = board.threads.length;
            super.notifyDataSetChanged();
        }
        @Override
        public int getCount() {
            return count;
        }
        @Override
        public Fragment getItem(int pos) {
            if (pos < count)
                return createFragment(pos);
            else
                return null;
        }
        protected Fragment createFragment(int pos) {
            // get thread
            ChanPost thread = board.threads[pos];
            String boardCode = thread.board;
            long threadNo = thread.no;
            long postNo = (boardCode != null
                    && boardCode.equals(ThreadActivity.this.boardCode)
                    && threadNo == ThreadActivity.this.threadNo)
                    && ThreadActivity.this.postNo > 0
                    ? ThreadActivity.this.postNo
                    : -1;
            String query = this.query;
            // make fragment
            ThreadFragment fragment = new ThreadFragment();
            Bundle bundle = new Bundle();
            bundle.putString(BOARD_CODE, boardCode);
            bundle.putLong(THREAD_NO, threadNo);
            bundle.putLong(POST_NO, postNo);
            bundle.putString(SearchManager.QUERY, query);
            if (DEBUG) Log.i(TAG, "createFragment /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
            fragment.setArguments(bundle);
            fragment.setHasOptionsMenu(true);
            return fragment;
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object object = super.instantiateItem(container, position);
            fragments.put(position, new WeakReference<ThreadFragment>((ThreadFragment)object));
            return object;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            fragments.remove(position);
        }
        public ThreadFragment getCachedItem(int position) {
            WeakReference<ThreadFragment> ref = fragments.get(position);
            if (ref == null)
                return null;
            else
                return ref.get();
        }
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            ThreadFragment fragment = (ThreadFragment)object;
            if (primaryItem != fragment && fragment.getChanActivityId().threadNo > 0) {
                if (DEBUG) Log.i(TAG, "setPrimaryItem pos=" + position + " obj=" + fragment
                        + " rebinding mPullToRefreshAttacher");
                if (primaryItem != null)
                    primaryItem.setPullToRefreshAttacher(null);
                primaryItem = fragment;
                primaryItem.fetchIfNeeded(handler);
                fragment.setPullToRefreshAttacher(mPullToRefreshAttacher);
                if (onTablet()) {
                    int first = boardGrid.getFirstVisiblePosition();
                    int last = boardGrid.getLastVisiblePosition();
                    boolean positionVisible = first <= position && position <= last;
                    if (!positionVisible) {
                        if (DEBUG) Log.i(TAG, "scrolling to pos=" + position + " not in range [" + first + "," + last + "]");
                        final int toPos = position;
                        boardGrid.post(new Runnable() {
                            @Override
                            public void run() {
                                boardGrid.setSelection(toPos);
                            }
                        });
                    }
                    else {
                        if (DEBUG) Log.i(TAG, "not scrolling to pos=" + position + " in range [" + first + "," + last + "]");
                    }
                }
            }
            super.setPrimaryItem(container, position, object);
        }
        protected ThreadFragment primaryItem = null;
    }

    public ThreadFragment getPrimaryItem() {
        if (mAdapter == null)
            return null;
        else
            return mAdapter.primaryItem;
    }

    public void setProgressForFragment(String boardCode, long threadNo, boolean on) {
        ThreadFragment fragment = getCurrentFragment();
        if (fragment == null)
            return;
        ChanActivityId data = fragment.getChanActivityId();
        if (data == null)
            return;
        if (data.boardCode == null)
            return;
        if (!data.boardCode.equals(boardCode))
            return;
        if (data.threadNo != threadNo)
            return;
        setProgress(on);
    }

    @Override
    public void setProgress(boolean on) {
        if (DEBUG) Log.i(TAG, "setProgress(" + on + ")");
        if (mPullToRefreshAttacher != null) {
            if (DEBUG) Log.i(TAG, "mPullToRefreshAttacher.setRefreshing(" + on + ")");
            mPullToRefreshAttacher.setRefreshing(on);
        }
    }

    protected void initTablet() {
        if (!tabletTestDone) {
            boardGrid = (AbsListView)findViewById(R.id.board_grid_view_tablet);
            tabletTestDone = true;
        }
    }

    public boolean onTablet() {
        initTablet();
        return boardGrid != null;
    }

    protected void createAbsListView() {
        initTablet();
        if (adapterBoardsTablet != null && adapterBoardsTablet.getCount() > 0) {
            if (DEBUG) Log.i(TAG, "createAbsListView() /" + boardCode + "/" + threadNo + " adapter already loaded, skipping");
            return;
        }
        if (DEBUG) Log.i(TAG, "createAbsListView() /" + boardCode + "/" + threadNo + " creating adapter");
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        columnWidth = ChanGridSizer.getCalculatedWidth(
                getResources().getDimensionPixelSize(R.dimen.BoardGridViewTablet_image_width),
                1,
                getResources().getDimensionPixelSize(R.dimen.BoardGridView_spacing));
        columnHeight = 2 * columnWidth;
        if (narrowTablet)
            adapterBoardsTablet = new BoardNarrowCursorAdapter(this, viewBinder);
        else
            adapterBoardsTablet = new BoardCursorAdapter(this, viewBinder);
        adapterBoardsTablet.setGroupBoardCode(boardCode);
        boardGrid.setAdapter(adapterBoardsTablet);
        boardGrid.setOnItemClickListener(boardGridListener);
        boardGrid.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        boardGrid.setFastScrollEnabled(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, false));
    }

    protected void onBoardsTabletLoadFinished(Cursor data) {
        if (boardGrid == null)
            createAbsListView();
        //this.adapterBoardsTablet.swapCursor(data);
        this.adapterBoardsTablet.changeCursor(data);
        // retry load if maybe data wasn't there yet
        if (data != null && data.getCount() < 1 && handler != null) {
            if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " data count=0");
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(getActivityContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
        else if (firstVisibleBoardPosition >= 0) {
            if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " firstVisibleBoardPosition=" + firstVisibleBoardPosition);
            boardGrid.setSelection(firstVisibleBoardPosition);
            firstVisibleBoardPosition = -1;
            firstVisibleBoardPositionOffset = -1;
        }
        else if (threadNo > 0) {
            Cursor cursor = adapterBoardsTablet.getCursor();
            cursor.moveToPosition(-1);
            boolean found = false;
            int pos = 0;
            while (cursor.moveToNext()) {
                long threadNoAtPos = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (threadNoAtPos == threadNo) {
                    found = true;
                    break;
                }
                pos++;
            }
            if (found) {
                if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " pos=" + pos);
                final int selectedPos = pos;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            boardGrid.setSelection(selectedPos);
                        }
                    });
            }
            else {
                if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " thread not found");
            }
        }
    }

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ id=" + id);
            BoardSortType sortType = BoardSortType.loadFromPrefs(ThreadActivity.this);
            return new BoardCursorLoader(getActivityContext(), boardCode, "", true, false, sortType);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/ id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()) + " loader=" + loader);
            onBoardsTabletLoadFinished(data);
            refreshing = false;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ id=" + loader.getId());
            adapterBoardsTablet.changeCursor(null);
        }
    };

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int options = narrowTablet ? 0 : BoardViewer.CATALOG_GRID;
            return BoardViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight, null, null, options, null);
        }
    };

    protected AdapterView.OnItemClickListener boardGridListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                BoardActivity.startActivity(getActivityContext(), boardLink, "");
            }
            else {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                final long threadNoLink = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (boardCode.equals(boardLink) && threadNo == threadNoLink) { // already on this, do nothing
                } else if (boardCode.equals(boardLink)) { // just redisplay right tab
                    showThread(threadNoLink);
                } else {
                    ThreadActivity.startActivity(getActivityContext(), boardLink, threadNoLink, "");
                }
            }
        }
    };

    public void notifyBoardChanged() {
        if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ChanBoard board = BoardCursorLoader.loadBoardSorted(ThreadActivity.this, boardCode);
                if (board.defData) {
                    if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/ couldn't load board, exiting");
                    return;
                }
                if (mPager != null && mPager.getAdapter() != null && mAdapter != null && mAdapter.getCount() > 0) {
                    if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/ pager already filled, restarting loader");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onTablet())
                                    getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
                                mAdapter.setQuery(query);
                                mAdapter.setBoard(board);
                            }
                        });
                }
                else {
                    if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/ creating pager");
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onTablet())
                                    createAbsListView();
                                createPager(board);
                                syncPager(board);
                            }
                        });
                }
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    private boolean warnedAboutNetworkDown = false;

    public boolean warnedAboutNetworkDown() {
        return warnedAboutNetworkDown;
    }

    public void warnedAboutNetworkDown(boolean set) {
        warnedAboutNetworkDown = set;
    }

    @Override
    public void onBackPressed() {
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "onBackPressed with query, refreshing activity");
            switchThreadInternal(boardCode, threadNo, postNo, "");
        }
        else {
            if (DEBUG) Log.i(TAG, "onBackPressed without query, navigating up");
            navigateUp();
        }
    }

    public void navigateUp() {
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        if (numTasks == 0 || (numTasks == 1 && task != null && task.topActivity != null && task.topActivity.getClassName().equals(getClass().getName()))) {
            if (DEBUG) Log.i(TAG, "no valid up task found, creating new one");
            Intent intent = BoardActivity.createIntent(getActivityContext(), boardCode, "");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DEBUG) Log.i(TAG, "dispatchKeyEvent event=" + event.toString());
        ThreadFragment fragment = getCurrentFragment();
        if (fragment == null) {
            if (DEBUG) Log.i(TAG, "dispatchKeyEvent current fragment is null, ignoring");
            return super.dispatchKeyEvent(event);
        }
        AbsListView absListView = fragment.getAbsListView();
        boolean handled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, false)
            && ListViewKeyScroller.dispatchKeyEvent(event, absListView);
        if (handled)
            return true;
        else
            return super.dispatchKeyEvent(event);
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }

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
                        if (boardGrid != null)
                            boardGrid.setFastScrollEnabled(receivedEnable);
                        ThreadFragment fragment = getPrimaryItem();
                        if (fragment != null)
                            fragment.onUpdateFastScroll(receivedEnable);
                    }
                });
        }
    };

    public static void updateFastScroll(Context context, boolean enabled) {
        Intent intent = new Intent(BoardActivity.UPDATE_FAST_SCROLL_ACTION);
        intent.putExtra(OPTION_ENABLE, enabled);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void switchBoard(String boardCode, String query) {
        Intent intent = BoardActivity.createIntent(this, boardCode, query);
        if (!this.boardCode.equals(boardCode)) {
            startActivity(intent);
        }
    }

    protected void switchThreadInternal(String boardCode, long threadNo, long postNo, String query) { // for when we are already in this class
        if (DEBUG) Log.i(TAG, "switchThreadInternal begin /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        String oldBoardCode = this.boardCode;
        String oldQuery = this.query != null ? this.query : "";
        String checkQuery = query != null ? query : "";
        Intent intent = createIntent(this, boardCode, threadNo, postNo, query);
        setIntent(intent);
        setFromIntent(intent);
        NetworkProfileManager.instance().activityChange(this);

        /* move spinner to right board */
        mIgnoreMode = true;
        selectActionBarNavigationItem();

        if (!oldBoardCode.equals(boardCode)) { // recreate pager
            if (DEBUG) Log.i(TAG, "switchThreadInternal new board redisplayPager() /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
            redisplayPager(boardCode, threadNo, query, "");
        }
        else if (!oldQuery.equals(query)) {
            if (DEBUG) Log.i(TAG, "switchThreadInternal new query redisplayPager() /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
            redisplayPager(boardCode, threadNo, query, "");
        }
        else {
            if (DEBUG) Log.i(TAG, "switchThreadInternal showThread() /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
            showThread(threadNo);
        }
        if (DEBUG) Log.i(TAG, "switchThreadInternal end /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
    }


}
