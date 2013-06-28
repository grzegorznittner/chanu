package com.chanapps.four.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.ThreadListener;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadFragment extends Fragment implements ThreadViewable
{

    public static final String TAG = ThreadFragment.class.getSimpleName();
    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    
    public static final boolean DEBUG = true;

    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final int MAX_HTTP_GET_URL_LEN = 2000;
    protected static final int THREAD_DONE = 0x1;
    protected static final int BOARD_DONE = 0x2;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 250;
    protected static final String FIRST_VISIBLE_POSITION = "firstVisiblePosition";
    protected static final String FIRST_VISIBLE_POSITION_OFFSET = "firstVisiblePositionOffset";
    protected static final String FIRST_VISIBLE_BOARD_POSITION = "firstVisibleBoardPosition";
    protected static final String FIRST_VISIBLE_BOARD_POSITION_OFFSET = "firstVisibleBoardPositionOffset";

    protected String boardCode;
    protected long threadNo;

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected String query = "";
    protected int columnWidth = 0;
    protected int columnHeight = 0;
    protected MenuItem searchMenuItem;
    protected long postNo; // for direct jumps from latest post / recent images
    protected String imageUrl;
    protected boolean shouldPlayThread = false;
    protected ShareActionProvider shareActionProvider = null;
    protected Map<String, Uri> checkedImageUris = new HashMap<String, Uri>(); // used for tracking what's in the media store
    protected ActionMode actionMode = null;
    protected int firstVisiblePosition = -1;
    protected int firstVisiblePositionOffset = -1;
    protected int firstVisibleBoardPosition = -1;
    protected int firstVisibleBoardPositionOffset = -1;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected AbsListView absBoardListView;
    protected int loadingStatusFlags = 0;

    protected ThreadListener threadListener;

    protected boolean progressVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        if (bundle == null)
            bundle = getArguments();
        boardCode = bundle.getString(BOARD_CODE);
        threadNo = bundle.getLong(THREAD_NO);
        postNo = bundle.getLong(POST_NO);
        query = bundle.getString(SearchManager.QUERY);
        if (DEBUG) Log.i(TAG, "onCreateView /" + boardCode + "/" + threadNo + " q=" + query);
        layout = inflater.inflate(R.layout.thread_list_layout, viewGroup, false);
        createAbsListView();
        if (onTablet())
            getLoaderManager().initLoader(1, null, loaderCallbacks); // board loader for tablet view
        getLoaderManager().initLoader(0, null, loaderCallbacks);
        return layout;
    }

    protected boolean onTablet() {
        return absBoardListView != null;
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putLong(ChanThread.THREAD_NO, threadNo);
        savedInstanceState.putString(SearchManager.QUERY, query);
        int pos = absListView == null ? -1 : absListView.getFirstVisiblePosition();
        View view = absListView == null ? null : absListView.getChildAt(0);
        int offset = view == null ? 0 : view.getTop();
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION, pos);
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION_OFFSET, offset);
        int boardPos = !onTablet() ? -1 : absBoardListView.getFirstVisiblePosition();
        View boardView = !onTablet() ? null : absBoardListView.getChildAt(0);
        int boardOffset = boardView == null ? 0 : view.getTop();
        savedInstanceState.putInt(FIRST_VISIBLE_BOARD_POSITION, boardPos);
        savedInstanceState.putInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET, boardOffset);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo + " pos=" + pos);
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);
        if (bundle == null)
            return;
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        query = bundle.getString(SearchManager.QUERY);
        firstVisiblePosition = bundle.getInt(FIRST_VISIBLE_POSITION);
        firstVisiblePositionOffset = bundle.getInt(FIRST_VISIBLE_POSITION_OFFSET);
        firstVisibleBoardPosition = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION);
        firstVisibleBoardPositionOffset = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/" + threadNo + " pos=" + firstVisiblePosition);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        threadListener = new ThreadListener(this);
    }

    @Override
    public AbsListView getAbsListView() {
        return absListView;
    }

    @Override
    public ResourceCursorAdapter getAdapter() {
        return adapter;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        if (adapter == null || adapter.getCount() == 0)
            getLoaderManager().restartLoader(0, null, loaderCallbacks);
        if (onTablet() && (adapterBoardsTablet == null || adapterBoardsTablet.getCount() == 0))
            getLoaderManager().restartLoader(1, null, loaderCallbacks); // board loader for tablet view
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/" + threadNo);
        handler = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/" + threadNo);
        if (absListView != null)
            getLoaderManager().destroyLoader(0);
        if (onTablet())
            getLoaderManager().destroyLoader(1);
        handler = null;
    }

    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(getActivityContext(), viewBinder);
        absListView.setAdapter(adapter);
        if (onTablet() && absBoardListView instanceof GridView) {
            adapterBoardsTablet = new BoardGridCursorAdapter(getActivityContext(), viewBinder,
                    columnWidth, columnHeight);
            absBoardListView.setAdapter(adapterBoardsTablet);
        }
    }

    protected void initAbsListView() {
        absListView = (ListView) layout.findViewById(R.id.thread_list_view);
        absBoardListView = (GridView) layout.findViewById(R.id.board_grid_view_tablet);
        if (onTablet())
            sizeTabletGridToDisplay();
    }

    protected void sizeTabletGridToDisplay() {
        columnWidth = ChanGridSizer.getCalculatedWidth(getResources().getDisplayMetrics(),
                getResources().getInteger(R.integer.BoardTabletGridView_numColumns),
                getResources().getDimensionPixelSize(R.dimen.BoardGridViewTablet_spacing));
        columnHeight = 2 * columnWidth;
        ViewGroup.LayoutParams params = absBoardListView.getLayoutParams();
        params.width = columnWidth; // 1-column-wide, no padding
    }

    protected void tryFetchThread() {
        if (DEBUG) Log.i(TAG, "tryFetchThread /" + boardCode + "/" + threadNo);
        if (handler == null) {
            if (DEBUG) Log.i(TAG, "tryFetchThread not in foreground, exiting");
            return;
        }
        NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
        if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
            if (DEBUG) Log.i(TAG, "tryFetchThread bad health, exiting");
            String msg = String.format(getString(R.string.mobile_profile_health_status),
                    health.toString().toLowerCase().replaceAll("_", " "));
            Toast.makeText(getActivityContext(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        ((ThreadActivity)getActivity()).setProgressForFragment(boardCode, threadNo, true);
        FetchChanDataService.scheduleThreadFetchWithPriority(getActivityContext(), boardCode, threadNo);
    }

    protected void onThreadLoadFinished(Cursor data) {
        adapter.swapCursor(data);
        if (DEBUG) Log.i(TAG, "onThreadLoadFinished /" + boardCode + "/" + threadNo + " listView.count=" + absListView.getCount());
        // retry load if maybe data wasn't there yet
        ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
        if (DEBUG) Log.i(TAG, "onThreadLoadFinished /" + boardCode + "/" + threadNo + " thread=" + thread);
        if (data == null) {
            tryFetchThread();
        }
        else if (thread == null) {
            tryFetchThread();
        }
        else if (thread.posts == null) {
            tryFetchThread();
        }
        else if (thread.replies < thread.posts.length && !thread.isDead) {
            tryFetchThread();
        }
        else if (postNo > 0) {
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(-1);
            boolean found = false;
            int pos = 0;
            while (cursor.moveToNext()) {
                long postNoAtPos = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                if (postNoAtPos == postNo) {
                    found = true;
                    break;
                }
                pos++;
            }
            if (found) {
                absListView.setSelection(pos);
                postNo = -1;
            }
            return;
        }
        else if (firstVisiblePosition >= 0) {
            if (absListView instanceof ListView)
                ((ListView)absListView).setSelectionFromTop(firstVisiblePosition, firstVisiblePositionOffset);
            else
                absListView.setSelection(firstVisiblePosition);
            firstVisiblePosition = -1;
            firstVisiblePositionOffset = -1;
        }
        loadingStatusFlags |= THREAD_DONE;
        stopProgressBarIfLoadersDone();
    }

    protected void onBoardsTabletLoadFinished(Cursor data) {
        this.adapterBoardsTablet.swapCursor(data);
        // retry load if maybe data wasn't there yet
        if (data != null && data.getCount() < 1 && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(getActivityContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
        else if (firstVisibleBoardPosition >= 0) {
            if (absBoardListView instanceof ListView)
                ((ListView)absBoardListView).setSelectionFromTop(firstVisibleBoardPosition, firstVisibleBoardPositionOffset);
            else
                absBoardListView.setSelection(firstVisibleBoardPosition);
            firstVisibleBoardPosition = -1;
            firstVisibleBoardPositionOffset = -1;
        }
        else if (threadNo > 0) {
            Cursor cursor = adapter.getCursor();
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
                absBoardListView.setSelection(pos);
            }
        }
        loadingStatusFlags |= BOARD_DONE;
        stopProgressBarIfLoadersDone();
    }

    protected void stopProgressBarIfLoadersDone() {
        if (!onTablet() && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgress(false);
        else if (onTablet() && (loadingStatusFlags & BOARD_DONE) > 0 && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgress(false);
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        // we don't use fragments, but create anything needed
        initAbsListView();
        initAdapter();
        setupContextMenu();
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        absListView.setOnItemClickListener(threadItemListener);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        if (onTablet()) {
            absBoardListView.setOnItemClickListener(absBoardListViewListener);
            absBoardListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        }
    }

    protected AbsListView.OnItemClickListener threadItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
            postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "onItemClick pos=" + position + " postNo=" + postNo + " flags=" + flags + " view=" + view);
            // /updateSharedIntent();
            if ((flags & ChanPost.FLAG_IS_AD) > 0)
                itemAdListener.onClick(view);
            else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
                itemTitleListener.onClick(view);
            else if ((flags & ChanPost.FLAG_IS_BUTTON) > 0)
                itemButtonListener.onClick(view);
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
        PostReplyActivity.startActivity(getActivityContext(), boardCode, threadNo, 0, ChanPost.planifyText(replyText));
    }

    protected View.OnClickListener imagesOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            GalleryViewActivity.startAlbumViewActivity(getActivityContext(), boardCode, threadNo);
        }
    };

    protected View.OnClickListener postReplyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            if (pos < 0)
                return;
            Cursor cursor = adapter.getCursor();
            if (!cursor.moveToPosition(pos))
                return;
            long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            if (postNo == threadNo)
                postNo = 0;
            PostReplyActivity.startActivity(getActivityContext(), boardCode, threadNo, postNo, ChanPost.planifyText(""));
        }
    };

    protected View.OnClickListener expandedImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            if (pos < 0)
                return;
            Cursor cursor = adapter.getCursor();
            if (!cursor.moveToPosition(pos))
                return;
            long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            //if (postNo == threadNo)
            //    postNo = 0;
            if (postNo > 0)
                GalleryViewActivity.startActivity(getActivityContext(), boardCode, threadNo, postNo);
            else
                GalleryViewActivity.startAlbumViewActivity(getActivityContext(), boardCode, threadNo);
        }
    };

/*
    protected boolean navigateUp() {
        // FIXME: know that I'm coming from watching and return there
        Intent upIntent = new Intent(this, BoardActivity.class);
        upIntent.putExtra(BOARD_CODE, boardCode);
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

    protected boolean isThreadPlayable() {
        return adapter != null
                && adapter.getCount() > 0
                && !getLoaderManager().hasRunningLoaders()
                && !progressVisible;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem playMenuItem = menu.findItem(R.id.play_thread_menu);
        if (playMenuItem != null)
            synchronized (this) {
                if (isThreadPlayable()) {
                    playMenuItem.setIcon(shouldPlayThread ? R.drawable.av_stop : R.drawable.av_play);
                    playMenuItem.setTitle(shouldPlayThread ? R.string.play_thread_stop_menu : R.string.play_thread_menu);
                    playMenuItem.setVisible(true);
                }
                else {
                    playMenuItem.setVisible(false);
                }
            }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case android.R.id.home:
                Intent intent = BoardActivity.createIntent(this, boardCode, "");
                NavUtils.navigateUpTo(this, intent);
                return true;
            */
            case R.id.refresh_menu:
                setProgress(true);
                NetworkProfileManager.instance().manualRefresh(getChanActivity());
                return true;
            case R.id.post_reply_menu:
                postReply("");
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.download_all_images_menu:
                ThreadImageDownloadService.startDownloadToBoardFolder(getActivityContext(), boardCode, threadNo);
                Toast.makeText(getActivityContext(), R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getActivityContext(), boardCode, threadNo);
                Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.play_thread_menu:
                return playThreadMenu();
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.web_menu:
                String url = ChanThread.threadUrl(boardCode, threadNo);
                ChanHelper.launchUrlInBrowser(getActivityContext(), url);
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
                = new RawResourceDialog(getActivityContext(),
                R.layout.board_rules_dialog, R.raw.board_rules_header, boardRulesId);
        rawResourceDialog.show();

    }

    protected void addToWatchlist() {
        final Context context = getActivityContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
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
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivityContext(), stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }).start();
    }

    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo, postNo, query);
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
            String message = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_TEXT));
            if (!subject.isEmpty() || !message.isEmpty()) {
                text = subject
                        + (!subject.isEmpty() && !message.isEmpty() ? "<br/>" : "")
                        + message;
                if (DEBUG) Log.i(TAG, "selectText() de-spoilered text=" + text);
            }
            else {
                subject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
                message = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
                text = subject
                        + (!subject.isEmpty() && !message.isEmpty() ? "<br/>" : "")
                        + message;
                if (DEBUG) Log.i(TAG, "selectText() raw text=" + text);
            }
            break;
//            if (!text.isEmpty())
//                text += "<br/><br/>";
        }
        text = text.replaceAll("(</?br/?>)+", "\n").replaceAll("<[^>]*>", "");
        if (DEBUG) Log.i(TAG, "selectText() returning filtered text=" + text);
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
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivityContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                getActivityContext().getString(R.string.app_name),
                ChanPost.planifyText(text));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getActivityContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
    }

    protected boolean isTablet() {
        return onTablet();
    }

    protected AdapterView.OnItemClickListener absBoardListViewListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
                ChanHelper.launchUrlInBrowser(getActivityContext(), clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getFragmentManager(), ThreadFragment.TAG);
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
                    threadNo = threadNoLink;
                    refreshThread();
                    NetworkProfileManager.instance().activityChange(getChanActivity());
                } else {
                    ThreadActivity.startActivity(getActivityContext(), boardLink, threadNoLink, "");
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
                ChanHelper.launchUrlInBrowser(getActivity(), adUrl);
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
                        .show(getFragmentManager(), ThreadFragment.TAG);
            }

        }
    };

    // we only support reply button currently
    protected View.OnClickListener itemButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if ((flags & ChanPost.FLAG_IS_BUTTON) > 0)
                PostReplyActivity.startActivity(getActivity(), boardCode, threadNo, 0, "");
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
            if (onTablet() && boardCode.equals(linkedBoardCode)) {
                threadNo = linkedThreadNo;
                refresh();
            } else {
                ThreadActivity.startActivity(getActivity(), linkedBoardCode, linkedThreadNo, "");
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
                BoardActivity.startActivity(getActivity(), linkedBoardCode, "");
        }
    };

    protected View.OnLongClickListener startActionModeListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            if (cursor.moveToPosition(pos))
                postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            if (DEBUG) Log.i(TAG, "on long click for pos=" + pos + " postNo=" + postNo);

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

            absListView.setItemChecked(pos, true);

            if (actionMode == null) {
                if (DEBUG) Log.i(TAG, "starting action mode...");
                getActivity().startActionMode(actionModeCallback);
                if (DEBUG) Log.i(TAG, "started action mode");
            }
            else {
                if (DEBUG) Log.i(TAG, "action mode already started, updating share intent");
                updateSharedIntent();
            }
            return true;
        }
    };

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
            Toast.makeText(getActivityContext(), R.string.translate_no_text, Toast.LENGTH_SHORT);
            return true;
        }
        String translateUrl = GOOGLE_TRANSLATE_ROOT + localeCode + "&text=" + escaped;
        if (translateUrl.length() > MAX_HTTP_GET_URL_LEN)
            translateUrl = translateUrl.substring(0, MAX_HTTP_GET_URL_LEN);
        ChanHelper.launchUrlInBrowser(getActivityContext(), translateUrl);
        return true;
    }

    protected boolean playThreadMenu() {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.PLAY_THREAD);
        synchronized (this) {
            shouldPlayThread = !shouldPlayThread; // user clicked, invert play status
            getActivity().invalidateOptionsMenu();
            if (!shouldPlayThread) {
                return false;
            }
            if (!canPlayThread()) {
                shouldPlayThread = false;
                Toast.makeText(getActivityContext(), R.string.thread_no_start_play, Toast.LENGTH_SHORT).show();
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
                                    int first = gridView.getFirstVisiblePosition();
                                    int last = gridView.getLastVisiblePosition();
                                    for (int pos = first; pos <= last; pos++)
                                        expandVisibleItem(first, pos);
                                    */
                                    absListView.smoothScrollBy(2, 25);
                                }
                                /*
                                private void expandVisibleItem(int first, int pos) {
                                    View listItem = gridView.getChildAt(pos - first);
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
                                        gridView.performItemClick(image, pos, id);
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
                                getActivity().invalidateOptionsMenu();
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
        //if (gridView.getLastVisiblePosition() == adapter.getCount() - 1)
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
        if (DEBUG) Log.i(TAG, "updateSharedIntent() checked count=" + postPos.size());
        String linkUrl = (postNo > 0 && postNo != threadNo)
                ? ChanPost.postUrl(boardCode, threadNo, postNo)
                : ChanThread.threadUrl(boardCode, threadNo);
        String text = selectText(postPos);
        String extraText = linkUrl + (text.isEmpty() ? "" : "\n\n" + text);
        ArrayList<String> paths = new ArrayList<String>();
        Cursor cursor = adapter.getCursor();
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i) || !cursor.moveToPosition(i))
                continue;
            File file = ThreadViewer.fullSizeImageFile(getActivityContext(), cursor); // try for full size first
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
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
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
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
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
        MediaScannerConnection.scanFile(getActivityContext(), paths, types, mediaScannerListener);
    }

    public void refresh() {
        if (getActivity() != null)
            getActivity().invalidateOptionsMenu(); // in case spinner needs to be reset
        refreshBoard(); // for tablets
        refreshThread();
        if (actionMode != null)
            actionMode.finish();
    }

    public void refreshBoard() { /* for tablets */
        if (handler != null && onTablet())
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "refreshBoard() restarting loader");
                    getLoaderManager().restartLoader(1, null, loaderCallbacks);
                }
            });
    }

    public void refreshThread() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "refreshThread() restarting loader");
                    getLoaderManager().restartLoader(0, null, loaderCallbacks);
                }
            });
    }

    public void setProgress(boolean on) {
        if (handler != null)
            getActivity().setProgressBarIndeterminateVisibility(on);
        progressVisible = on;
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
            Toast.makeText(getActivityContext(), R.string.full_screen_image_search_not_found, Toast.LENGTH_SHORT)
                    .show();
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
            Toast.makeText(getActivityContext(), R.string.full_screen_image_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    protected View.OnClickListener overflowListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            uncheckAll();
            int pos = absListView.getPositionForView(v);
            if (pos >= 0) {
                absListView.setItemChecked(pos, true);
                postNo = absListView.getItemIdAtPosition(pos);
            }
            updateSharedIntent();
            PopupMenu popup = new PopupMenu(getActivityContext(), v);
            popup.inflate(R.menu.thread_context_menu);
            popup.setOnMenuItemClickListener(popupListener);
            popup.setOnDismissListener(popupDismissListener);
            MenuItem shareItem = popup.getMenu().findItem(R.id.thread_context_share_action_menu);
            shareActionProvider = shareItem == null ? null : (ShareActionProvider) shareItem.getActionProvider();
            popup.show();
        }
    };

    protected PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            long[] postNos = absListView.getCheckedItemIds();
            SparseBooleanArray postPos = absListView.getCheckedItemPositions();
            if (postNos.length == 0) {
                Toast.makeText(getActivityContext(), R.string.thread_no_posts_selected, Toast.LENGTH_SHORT).show();
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
                    //(new SelectTextDialogFragment(text)).show(getFragmentManager(), SelectTextDialogFragment.TAG);
                    return true;
                case R.id.download_images_to_gallery_menu:
                    ThreadImageDownloadService.startDownloadToGalleryFolder(
                            getActivityContext(), boardCode, threadNo, null, postNos);
                    Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
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
                            .show(getFragmentManager(), DeletePostDialogFragment.TAG);
                    return true;
                case R.id.report_posts_menu:
                    (new ReportPostDialogFragment(boardCode, threadNo, postNos))
                            .show(getFragmentManager(), ReportPostDialogFragment.TAG);
                    return true;
                case R.id.block_posts_menu:
                    Map<ChanBlocklist.BlockType, List<String>> blocklist = extractBlocklist(postPos);
                    (new BlocklistSelectToAddDialogFragment(blocklist)).show(
                            getActivity().getFragmentManager(), TAG);
                    return true;
                case R.id.web_menu:
                    String url = ChanPost.postUrl(boardCode, threadNo, postNos[0]);
                    ChanHelper.launchUrlInBrowser(getActivityContext(), url);
                default:
                    return false;
            }
        }
    };

    protected PopupMenu.OnDismissListener popupDismissListener = new PopupMenu.OnDismissListener() {
        @Override
        public void onDismiss(PopupMenu menu) {
            uncheckAll();
        }
    };

    protected void uncheckAll() {
        SparseBooleanArray checked = absListView.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            int pos = checked.keyAt(i);
            if (checked.get(pos, false))
                absListView.setItemChecked(pos, false);
        }
    }

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/" + threadNo + " id=" + id);
            if (id == 0 && threadNo > 0) {
                loadingStatusFlags &= ~THREAD_DONE;
                if (DEBUG) Log.i(TAG, "onCreateLoader returning ThreadCursorLoader for /" + boardCode + "/" + threadNo);
                setProgress(true);
                return new ThreadCursorLoader(getActivityContext(),
                        boardCode, threadNo, query, !onTablet());
            } else if (id == 0) {
                setProgress(false);
                return null;
            } else {
                loadingStatusFlags &= ~BOARD_DONE;
                if (DEBUG) Log.i(TAG, "onCreateLoder returning BoardCursorLoader for /" + boardCode + "/" + threadNo);
                setProgress(true);
                return new BoardCursorLoader(getActivityContext(), boardCode, "");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/" + threadNo + " id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()) + " loader=" + loader);
            if (loader instanceof ThreadCursorLoader)
                onThreadLoadFinished(data);
            else
                onBoardsTabletLoadFinished(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/" + threadNo + " id=" + loader.getId());
            adapter.swapCursor(null);
        }
    };

    protected Context getActivityContext() {
        return getActivity();
    }

    protected ChanIdentifiedActivity getChanActivity() {
        return (ChanIdentifiedActivity)getActivity();
    }
    
    protected ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
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
                Toast.makeText(getActivityContext(), R.string.thread_no_posts_selected, Toast.LENGTH_SHORT).show();
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
                    //(new SelectTextDialogFragment(text)).show(getFragmentManager(), SelectTextDialogFragment.TAG);
                    return true;
                case R.id.download_images_to_gallery_menu:
                    ThreadImageDownloadService.startDownloadToGalleryFolder(
                            getActivityContext(), boardCode, threadNo, null, postNos);
                    Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
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
                            .show(getFragmentManager(), DeletePostDialogFragment.TAG);
                    return true;
                case R.id.report_posts_menu:
                    (new ReportPostDialogFragment(boardCode, threadNo, postNos))
                            .show(getFragmentManager(), ReportPostDialogFragment.TAG);
                    return true;
                case R.id.block_posts_menu:
                    Map<ChanBlocklist.BlockType, List<String>> blocklist = extractBlocklist(postPos);
                    (new BlocklistSelectToAddDialogFragment(blocklist))
                            .show(getActivity().getFragmentManager(), TAG);
                    return true;
                case R.id.web_menu:
                    String url = ChanPost.postUrl(boardCode, threadNo, postNos[0]);
                    ChanHelper.launchUrlInBrowser(getActivityContext(), url);
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
    };

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            return ThreadViewer.setViewValue(view, cursor, boardCode,
                    isTablet(),
                    true,
                    columnWidth,
                    columnHeight,
                    threadListener.imageOnClickListener,
                    threadListener.backlinkOnClickListener,
                    imagesOnClickListener,
                    threadListener.repliesOnClickListener,
                    threadListener.sameIdOnClickListener,
                    threadListener.exifOnClickListener,
                    postReplyListener,
                    overflowListener,
                    expandedImageListener,
                    startActionModeListener);
        }
    };

    protected MediaScannerConnection.OnScanCompletedListener mediaScannerListener
            = new MediaScannerConnection.MediaScannerConnectionClient()
    {
        @Override
        public void onMediaScannerConnected() {}
        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (DEBUG) Log.i(TAG, "Scan completed for path=" + path + " result uri=" + uri);
            if (uri == null)
                uri = Uri.parse(path);
            checkedImageUris.put(path, uri);
            updateSharedIntent();
        }
    };

    @Override
    public void showDialog(String boardCode, long threadNo, long postNo, int pos, ThreadPopupDialogFragment.PopupType popupType) {
        if (DEBUG) Log.i(TAG, "showDialog /" + boardCode + "/" + threadNo + "#p" + postNo + " pos=" + pos);
        (new ThreadPopupDialogFragment(boardCode, threadNo, postNo, pos, popupType))
                .show(getFragmentManager(), ThreadPopupDialogFragment.TAG);
    }

    public String toString() {
        return "ThreadFragment[] " + getChanActivityId().toString();
    }
}
