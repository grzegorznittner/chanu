package com.chanapps.four.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridSmallTabletColCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardGridViewer;
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
        extends AbstractBoardSpinnerActivity
        implements ChanIdentifiedActivity
{

    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    protected static final int OFFSCREEN_PAGE_LIMIT = 1;
    protected static final int LOADER_ID = 1;
    protected static final String FIRST_VISIBLE_BOARD_POSITION = "firstVisibleBoardPosition";
    protected static final String FIRST_VISIBLE_BOARD_POSITION_OFFSET = "firstVisibleBoardPositionOffset";

    protected ThreadPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected Handler handler;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected long postNo; // for direct jumps from latest post / recent images
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected boolean wideTablet;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected AbsListView boardGrid;
    protected int firstVisibleBoardPosition = -1;
    protected int firstVisibleBoardPositionOffset = -1;
    protected boolean tabletTestDone = false;
    protected int columnWidth = 0;
    protected int columnHeight = 0;

    public static void startActivity(Context from, String boardCode, long threadNo, String query) {
        startActivity(from, boardCode, threadNo, 0, query);
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postNo, String query) {
        if (DEBUG) Log.i(TAG, "startActivity /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        if (threadNo <= 0)
            BoardActivity.startActivity(from, boardCode, query);
        else if (postNo <= 0)
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
    public boolean isSelfBoard(String boardAsMenu) {
        return false; // always jump to board
    }

    @Override
    protected int activityLayout() {
        return R.layout.thread_activity_layout;
    }

    @Override
    protected void createViews(Bundle bundle) {
        // first get all the variables
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (DEBUG) Log.i(TAG, "createViews() /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.ALL_BOARDS_BOARD_CODE;
        if (threadNo <= 0)
            redirectToBoard();

        mPullToRefreshAttacher = new PullToRefreshAttacher(this);
        ThreadViewer.initStatics(getApplicationContext(), ThemeSelector.instance(getApplicationContext()).isDark());

        wideTablet = getResources().getBoolean(R.bool.wide_tablet);
    }

    protected void createPager(final ChanBoard board) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    createPagerSync(board);
                }
            });
    }

    protected void createPagerAndSetCurrentItemAsync(final ChanBoard board) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onTablet())
                        createAbsListView();
                    createPagerSync(board);
                    setCurrentItemToThread();
                }
            });
    }

    protected void createPagerSync(final ChanBoard board) {
        if (onTablet())
            getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
        mAdapter = new ThreadPagerAdapter(getSupportFragmentManager());
        mAdapter.setBoard(board);
        mAdapter.setQuery(query);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        mPager.setAdapter(mAdapter);
    }

    public void showThread(long threadNo) {
        this.threadNo = threadNo;
        setCurrentItemToThread();
    }

    protected void setCurrentItemToThread() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int pos = getCurrentThreadPos();
                    if (mAdapter != null && pos >= 0 && pos < mAdapter.getCount()) { // found it
                        if (DEBUG) Log.i(TAG, "setCurrentItemToThread /" + boardCode + "/" + threadNo + " setting pos=" + pos);
                        if (pos == mPager.getCurrentItem()) // it's already selected, do nothing
                            ;
                        else
                            mPager.setCurrentItem(pos, false); // select the item
                        /*
                        ThreadFragment fragment = getCurrentFragment();
                        ChanActivityId aid = fragment == null ? null : fragment.getChanActivityId();
                        if (aid != null
                                && boardCode != null
                                && boardCode.equals(aid.boardCode)
                                && threadNo == aid.threadNo
                                && postNo > 0) {
                            if (DEBUG) Log.i(TAG, "setCurrentItemToThread /" + boardCode + "/" + threadNo + "#p" + postNo
                                    + " scrolling fragment to post");
                            fragment.scroll
                            postNo = 0;
                        }
                        */
                    }
                    else { // we didn't find it, default to 0th thread
                        if (DEBUG) Log.i(TAG, "setCurrentItemToThread /" + boardCode + "/" + threadNo + " not found pos=" + pos + " defaulting to zero");
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
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
    }

    protected int getCurrentThreadPos() {
        return ChanFileStorage.loadBoardData(this, boardCode).getThreadIndex(boardCode, threadNo);
    }

    /*
    protected String fragmentTag() {
        return fragmentTag(boardCode, threadNo, postNo);
    }

    public static String fragmentTag(String boardCode, long threadNo, long postNo) {
        return "/" + boardCode + "/" + threadNo + (postNo > 0 && postNo != threadNo ? "#p" + postNo : "");
    }
    */
    protected void redirectToBoard() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board /" + boardCode + "/");
        Intent intent = BoardActivity.createIntent(this, boardCode, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
                + intent.getLongExtra(ChanThread.THREAD_NO, 0));
        setIntent(intent);
        setFromIntent(intent);
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/" + threadNo);
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
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/" + threadNo + "#p" + postNo);
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
        if (onTablet())
            createAbsListView();
        createPagerAsync();
        AnalyticsComponent.onStart(this);
    }

    protected void createPagerAsync() {
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (mAdapter != null && mAdapter.getCount() > 0 && board.hasData() && board.isCurrent()) {
            if (DEBUG) Log.i(TAG, "onStart() /" + boardCode + "/" + threadNo + " adapter already loaded, skipping");
        }
        else if (board.hasData() && board.isCurrent()) {
            if (DEBUG) Log.i(TAG, "onStart() /" + boardCode + "/" + threadNo + " board has current data, loading");
            createPager(board);
        }
        else if (board.hasData() && NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth()
                == NetworkProfile.Health.NO_CONNECTION) {
            if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " board has old data but connection down, loading");
            createPager(board);
        }
        else if (NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth()
                == NetworkProfile.Health.NO_CONNECTION) {
            if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " no board data and connection is down");
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.board_no_connection_load, Toast.LENGTH_SHORT).show();
                        setProgress(false);
                    }
                });
            /*
            if (emptyText != null) {
                emptyText.setText(R.string.board_no_connection_load);
                emptyText.setVisibility(View.VISIBLE);
            }
            */
        }
        else {
            /*
            if (DEBUG) Log.i(TAG, "onStart() /" + boardCode + "/" + threadNo + " non-current board data, loading");
            if (onTablet())
                getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
            createPager();
            */
            if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " non-current board data, manual refreshing");
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setProgress(true);
                    }
                });
            NetworkProfileManager.instance().manualRefresh(activity);
            refreshing = true;
        }
            }
        }).start();
    }

    public boolean refreshing = false;

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        //ChanBoard board = ChanFileStorage.loadBoardData(this, boardCode);
        //if (board == null || !board.link.equals(boardCode) || mPager == null) { // recreate pager
        //    if (DEBUG) Log.i(TAG, "onResume() /" + boardCode + "/" + threadNo + " creating pager");
        //    createPager();
        //}
        //invalidateOptionsMenu(); // for correct spinner display
        //ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
        //ThreadFragment fragment = getCurrentFragment();
        /*
        if (activityId == null
                || activityId.boardCode == null
                || !activityId.boardCode.equals(boardCode)
                || activityId.threadNo != threadNo
                || NetworkProfileManager.instance().getActivity() != this
                ) {
            if (DEBUG) Log.i(TAG, "onResume() activity change");
            NetworkProfileManager.instance().activityChange(this);
            setCurrentItemToThread();
        }
        */
        setCurrentItemToThreadAsync();
        /*
        else if (fragment != null) {
            activityId = fragment.getChanActivityId();
            if (activityId == null
                    || activityId.boardCode == null
                    || !activityId.boardCode.equals(boardCode)
                    || activityId.threadNo != threadNo) {
                if (DEBUG) Log.i(TAG, "onResume() set current item to thread");
                setCurrentItemToThread();
            }
        }
        */

        if (onTablet()
                && !getSupportLoaderManager().hasRunningLoaders()
                && (adapterBoardsTablet == null || adapterBoardsTablet.getCount() == 0)) {
            if (DEBUG) Log.i(TAG, "onResume calling restartLoader");
            getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
        }
    }

    protected void setCurrentItemToThreadAsync() {
        //if (mAdapter != null && mAdapter.getCount() > 0) {
        //    if (DEBUG) Log.i(TAG, "setCurrentItemToThreadAsync() /" + boardCode + "/" + threadNo + " adapter already loaded, skipping");
        //    return;
        //}
        if (DEBUG) Log.i(TAG, "setCurrentItemToThreadAsync() /" + boardCode + "/" + threadNo + " selecting current item in pager");
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "onResume() storing activity change");
                    NetworkProfileManager.instance().activityChange(activity);
                }
                int idx = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode).getThreadIndex(boardCode, threadNo);
                if (idx == -1) {
                    if (DEBUG) Log.i(TAG, "onResume() thread not in board, waiting for board refresh");
                }
                else {
                    if (DEBUG) Log.i(TAG, "onResume() set current item to thread");
                    setCurrentItemToThread();
                }
                //To change body of implemented methods use File | Settings | File Templates.
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
        AnalyticsComponent.onStop(this);
    }

    private void postReply(long postNos[]) {
        String replyText = "";
        for (long postNo : postNos) {
            replyText += ">>" + postNo + "\n";
        }
        postReply(replyText);
    }

    private void postReply(String replyText) {
        PostReplyActivity.startActivity(this, boardCode, threadNo, 0, ChanPost.planifyText(replyText));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // menu creation handled at fragment level instead
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.thread_menu, menu);
        //createSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void createSearchView(Menu menu) {
        searchMenuItem = menu.findItem(R.id.search_menu);
        SearchActivity.createSearchView(this, searchMenuItem);
    }
    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo, postNo, query);
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
                if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " message=" + message);
                ChanBoard fragmentBoard = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
                if (mPager == null && !fragmentBoard.defData) {
                    if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " board loaded, creating pager");
                    createPager(fragmentBoard);
                    setCurrentItemToThread();
                    setProgress(false);
                }
                if (mPager == null) {
                    if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null pager");
                    setProgress(false);
                    return;
                }
                if (mAdapter == null) {
                    if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null adapter");
                    setProgress(false);
                    return;
                }
                int current = mPager.getCurrentItem();
                int delta = mPager.getOffscreenPageLimit();
                boolean found = false;
                for (int i = current - delta; i < current + delta + 1; i++) {
                    if (refreshFragmentAtPosition(boardCode, threadNo, i, i == current ? message : null))
                        found = true;
                }
                if (!found) {
                    if (DEBUG) Log.i(TAG, "refreshFragment() no fragment found");
                    ThreadFragment fragment = getCurrentFragment();
                    ChanActivityId activityId = fragment == null ? null : fragment.getChanActivityId();
                    if (fragment == null
                            || activityId == null
                            || activityId.threadNo <= 0) { // recreate, nothing displayed
                        if (DEBUG) Log.i(TAG, "refreshFragment() nothing displayed, recreating pager");
                        createPager(fragmentBoard);
                        if (mAdapter != null && mAdapter.getCount() > 0) {
                            int pos = getCurrentThreadPos();
                            if (pos == -1) {
                                if (DEBUG) Log.i(TAG, "refreshFragment() thread not found in board, setting pos to 0");
                                pos = 0;
                            }
                            if (DEBUG) Log.i(TAG, "refreshFragment() setting item to pos=" + pos);
                            mPager.setCurrentItem(pos, false);
                            ThreadFragment fragment2;
                            if ((fragment2 = getFragmentAtPosition(pos)) != null
                                    && fragment2.getChanActivityId() != null
                                    && fragment2.getChanActivityId().threadNo > 0)
                                fragment2.refreshThread(null);
                        }
                        else {
                            if (DEBUG) Log.i(TAG, "refreshFragment() empty adapter, skipping fragment refresh");
                        }
                    }
                }
            }
        }).start();
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
        return handler;
    }

    @Override
    protected void createActionBar() {
        super.createActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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
            this.count = board.threads.length;
            notifyDataSetChanged();
        }
        public void setQuery(String query) {
            this.query = query;
        }
        @Override
        public void notifyDataSetChanged() {
            board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
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
                primaryItem.fetchIfNeeded();
                //primaryItem.tryFetchThread(); // update if necessary
                /*
                ChanActivityId activityId = fragment.getChanActivityId();
                if (activityId != null
                        && activityId.boardCode != null
                        && !activityId.boardCode.isEmpty()
                        && activityId.threadNo > 0
                        ) { // different activity
                    // only change if thread doesn't exist in board
                    if (board.getThreadIndex(boardCode, threadNo) == -1) {
                        boardCode = activityId.boardCode;
                        threadNo = activityId.threadNo;
                        if (DEBUG) Log.i(TAG, "setPrimaryItem set activity to /" + boardCode + "/" + threadNo);
                    }
                }
                */
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
        if (mPullToRefreshAttacher != null && !on) {
            if (DEBUG) Log.i(TAG, "mPullToRefreshAttacher.setRefreshComplete()");
            mPullToRefreshAttacher.setRefreshComplete();
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
        adapterBoardsTablet = new BoardGridSmallTabletColCursorAdapter(this, viewBinder);
        boardGrid.setAdapter(adapterBoardsTablet);
        boardGrid.setOnItemClickListener(boardGridListener);
        boardGrid.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected void onBoardsTabletLoadFinished(Cursor data) {
        if (boardGrid == null)
            createAbsListView();
        this.adapterBoardsTablet.swapCursor(data);
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
            //if (boardGrid instanceof ListView)
            //    ((ListView)boardGrid).setSelectionFromTop(firstVisibleBoardPosition, firstVisibleBoardPositionOffset);
            //else
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
                boardGrid.setSelection(pos);
            }
            else {
                if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " thread not found");
            }
        }
        //setProgress(false);
    }

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ id=" + id);
            //setProgress(true);
            return new BoardCursorLoader(getActivityContext(), boardCode, "", true);
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
            adapterBoardsTablet.swapCursor(null);
        }
    };

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int options = wideTablet ? 0 : BoardGridViewer.SMALL_GRID;
            return BoardGridViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight, null, null, options);
        }
    };

    protected AdapterView.OnItemClickListener boardGridListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
                ActivityDispatcher.launchUrlInBrowser(getActivityContext(), clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), ThreadFragment.TAG);
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
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
        if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/ recreating pager");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
                if (board.defData)
                    return;
                createPagerAndSetCurrentItemAsync(board);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private boolean warnedAboutNetworkDown = false;

    public boolean warnedAboutNetworkDown() {
        return warnedAboutNetworkDown;
    }

    public void warnedAboutNetworkDown(boolean set) {
        warnedAboutNetworkDown = set;
    }

}