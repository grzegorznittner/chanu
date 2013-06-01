package com.chanapps.four.activity;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.UserStatistics.ChanFeature;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.ThreadListener;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.*;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadActivity
        extends AbstractDrawerActivity
        implements ChanIdentifiedActivity,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder,
        ActionMode.Callback,
        MediaScannerConnection.OnScanCompletedListener {

    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final int MAX_HTTP_GET_URL_LEN = 2000;
    protected static final int THREAD_DONE = 0x1;
    protected static final int BOARD_DONE = 0x2;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 250;

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected long tim;
    protected String boardCode;
    protected String query = "";
    protected int columnWidth = 0;
    protected int columnHeight = 0;
    protected MenuItem searchMenuItem;
    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected boolean shouldPlayThread = false;
    protected ShareActionProvider shareActionProvider = null;
    protected Map<String, Uri> checkedImageUris = new HashMap<String, Uri>(); // used for tracking what's in the media store
    protected ActionMode actionMode = null;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected BoardCursorLoader cursorLoaderBoardsTablet;
    protected AbsListView absBoardListView;
    protected int loadingStatusFlags = 0;

    protected ThreadListener threadListener;

    public static void startActivity(Activity from, String boardCode, long threadNo, String query) {
        if (threadNo <= 0)
            BoardActivity.startActivity(from, boardCode, query);
        else
            from.startActivity(createIntentForActivity(from, boardCode, threadNo, query));
    }

    public static Intent createIntentForActivity(Context context, final String boardCode, final long threadNo, String query) {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return false; // always jump to board
    }

    @Override
    protected void createViews(Bundle bundle) {
        createAbsListView();
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (DEBUG) Log.i(TAG, "onCreate /" + boardCode + "/" + threadNo + " q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            redirectToBoardSelector();
        else if (threadNo <= 0)
            redirectToBoard();
    }

    protected void redirectToBoardSelector() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board selector");
        Intent selectorIntent = new Intent(this, BoardSelectorActivity.class);
        selectorIntent.putExtra(ChanHelper.BOARD_TYPE, BoardType.JAPANESE_CULTURE.toString());
        selectorIntent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
        selectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(selectorIntent);
        finish();
    }

    protected void redirectToBoard() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board /" + boardCode + "/");
        Intent intent = BoardActivity.createIntentForActivity(this, boardCode, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
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
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            String uriThreadNo = params.get(1);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null && uriThreadNo != null) {
                boardCode = uriBoardCode;
                threadNo = Long.valueOf(uriThreadNo);
                query = "";
                if (DEBUG) Log.i(TAG, "loaded /" + boardCode + "/" + threadNo + " from url intent");
            }
            else {
                boardCode = ChanBoard.DEFAULT_BOARD_CODE;
                threadNo = 0;
                query = "";
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/" + threadNo);
    }

    public Cursor getCursor() {
        if (adapter != null)
            return adapter.getCursor();
        else
            return null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ id=" + id);
        if (id == 0) {
            if (threadNo > 0) {
                loadingStatusFlags &= ~THREAD_DONE;
                cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, query, absBoardListView == null);
                if (DEBUG) Log.i(TAG, "Started loader for thread /" + boardCode + "/" + threadNo);
                setProgressBarIndeterminateVisibility(true);
            } else {
                cursorLoader = null;
                setProgressBarIndeterminateVisibility(false);
            }
            return cursorLoader;
        } else {
            loadingStatusFlags &= ~BOARD_DONE;
            if (DEBUG) Log.i(TAG, "Started loader for board /" + boardCode + "/");
            cursorLoaderBoardsTablet = new BoardCursorLoader(this, boardCode, "");
            setProgressBarIndeterminateVisibility(true);
            return cursorLoaderBoardsTablet;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new LoaderHandler();
        threadListener = new ThreadListener(getSupportFragmentManager(), absListView, adapter, handler);
        setActionBarTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new LoaderHandler();
        invalidateOptionsMenu(); // for correct spinner display
        NetworkProfileManager.instance().activityChange(this);
        if (absBoardListView != null)
            getSupportLoaderManager().restartLoader(1, null, this); // board loader for tablet view
        getSupportLoaderManager().restartLoader(0, null, this); // thread loader
        new TutorialOverlay(layout, tutorialPage());
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
        if (absListView != null)
            getLoaderManager().destroyLoader(0);
        if (absBoardListView != null)
            getLoaderManager().destroyLoader(1);
        handler = null;
    }

    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(this, this);
        absListView.setAdapter(adapter);
        if (absBoardListView != null && absBoardListView instanceof GridView) {
            adapterBoardsTablet = new BoardGridCursorAdapter(this, this, columnWidth, columnHeight);
            absBoardListView.setAdapter(adapterBoardsTablet);
        }
    }

    protected void initAbsListView() {
        absListView = (ListView) findViewById(R.id.thread_list_view);
        absBoardListView = (GridView) findViewById(R.id.board_grid_view_tablet);
        if (absBoardListView != null)
            sizeTabletGridToDisplay();
    }

    protected void sizeTabletGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absBoardListView, display, ChanGridSizer.ServiceType.BOARDTHREAD);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
        ViewGroup.LayoutParams params = absBoardListView.getLayoutParams();
        params.width = columnWidth; // 1-column-wide, no padding
    }

    protected void onThreadLoadFinished(Cursor data) {
        adapter.swapCursor(data);
        if (DEBUG) Log.i(TAG, "listview count=" + absListView.getCount());
        // retry load if maybe data wasn't there yet
        ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
        if ((data == null || data.getCount() < 1
                || (thread != null && thread.replies > 0 && !thread.isDead && data.getCount() <= 2))
                && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                stopProgressBarIfLoadersDone();
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        } else {
            loadingStatusFlags |= THREAD_DONE;
            stopProgressBarIfLoadersDone();
        }
    }

    protected void onBoardsTabletLoadFinished(Cursor data) {
        this.adapterBoardsTablet.swapCursor(data);
        // retry load if maybe data wasn't there yet
        if (data != null && data.getCount() < 1 && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                stopProgressBarIfLoadersDone();
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        } else {
            loadingStatusFlags |= BOARD_DONE;
            stopProgressBarIfLoadersDone();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/ id=" + loader.getId()
                + " count=" + (data == null ? 0 : data.getCount()));
        if (loader == this.cursorLoader)
            onThreadLoadFinished(data);
        else if (loader == this.cursorLoaderBoardsTablet)
            onBoardsTabletLoadFinished(data);
    }

    protected void stopProgressBarIfLoadersDone() {
        if (absBoardListView == null && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgressBarIndeterminateVisibility(false);
        else if (absBoardListView != null && (loadingStatusFlags & BOARD_DONE) > 0 && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgressBarIndeterminateVisibility(false);
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        // we don't use fragments, but create anything needed
        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        layout = View.inflate(getApplicationContext(), getLayoutId(), null);
        contentFrame.addView(layout);
        initAbsListView();
        initAdapter();
        setupContextMenu();
        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        absListView.setOnItemClickListener(threadItemListener);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        if (absBoardListView != null) {
            absBoardListView.setOnItemClickListener(absBoardListViewListener);
            absBoardListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        }
    }

    protected AbsListView.OnItemClickListener threadItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "onItemClick pos=" + position + " flags=" + flags + " view=" + view);
            if ((flags & ChanPost.FLAG_IS_AD) > 0)
                itemAdListener.onClick(view);
            else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
                itemTitleListener.onClick(view);
            else if ((flags & ChanPost.FLAG_IS_THREADLINK) > 0)
                itemThreadLinkListener.onClick(view);
            else if ((flags & ChanPost.FLAG_IS_BOARDLINK) > 0)
                itemBoardLinkListener.onClick(view);
        }
    };

    protected void setupContextMenu() {
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        absListView.setOnCreateContextMenuListener(this);
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        return ThreadViewer.setViewValue(view, cursor, boardCode,
                isTablet(),
                threadListener.imageOnClickListener,
                threadListener.backlinkOnClickListener,
                threadListener.repliesOnClickListener,
                threadListener.sameIdOnClickListener,
                threadListener.exifOnClickListener,
                startActionModeListener);
    }

    protected File fullSizeImageFile(Cursor cursor) {
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String ext = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(getApplicationContext(), boardCode, postNo, ext);
        File localImage = new File(URI.create(uri.toString()));
        if (localImage != null && localImage.exists() && localImage.canRead() && localImage.length() > 0)
            return localImage;
        else
            return null;
    }

    protected void setAbsListViewClass() {
        absListViewClass = ListView.class;
    }

    private void postReply(long postNos[]) {
        String replyText = "";
        for (long postNo : postNos) {
            replyText += ">>" + postNo + "\n";
        }
        postReply(replyText);
    }

    private void postReply(String replyText) {
        Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
        replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        replyIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        replyIntent.putExtra(ChanPost.POST_NO, 0);
        replyIntent.putExtra(ChanHelper.TEXT, ChanPost.planifyText(replyText));
        startActivity(replyIntent);
    }

/*
    protected boolean navigateUp() {
        // FIXME: know that I'm coming from watching and return there
        Intent upIntent = new Intent(this, BoardActivity.class);
        upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        if (DEBUG) Log.i(TAG, "Made up intent with board=" + boardCode);
//                if (NavUtils.shouldUpRecreateTask(this, upIntent)) { // needed when calling from widget
//                    if (DEBUG) Log.i(TAG, "Should recreate task");
        TaskStackBuilder.create(this).addParentStack(this).startActivities();
        this.finish();
//                }
//                else {
//                    if (DEBUG) Log.i(TAG, "Navigating up...");
//                    NavUtils.navigateUpTo(this, upIntent);
//                }
        return true;
    }
*/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem playMenuItem = menu.findItem(R.id.play_thread_menu);
        if (playMenuItem != null)
            synchronized (this) {
                playMenuItem.setIcon(shouldPlayThread ? R.drawable.av_stop : R.drawable.av_play);
                playMenuItem.setTitle(shouldPlayThread ? R.string.play_thread_stop_menu : R.string.play_thread_menu);
            }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.post_reply_menu:
                postReply("");
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                return true;
            case R.id.download_all_images_menu:
                ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.play_thread_menu:
                return playThreadMenu();
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
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
                        BoardGroupFragment.refreshWatchlist();
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
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo);
    }

    protected int getLayoutId() {
        return R.layout.thread_list_layout;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (DEBUG) Log.i(TAG, "onCreateActionMode");
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thread_context_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.thread_context_share_action_menu);
        if (shareItem != null) {
            shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        } else {
            shareActionProvider = null;
        }

        mode.setTitle(R.string.thread_context_select);
        actionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (DEBUG) Log.i(TAG, "onPrepareActionMode");
        updateSharedIntent();
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        long[] postNos = absListView.getCheckedItemIds();
        SparseBooleanArray postPos = absListView.getCheckedItemPositions();
        if (postNos.length == 0) {
            Toast.makeText(getApplicationContext(), R.string.thread_no_posts_selected, Toast.LENGTH_SHORT).show();
            return false;
        }

        switch (item.getItemId()) {
            case R.id.post_reply_all_menu:
                if (DEBUG) Log.i(TAG, "Post nos: " + Arrays.toString(postNos));
                postReply(postNos);
                return true;
            case R.id.post_reply_all_quote_menu:
                String quotesText = selectQuoteText(postPos);
                postReply(quotesText);
                return true;
            case R.id.copy_text_menu:
                String selectText = selectText(postPos);
                copyToClipboard(selectText);
                //(new SelectTextDialogFragment(text)).show(getSupportFragmentManager(), SelectTextDialogFragment.TAG);
                return true;
            case R.id.download_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null, postNos);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.image_search_menu:
                imageSearch(postPos, IMAGE_SEARCH_ROOT);
                return true;
            case R.id.anime_image_search_menu:
                imageSearch(postPos, IMAGE_SEARCH_ROOT_ANIME);
                return true;
            case R.id.translate_posts_menu:
                return translatePosts(postPos);
            case R.id.delete_posts_menu:
                (new DeletePostDialogFragment(boardCode, threadNo, postNos))
                        .show(getSupportFragmentManager(), DeletePostDialogFragment.TAG);
                return true;
            case R.id.report_posts_menu:
                (new ReportPostDialogFragment(boardCode, threadNo, postNos))
                        .show(getSupportFragmentManager(), ReportPostDialogFragment.TAG);
                return true;
            case R.id.block_posts_menu:
                Map<ChanBlocklist.BlockType, List<String>> blocklist = extractBlocklist(postPos);
                (new BlocklistSelectToAddDialogFragment(blocklist)).show(getFragmentManager(), TAG);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        SparseBooleanArray positions = absListView.getCheckedItemPositions();
        if (DEBUG) Log.i(TAG, "onDestroyActionMode checked size=" + positions.size());
        for (int i = 0; i < absListView.getCount(); i++) {
            if (positions.get(i)) {
                absListView.setItemChecked(i, false);
            }
        }
        actionMode = null;
    }

    protected String selectText(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor) adapter.getItem(i);
            if (cursor == null)
                continue;
            String subject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_SUBJECT));
            if (subject != null && !subject.isEmpty())
                text += (text.isEmpty() ? "" : "\n") + subject;
            String message = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_TEXT));
            if (message != null && !message.isEmpty())
                text += (text.isEmpty() ? "" : "\n") + message;
            if (text.isEmpty()) {
                subject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
                if (subject != null && !subject.isEmpty())
                    text += (text.isEmpty() ? "" : "\n") + subject;
                message = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
                if (message != null && !message.isEmpty())
                    text += (text.isEmpty() ? "" : "\n") + message;
            }
            if (!text.isEmpty())
                text += "<br/><br/>";
        }
        return text;
    }

    protected String selectQuoteText(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor) adapter.getItem(i);
            if (cursor == null)
                continue;
            String postNo = cursor.getString(cursor.getColumnIndex(ChanPost.POST_ID));
            String itemText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            if (itemText == null)
                itemText = "";
            String postPrefix = ">>" + postNo + "<br/>";
            text += (text.isEmpty() ? "" : "<br/><br/>") + postPrefix + ChanPost.quoteText(itemText);
        }
        if (DEBUG) Log.i(TAG, "Selected quote text: " + text);
        return text;
    }

    protected void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                getApplicationContext().getString(R.string.app_name),
                ChanPost.planifyText(text));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
    }

    protected boolean isTablet() {
        return absBoardListView != null;
    }

    protected AdapterView.OnItemClickListener absBoardListViewListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
                ChanHelper.launchUrlInBrowser(ThreadActivity.this, clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), ThreadActivity.TAG);
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                BoardActivity.startActivity(ThreadActivity.this, boardLink, "");
            }
            else {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                final long threadNoLink = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (boardCode.equals(boardLink) && threadNo == threadNoLink) { // already on this, do nothing
                } else if (boardCode.equals(boardLink)) { // just redisplay right tab
                    threadNo = threadNoLink;
                    refreshThread();
                    NetworkProfileManager.instance().activityChange(ThreadActivity.this);
                } else {
                    startActivity(ThreadActivity.this, boardLink, threadNoLink, "");
                }
            }
        }
    };

  protected View.OnClickListener itemAdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            String adUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            if (adUrl != null && !adUrl.isEmpty())
                ChanHelper.launchUrlInBrowser(ThreadActivity.this, adUrl);
        }
    };

    protected View.OnClickListener itemTitleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            if ((flags & ChanPost.FLAG_IS_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), ThreadActivity.TAG);
            }

        }
    };

    protected View.OnClickListener itemThreadLinkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            String linkedBoardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
            long linkedThreadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            absListView.setItemChecked(pos, false); // gets checked for some reason
            if (linkedBoardCode == null || linkedBoardCode.isEmpty() || linkedThreadNo <= 0)
                return;
            if (absBoardListView != null && boardCode.equals(linkedBoardCode)) {
                threadNo = linkedThreadNo;
                refresh();
            } else {
                ThreadActivity.startActivity(ThreadActivity.this, linkedBoardCode, linkedThreadNo, "");
            }
        }
    };

    protected View.OnClickListener itemBoardLinkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            String linkedBoardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
            absListView.setItemChecked(pos, false); // gets checked for some reason
            if (linkedBoardCode != null && !linkedBoardCode.isEmpty())
                BoardActivity.startActivity(ThreadActivity.this, linkedBoardCode, "");
        }
    };

    protected View.OnLongClickListener startActionModeListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int pos = absListView.getPositionForView(v);
            if (DEBUG) Log.i(TAG, "received item click pos: " + pos);

            View itemView = null;
            for (int i = 0; i < absListView.getChildCount(); i++) {
                View child = absListView.getChildAt(i);
                if (absListView.getPositionForView(child) == pos) {
                    itemView = child;
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "found itemView=" + itemView);
            if (itemView == null)
                return false;

            startActionMode(ThreadActivity.this);
            absListView.setItemChecked(pos, true);
            return true;
        }
    };

    public void setActionBarTitle() {
        /*
        if (query != null && !query.isEmpty()) {
            String title = String.format(getString(R.string.search_results_title_thread), boardCode, threadNo);
            getActionBar().setTitle(title);
            return;
        }
        */
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        String boardTitle = (board == null ? "Board" : board.name) + " /" + boardCode + "/";
        getActionBar().setTitle(boardTitle);
        ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
        String threadTitle = (thread == null || thread.posts == null || thread.posts.length == 0 || thread.posts[0] == null)
                ? null
                : thread.posts[0].threadSubject(getApplicationContext())
                .replaceAll("<br/?>", " ")
                .replaceAll("<[^>]*>", "");
        if (threadTitle == null || threadTitle.isEmpty() || threadTitle.trim().isEmpty())
            threadTitle = String.valueOf(threadNo);
        else
            threadTitle = " " + threadTitle.trim();
        getActionBar().setTitle("/" + boardCode + "/" + threadTitle);
    }

    protected Map<ChanBlocklist.BlockType, List<String>> extractBlocklist(SparseBooleanArray postPos) {
        Map<ChanBlocklist.BlockType, List<String>> blocklist = new HashMap<ChanBlocklist.BlockType, List<String>>();
        List<String> tripcodes = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> emails = new ArrayList<String>();
        List<String> userIds = new ArrayList<String>();
        if (adapter == null)
            return blocklist;
        Cursor cursor = adapter.getCursor();
        if (cursor == null)
            return blocklist;

        for (int i = 0; i < adapter.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            if (!cursor.moveToPosition(i))
                continue;
            String tripcode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TRIPCODE));
            if (tripcode != null && !tripcode.isEmpty())
                tripcodes.add(tripcode);
            String name = cursor.getString(cursor.getColumnIndex(ChanPost.POST_NAME));
            if (name != null && !name.isEmpty() && !name.equals("Anonymous"))
                names.add(name);
            String email = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EMAIL));
            if (email != null && !email.isEmpty() && !email.equals("sage"))
                emails.add(email);
            String userId = cursor.getString(cursor.getColumnIndex(ChanPost.POST_USER_ID));
            if (userId != null && !userId.isEmpty() && !userId.equals("Heaven"))
                userIds.add(userId);
        }
        if (tripcodes.size() > 0)
            blocklist.put(ChanBlocklist.BlockType.TRIPCODE, tripcodes);
        if (names.size() > 0)
            blocklist.put(ChanBlocklist.BlockType.NAME, names);
        if (emails.size() > 0)
            blocklist.put(ChanBlocklist.BlockType.EMAIL, emails);
        if (userIds.size() > 0)
            blocklist.put(ChanBlocklist.BlockType.ID, userIds);

        return blocklist;
    }

    protected boolean translatePosts(SparseBooleanArray postPos) {
        final Locale locale = getResources().getConfiguration().locale;
        final String localeCode = locale.getLanguage();
        final String text = selectText(postPos);
        final String strippedText = text.replaceAll("<br/?>", "\n").replaceAll("<[^>]*>", "").trim();
        String escaped;
        try {
            escaped = URLEncoder.encode(strippedText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding utf-8? You crazy!", e);
            escaped = strippedText;
        }
        if (escaped.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.translate_no_text, Toast.LENGTH_SHORT);
            return true;
        }
        String translateUrl = GOOGLE_TRANSLATE_ROOT + localeCode + "&text=" + escaped;
        if (translateUrl.length() > MAX_HTTP_GET_URL_LEN)
            translateUrl = translateUrl.substring(0, MAX_HTTP_GET_URL_LEN);
        ChanHelper.launchUrlInBrowser(this, translateUrl);
        return true;
    }

    protected boolean playThreadMenu() {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.PLAY_THREAD);
        synchronized (this) {
            shouldPlayThread = !shouldPlayThread; // user clicked, invert play status
            invalidateOptionsMenu();
            if (!shouldPlayThread) {
                return false;
            }
            if (!canPlayThread()) {
                shouldPlayThread = false;
                Toast.makeText(this, R.string.thread_no_start_play, Toast.LENGTH_SHORT).show();
                return false;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                absListView.setFastScrollEnabled(false);
                            }
                        });
                    while (true) {
                        synchronized (this) {
                            if (!canPlayThread())
                                break;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (absListView == null || adapter == null)
                                        return;
                                    /*
                                    int first = absListView.getFirstVisiblePosition();
                                    int last = absListView.getLastVisiblePosition();
                                    for (int pos = first; pos <= last; pos++)
                                        expandVisibleItem(first, pos);
                                    */
                                    absListView.smoothScrollBy(2, 25);
                                }
                                /*
                                private void expandVisibleItem(int first, int pos) {
                                    View listItem = absListView.getChildAt(pos - first);
                                    View image = listItem == null ? null : listItem.findViewById(R.id.list_item_image);
                                    Cursor cursor = adapter.getCursor();
                                    //if (DEBUG) Log.i(TAG, "pos=" + pos + " listItem=" + listItem + " expandButton=" + expandButton);
                                    if (listItem != null
                                            && image != null
                                            && image.getVisibility() == View.VISIBLE
                                            && image.getHeight() > 0
                                            && cursor.moveToPosition(pos))
                                    {
                                        long id = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                                        absListView.performItemClick(image, pos, id);
                                    }
                                }
                                */
                            });
                        }
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    synchronized (this) {
                        shouldPlayThread = false;
                    }
                    if (handler != null)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                absListView.setFastScrollEnabled(true);
                                invalidateOptionsMenu();
                            }
                        });
                }
            }).start();
        }
        return true;
    }

    protected boolean canPlayThread() {
        if (shouldPlayThread == false)
            return false;
        if (absListView == null || adapter == null || adapter.getCount() <= 0)
            return false;
        //if (absListView.getLastVisiblePosition() == adapter.getCount() - 1)
        //    return false; // stop
        //It is scrolled all the way down here
        if (absListView.getLastVisiblePosition() >= absListView.getAdapter().getCount() - 1)
            return false;
        if (handler == null)
            return false;
        return true;
    }

    private void setShareIntent(final Intent intent) {
        if (ChanHelper.onUIThread())
            synchronized (this) {
                if (shareActionProvider != null && intent != null)
                    shareActionProvider.setShareIntent(intent);
            }
        else if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (shareActionProvider != null && intent != null)
                            shareActionProvider.setShareIntent(intent);
                    }
                }
            });
    }

    public void updateSharedIntent() {
        SparseBooleanArray postPos = absListView.getCheckedItemPositions();
        String extraText = selectText(postPos);
        if (extraText != null && !extraText.isEmpty())
            text += "\n\n" + extraText.replaceAll("</?br/?>", "\n").replaceAll("<[^>]*>", "");
        ArrayList<String> paths = new ArrayList<String>();
        Cursor cursor = adapter.getCursor();
        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i) || !cursor.moveToPosition(i))
                continue;
            File file = fullSizeImageFile(cursor); // try for full size first
            if (file == null) { // if can't find it, fall back to thumbnail
                String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL)); // thumbnail
                if (DEBUG) Log.i(TAG, "Couldn't find full image, falling back to thumbnail=" + url);
                file = (url == null || url.isEmpty()) ? null : imageLoader.getDiscCache().get(url);
            }
            if (file == null || !file.exists() || !file.canRead() || file.length() <= 0)
                continue;
            paths.add(file.getAbsolutePath());
        }
        Intent intent;
        if (paths.size() == 0) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setType("text/html");
            setShareIntent(intent);
        } else {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            ArrayList<String> missingPaths = new ArrayList<String>();
            for (String path : paths) {
                if (checkedImageUris.containsKey(path)) {
                    Uri uri = checkedImageUris.get(path);
                    uris.add(uri);
                    if (DEBUG) Log.i(TAG, "Added uri=" + uri);
                } else {
                    uris.add(Uri.fromFile(new File(path)));
                    missingPaths.add(path);
                }
            }
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setType("image/jpeg");
            setShareIntent(intent);
            if (missingPaths.size() > 0) {
                if (DEBUG) Log.i(TAG, "launching scanner for missing paths count=" + missingPaths.size());
                asyncUpdateSharedIntent(missingPaths);
            }
        }
    }

    protected void asyncUpdateSharedIntent(ArrayList<String> pathList) {
        String[] paths = new String[pathList.size()];
        String[] types = new String[pathList.size()];
        for (int i = 0; i < pathList.size(); i++) {
            paths[i] = pathList.get(i);
            types[i] = "image/jpeg";
        }
        MediaScannerConnection.scanFile(getApplicationContext(), paths, types, this);
    }

    public void onScanCompleted(String path, Uri uri) {
        if (DEBUG) Log.i(TAG, "Scan completed for path=" + path + " result uri=" + uri);
        if (uri == null)
            uri = Uri.parse(path);
        checkedImageUris.put(path, uri);
        updateSharedIntent();
    }

    @Override
    public void refresh() {
        invalidateOptionsMenu(); // in case spinner needs to be reset
        refreshBoard(); // for tablets
        refreshThread();
        if (actionMode != null)
            actionMode.finish();
    }

    public void refreshBoard() { /* for tablets */
        if (handler != null && absBoardListView != null)
            handler.sendEmptyMessageDelayed(1, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

    public void refreshThread() {
        if (handler != null)
            handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

    protected TutorialOverlay.Page tutorialPage() {
        return TutorialOverlay.Page.THREAD;
    }

    private class LoaderHandler extends Handler {
        public LoaderHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        if (DEBUG) Log.i(TAG, ">>>>>>>>>>> restart message received restarting loader");
                        getSupportLoaderManager().restartLoader(1, null, ThreadActivity.this);
                        break;

                    default:
                        if (DEBUG) Log.i(TAG, ">>>>>>>>>>> restart message received restarting loader");
                        getSupportLoaderManager().restartLoader(0, null, ThreadActivity.this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
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
    public void onLoaderReset(Loader<Cursor> loader) {
        if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ id=" + loader.getId());
        adapter.swapCursor(null);
    }

    @Override
    public void startProgress() {
        Handler handler = getChanHandler();
        if (handler != null)
            setProgressBarIndeterminateVisibility(true);
    }

    private static final String IMAGE_SEARCH_ROOT = "http://tineye.com/search?url=";
    private static final String IMAGE_SEARCH_ROOT_ANIME = "http://iqdb.org/?url=";

    private void imageSearch(SparseBooleanArray postPos, String rootUrl) {
        String imageUrl = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor) adapter.getItem(i);
            if (cursor == null)
                continue;
            int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if ((flags & ChanPost.FLAG_HAS_IMAGE) == 0)
                continue;
            String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_FULL_IMAGE_URL));
            if (url == null || url.isEmpty())
                continue;
            imageUrl = url;
            break;
        }
        if (imageUrl.isEmpty()) {
            Toast.makeText(this, R.string.full_screen_image_search_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8");
            String url =  rootUrl + encodedImageUrl;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't do image search imageUrl=" + imageUrl, e);
            Toast.makeText(this, R.string.full_screen_image_search_error, Toast.LENGTH_SHORT).show();
        }
    }

}
