package com.chanapps.four.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;

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
    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    public static final boolean DEBUG = true;
    protected static final int OFFSCREEN_PAGE_LIMIT = 0;

    protected ChanBoard board;
    protected ThreadPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected Handler handler;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected long postNo; // for direct jumps from latest post / recent images

    public static void startActivity(Context from, String boardCode, long threadNo, String query) {
        startActivity(from, boardCode, threadNo, 0, query);
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postNo, String query) {
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
        if (DEBUG) Log.i(TAG, "onCreate /" + boardCode + "/" + threadNo + " q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.META_BOARD_CODE;
        if (threadNo <= 0)
            redirectToBoard();

        createPager();
    }

    protected void createPager() {
        board = ChanFileStorage.loadBoardData(this, boardCode);
        mAdapter = new ThreadPagerAdapter(getSupportFragmentManager());
        mAdapter.setBoard(board);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        mPager.setAdapter(mAdapter);
        setCurrentItemToThread();
    }

    protected void setCurrentItemToThread() {
        int pos = getCurrentThreadPos();
        if (pos != mPager.getCurrentItem() && pos >= 0 && pos < mAdapter.getCount())
            mPager.setCurrentItem(pos, false);
    }

    protected int getCurrentThreadPos() {
        return board.getThreadIndex(boardCode, threadNo);
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
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putLong(ChanThread.THREAD_NO, threadNo);
        savedInstanceState.putString(SearchManager.QUERY, query);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
        threadNo = savedInstanceState.getLong(ChanThread.THREAD_NO, 0);
        query = savedInstanceState.getString(SearchManager.QUERY);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/" + threadNo);
        if (board == null || !board.link.equals(boardCode))
            createPager();
        else
            setCurrentItemToThread();
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
        if (data == null) {
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
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        invalidateOptionsMenu(); // for correct spinner display
        NetworkProfileManager.instance().activityChange(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/" + threadNo);
        handler = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/" + threadNo);
        handler = null;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = BoardActivity.createIntent(this, boardCode, "");
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.refresh_menu:
                setProgress(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.post_reply_menu:
                postReply("");
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.download_all_images_menu:
                ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.web_menu:
                String url = ChanThread.threadUrl(boardCode, threadNo);
                ChanHelper.launchUrlInBrowser(this, url);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_RULES);
        int boardRulesId = R.raw.global_rules_detail;
        try {
            boardRulesId = R.raw.class.getField("board_" + boardCode + "_rules").getInt(null);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't find rules for board:" + boardCode);
        }
        RawResourceDialog rawResourceDialog
                = new RawResourceDialog(this, R.layout.board_rules_dialog, R.raw.board_rules_header, boardRulesId);
        rawResourceDialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_menu, menu);
        searchMenuItem = menu.findItem(R.id.search_menu);
        SearchActivity.createSearchView(this, searchMenuItem);
        return super.onCreateOptionsMenu(menu);
    }

    protected void addToWatchlist() {
        final Context context = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't add null thread /" + boardCode + "/" + threadNo + " to watchlist");
                        msgId = R.string.thread_not_added_to_watchlist;
                    }
                    else {
                        ChanFileStorage.addWatchedThread(context, thread);
                        BoardActivity.refreshWatchlist();
                        msgId = R.string.thread_added_to_watchlist;
                        if (DEBUG) Log.i(TAG, "Added /" + boardCode + "/" + threadNo + " to watchlist");
                    }
                }
                catch (IOException e) {
                    msgId = R.string.thread_not_added_to_watchlist;
                    Log.e(TAG, "Exception adding /" + boardCode + "/" + threadNo + " to watchlist", e);
                }
                final int stringId = msgId;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), stringId, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo, postNo, query);
    }

    protected void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                getApplicationContext().getString(R.string.app_name),
                ChanPost.planifyText(text));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void refresh() {
        invalidateOptionsMenu(); // in case spinner needs to be reset
        ThreadFragment fragment = getCurrentFragment();
        //ThreadFragment fragment = (ThreadFragment)getSupportFragmentManager().findFragmentByTag(fragmentTag());
        if (fragment != null)
            fragment.refresh();
    }

    public void refreshFragment(String boardCode, long threadNo) {
        if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo);
        if (mPager == null) {
            if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null pager");
            return;
        }
        if (mAdapter == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " skipping, null adapter");
            return;
        }
        int current = mPager.getCurrentItem();
        int delta = mPager.getOffscreenPageLimit();
        for (int i = current - delta; i < current + delta + 1; i++)
            refreshFragmentAtPosition(boardCode, threadNo, i);
    }

    protected void refreshFragmentAtPosition(String boardCode, long threadNo, int pos) {
        ThreadFragment fragment;
        ChanActivityId data;
        if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos);
        if (pos < 0 || pos >= mAdapter.getCount()) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                + " out of bounds, skipping");
            return;
        }
        if ((fragment = getFragmentAtPosition(pos)) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null fragment at position, skipping");
            return;
        }
        if ((data = fragment.getChanActivityId()) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null getChanActivityId(), skipping");
            return;
        }
        if (data.boardCode == null || !data.boardCode.equals(boardCode) || data.threadNo != threadNo) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " unmatching data=/" + data.boardCode + "/" + data.threadNo + ", skipping");
            return;
        }
        if (DEBUG) Log.i(TAG, "refreshing fragment /" + boardCode + "/" + threadNo + " pos=" + pos);
        fragment.refreshThread();
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

    public static class ThreadPagerAdapter extends FragmentStatePagerAdapter {
        protected ChanBoard board;
        protected Map<Integer,WeakReference<ThreadFragment>> fragments
                = new HashMap<Integer, WeakReference<ThreadFragment>>();
        public ThreadPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        public void setBoard(ChanBoard board) {
            if (board == null || board.threads == null)
                throw new UnsupportedOperationException("can't start pager with null board or null threads");
            this.board = board;
        }
        @Override
        public int getCount() {
            return board.threads.length;
        }
        @Override
        public Fragment getItem(int pos) {
            if (pos < board.threads.length)
                return createFragment(pos);
            else
                return null;
        }
        protected Fragment createFragment(int pos) {
            // get thread
            ChanPost thread = board.threads[pos];
            String boardCode = thread.board;
            long threadNo = thread.no;
            long postNo = 0;
            String query = "";
            // make fragment
            ThreadFragment fragment = new ThreadFragment();
            Bundle bundle = new Bundle();
            bundle.putString(BOARD_CODE, boardCode);
            bundle.putLong(THREAD_NO, threadNo);
            bundle.putLong(POST_NO, postNo);
            bundle.putString(SearchManager.QUERY, query);
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

}
