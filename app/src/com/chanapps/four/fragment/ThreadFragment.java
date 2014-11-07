package com.chanapps.four.fragment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ResourceCursorAdapter;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.ThreadCursorAdapter;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.PreferenceDialogs;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.ThreadListener;
import com.chanapps.four.viewer.ThreadViewer;
import com.chanapps.four.widget.WidgetProviderUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

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

    protected static final int DRAWABLE_ALPHA_LIGHT = 0xc2;
    protected static final int DRAWABLE_ALPHA_DARK = 0xee;

    public static final boolean DEBUG = false;

    public static final int MAX_HTTP_GET_URL_LEN = 1000;
    protected static final int LOADER_ID = 0;

    protected String boardCode;
    protected long threadNo;

    protected AbstractBoardCursorAdapter adapter;
    protected AbstractBoardCursorAdapter fullAdapter; // only used for search
    protected View layout;
    protected AbsListView absListView;
    protected Handler handler;
    protected String query = "";
    protected long postNo; // for direct jumps from latest post / recent images
    protected String imageUrl;
    protected boolean shouldPlayThread = false;
    protected ShareActionProvider shareActionProviderOP = null;
    //protected ShareActionProvider shareActionProvider = null;
    protected Map<String, Uri> checkedImageUris = new HashMap<String, Uri>(); // used for tracking what's in the media store
    protected ActionMode actionMode = null;
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected View boardTitleBar;
    protected View boardSearchResultsBar;
    protected ThreadListener threadListener;
    protected boolean progressVisible = false;
    protected Menu menu = null;
    protected View.OnClickListener commentsOnClickListener = null;
    protected View.OnClickListener imagesOnClickListener = null;
    protected boolean firstLoad = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        if (bundle == null)
            bundle = getArguments();
        boardCode = bundle.getString(BOARD_CODE);
        threadNo = bundle.getLong(THREAD_NO);
        postNo = bundle.getLong(POST_NO);
        query = bundle.getString(SearchManager.QUERY);
        if (DEBUG) Log.i(TAG, "onCreateView /" + boardCode + "/" + threadNo + "#p" + postNo + " q=" + query);
        int layoutId = query != null && !query.isEmpty() ? R.layout.thread_list_layout_search : R.layout.thread_list_layout;
        layout = inflater.inflate(layoutId, viewGroup, false);
        createAbsListView();

        if (threadNo > 0)
            getLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks);
        else
            if (DEBUG) Log.i(TAG, "onCreateView /" + boardCode + "/" + threadNo + "#p" + postNo
                    + " no thread found, skipping loader");

        boardTitleBar = layout.findViewById(R.id.board_title_bar);
        boardSearchResultsBar = layout.findViewById(R.id.board_search_results_bar);
        setHasOptionsMenu(true);
        return layout;
    }

    protected PullToRefreshAttacher.OnRefreshListener pullToRefreshListener = new PullToRefreshAttacher.OnRefreshListener() {
        @Override
        public void onRefreshStarted(View view) {
            if (DEBUG) Log.i(TAG, "pullToRefreshListener.onRefreshStarted()");
            manualRefresh();
        }
    };

    protected boolean onTablet() {
        return getActivity() != null && ((ThreadActivity)getActivity()).onTablet();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putLong(ChanThread.THREAD_NO, threadNo);
        savedInstanceState.putString(SearchManager.QUERY, query);
        //int pos = absListView == null ? -1 : absListView.getFirstVisiblePosition();
        //View view = absListView == null ? null : absListView.getChildAt(0);
        //int offset = view == null ? 0 : view.getTop();
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);
        if (bundle == null)
            return;
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        query = bundle.getString(SearchManager.QUERY);
        if (DEBUG) Log.i(TAG, "onViewStateRestored /" + boardCode + "/" + threadNo);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        threadListener = new ThreadListener(this, ThemeSelector.instance(getActivity().getApplicationContext()).isDark());
        commentsOnClickListener = ThreadViewer.createCommentsOnClickListener(absListView, handler);
        imagesOnClickListener = ThreadViewer.createImagesOnClickListener(getActivityContext(), boardCode, threadNo);

        if (threadNo > 0 && (adapter == null || adapter.getCount() <= 1)) { // <= 0
            ThreadActivity activity = (ThreadActivity)getActivity();
            if (activity == null) {
                if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " activity null, skipping loader");
            }
            else if (activity.refreshing) {
                restartIfDeadAsync();
            }
            else if (!getLoaderManager().hasRunningLoaders()) {
                if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " no data and no running loaders, restarting loader");
                //getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
            }
        }
        else {
            if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " no thread found, skipping loader");
        }
        scheduleAutoUpdate();
    }

    protected void restartIfDeadAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
                final boolean isDead = thread != null && thread.isDead;
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isDead) {
                                if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " dead thread, restarting loader");
                                getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                            }
                            else {
                                if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo + " activity refreshing, skipping loader");
                            }
                        }
                    });
            }
        }).start();
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
        scheduleAutoUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/" + threadNo);
        saveViewPositionAsync();
        if (handler != null)
            handler.removeCallbacks(autoUpdateRunnable); // deschedule any current updates
        handler = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/" + threadNo);
        handler = null;
    }

    protected boolean warnedAboutNetworkDown() {
        ThreadActivity activity = (ThreadActivity)getActivity();
        if (activity == null)
            return false;
        else
            return activity.warnedAboutNetworkDown();
    }

    protected void warnedAboutNetworkDown(boolean set) {
        ThreadActivity activity = (ThreadActivity)getActivity();
        if (activity == null)
            return;
        else
            activity.warnedAboutNetworkDown(set);
    }

    public void fetchIfNeeded(final Handler activityHandler) {
        if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
                if (thread == null) {
                    if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo + " null thread, exiting");
                    return;
                }
                if (thread.isDead) {
                    if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo + " dead thread, exiting");
                    return;
                }
                if (query != null && !query.isEmpty()) {
                    if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo + " query present, exiting");
                    return;
                }
                final int replies = thread.replies;
                if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo + " checking thread replies=" + thread.replies);
                if (activityHandler != null)
                    activityHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (replies < 0 || replies > absListView.getCount() - 1) {
                                if (DEBUG) Log.i(TAG, "fetchIfNeeded() /" + boardCode + "/" + threadNo + " should fetch more, trying");
                                tryFetchThread();
                            }
                        }
                    });
            }
        }).start();
    }

    protected void tryFetchThread() {
        if (DEBUG) Log.i(TAG, "tryFetchThread /" + boardCode + "/" + threadNo);
        if (handler == null) {
            if (DEBUG) Log.i(TAG, "tryFetchThread not in foreground, exiting");
            setProgressAsync(false);
            return;
        }
        NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
        if (health == NetworkProfile.Health.NO_CONNECTION) { // || health == NetworkProfile.Health.BAD) {
            if (DEBUG) Log.i(TAG, "tryFetchThread no connection, exiting");
            final Context context = getActivityContext();
            if (handler != null && context != null && !warnedAboutNetworkDown()) {
                warnedAboutNetworkDown(true);
                final String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            setProgressAsync(false);
            return;
        }
        else {
            warnedAboutNetworkDown(false);
        }
        ThreadActivity activity = (ThreadActivity)getActivity();
        ThreadFragment primary = activity == null ? null : activity.getPrimaryItem();
        if (primary == null || primary != this) {
            if (DEBUG) Log.i(TAG, "tryFetchThread exiting since non-primary item this=" + this + " is not primary=" + primary);
            setProgressAsync(false);
            return;
        }
        if (DEBUG) Log.i(TAG, "tryFetchThread clearing fetch chan data service queue");
        FetchChanDataService.clearServiceQueue(getActivityContext());
        if (DEBUG) Log.i(TAG, "tryFetchThread calling fetch chan data service for /" + boardCode + "/" + threadNo);
        boolean fetchScheduled = FetchChanDataService.scheduleThreadFetch(getActivityContext(), boardCode, threadNo, true, false);
        if (fetchScheduled) {
            if (DEBUG) Log.i(TAG, "tryFetchThread scheduled fetch");
            setProgressAsync(true);
        }
        else {
            if (DEBUG) Log.i(TAG, "tryFetchThread couldn't fetch");
            setProgressAsync(false);
            return;
        }
    }
    
    protected void setProgressAsync(final boolean on) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(on);
                }
            });
    }

    protected void onThreadLoadFinished(Cursor data) {
        adapter.swapCursor(data);
        setupShareActionProviderOPMenu(menu);
        selectCurrentThreadAsync();
        if (firstLoad) {
            firstLoad = false;
            loadViewPositionAsync();
        }
    }

    protected void selectCurrentThreadAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
                selectCurrentThread(thread);
                scheduleAutoUpdate();
            }
        }).start();
    }

    protected static final int AUTOUPDATE_THREAD_DELAY_MS = 30000;

    protected void scheduleAutoUpdate() {
        if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() checking /" + boardCode + "/" + threadNo + " q=" + query);
        Context context = getActivityContext();
        if (context == null)
            return;
        boolean autoUpdate = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_AUTOUPDATE_THREADS, true);
        if (!autoUpdate) {
            if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() autoupdate disabled, exiting /" + boardCode + "/" + threadNo);
            return;
        }
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() query is present, exiting /" + boardCode + "/" + threadNo);
            return;
        }
        if (getActivity() != null && ((ThreadActivity)getActivity()).getCurrentFragment() != this) {
            if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() not current fragment, exiting /" + boardCode + "/" + threadNo);
            return;
        }
        ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
        if (thread == null || thread.isDead) {
            if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() dead thread, exiting /" + boardCode + "/" + threadNo);
            return;
        }
        if (handler != null)
            handler.removeCallbacks(autoUpdateRunnable); // deschedule any current updates
        if (handler != null)
            handler.postDelayed(autoUpdateRunnable, AUTOUPDATE_THREAD_DELAY_MS);
        if (handler == null) {
            if (DEBUG) Log.i(TAG, "scheduleAutoUpdate() null handler exiting /" + boardCode + "/" + threadNo);
        }
    }

    protected final Runnable autoUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.i(TAG, "autoUpdateRunnable preparing refresh /" + boardCode + "/" + threadNo);
            if (NetworkProfileManager.instance().getActivityId() != getChanActivityId()) {
                if (DEBUG) Log.i(TAG, "autoUpdateRunnable no longer foreground, cancelling update /" + boardCode + "/" + threadNo);
                return;
            }
            if (handler == null) {
                if (DEBUG) Log.i(TAG, "autoUpdateRunnable null handler, cancelling update /" + boardCode + "/" + threadNo);
                return;
            }
            if (DEBUG) Log.i(TAG, "autoUpdateRunnable manually refreshing /" + boardCode + "/" + threadNo);
            manualRefresh();
            if (DEBUG) Log.i(TAG, "autoUpdateRunnable scheduling next auto refresh /" + boardCode + "/" + threadNo);
            scheduleAutoUpdate();
        }
    };

    protected static final int FROM_BOARD_THREAD_ADAPTER_COUNT = 5; // thread header + related title + 3 related boards

    protected void setProgressFromThreadState(final ChanThread thread) {
        if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " listViewCount=" + (absListView == null ? 0 : absListView.getCount()));
        ThreadActivity activity = getActivity() instanceof ThreadActivity ? (ThreadActivity)getActivity() : null;
        if (activity == null) {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " not attached to activity, exiting");
            return;
        }
        else if (activity.getCurrentFragment() != this) {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " not the current fragment, exiting");
        }
        else if (!NetworkProfileManager.isConnected()) {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " no connection, setting load finished for thread=" + thread);
            setProgress(false);
        }
        else if (thread.isDead) {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " dead thread, setting load finished for thread=" + thread);
            setProgress(false);
        }
        else if (thread != null && thread.posts != null && thread.posts.length == 1 && thread.posts[0].replies > 0
                && absListView != null && absListView.getCount() <= FROM_BOARD_THREAD_ADAPTER_COUNT) {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " thread not fully loaded, awaiting load thread=" + thread);
        }
        else if (!thread.defData
                && thread.posts != null && thread.posts.length > 0
                && thread.posts[0] != null && !thread.posts[0].defData && thread.posts[0].replies >= 0) { // post is loaded
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " thread loaded, setting load finished for thread=" + thread);
            setProgress(false);
        }
        else {
            if (DEBUG) Log.i(TAG, "setProgressFromThreadState /" + boardCode + "/" + threadNo + " thread not yet loaded, awaiting load thread=" + thread);
        }    
    }

    public void scrollToPostAsync(final long scrollToPostNo) {
        if (DEBUG) Log.i(TAG, "scrollToPostAsync() postNo=" + scrollToPostNo);
        new Thread(new Runnable() {
            @Override
            public void run() {
                scrollToPost(scrollToPostNo, null);
            }
        }).start();
    }

    protected void scrollToPost(final long scrollToPostNo, final Runnable uiCallback) {
        if (DEBUG) Log.i(TAG, "scrollToPost() postNo=" + scrollToPostNo + " begin");
        if (adapter == null) {
            if (DEBUG) Log.i(TAG, "scrollToPost() postNo=" + scrollToPostNo + " null adapter, exiting");
            return;
        }
        Cursor cursor = adapter.getCursor();
        cursor.moveToPosition(-1);
        boolean found = false;
        int pos = 0;
        while (cursor.moveToNext()) {
            long postNoAtPos = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            if (postNoAtPos == scrollToPostNo) {
                found = true;
                break;
            }
            pos++;
        }
        final boolean hasPost = found;
        final int postPos = pos;
        if (!hasPost) {
            if (DEBUG) Log.i(TAG, "scrollToPost() didn't find postNo=" + scrollToPostNo);
            return;
        }
        if (DEBUG) Log.i(TAG, "scrollToPost() found postNo=" + scrollToPostNo + " at pos=" + pos);

        if (handler == null) {
            if (DEBUG) Log.i(TAG, "scrollToPost() postNo=" + scrollToPostNo + " null handler, skipping highlight");
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (absListView == null) {
                    if (DEBUG) Log.i(TAG, "scrollToPost() postNo=" + scrollToPostNo + " null list view, exiting");
                    return;
                }

                if (DEBUG) Log.i(TAG, "scrollToPost() postNo=" + scrollToPostNo + " scrolling to pos=" + postPos + " on UI thread");

                //(new ScrollerRunnable(absListView)).start(postPos);
                //absListView.smoothScrollToPosition(postPos);
                absListView.requestFocusFromTouch();
                absListView.setSelection(postPos);
                //if (uiCallback != null)
                //    uiCallback.run();
            }
        }, 100);

    }

    protected void selectCurrentThread(final ChanThread thread) {
        if (DEBUG) Log.i(TAG, "onThreadLoadFinished /" + boardCode + "/" + threadNo + " thread=" + thread);
        if (query != null && !query.isEmpty()) {
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        displaySearchTitle();
                        setProgressFromThreadState(thread);
                    }
                });
        }
        else if (thread.isDead) {
            if (DEBUG) Log.i(TAG, "onThreadLoadFinished /" + boardCode + "/" + threadNo + " dead thread, redisplaying");
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //absListView.invalidateViews();
                        displaySearchTitle();
                        setProgressFromThreadState(thread);
                    }
                });
        }
        else {
            if (DEBUG) Log.i(TAG, "onThreadLoadFinished /" + boardCode + "/" + threadNo + " setting spinner from thread state");
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        displaySearchTitle();
                        setProgressFromThreadState(thread);
                    }
                });
        }
    }

    protected void setProgress(boolean on) {
        progressVisible = on;
        ThreadActivity activity = (ThreadActivity)getActivity();
        if (activity != null) {
        	activity.setProgressForFragment(boardCode, threadNo, on);
        }
    }
    
    protected void createAbsListView() {
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        absListView = (ListView) layout.findViewById(R.id.thread_list_view);
        adapter = new ThreadCursorAdapter(getActivity(), viewBinder, true, null);
        absListView.setAdapter(adapter);
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        absListView.setOnCreateContextMenuListener(this);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        absListView.setFastScrollEnabled(PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, false));
    }

    public void setPullToRefreshAttacher(PullToRefreshAttacher mPullToRefreshAttacher) {
        this.mPullToRefreshAttacher = mPullToRefreshAttacher;
        if (mPullToRefreshAttacher == null)
            return;
        if (absListView != null)
            mPullToRefreshAttacher.setRefreshableView(absListView, pullToRefreshListener);
        new Thread(setPullToRefreshEnabledAsync).start();
    }

    private Runnable setPullToRefreshEnabledAsync = new Runnable() {
        @Override
        public void run() {
            Context context = getActivityContext();
            if (context == null)
                return;
            ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
            boolean enabled;
            if (thread != null && thread.isDead)
                enabled = false;
            else
                enabled = true;
            final boolean isEnabled = enabled;
            if (handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mPullToRefreshAttacher != null)
                            mPullToRefreshAttacher.setEnabled(isEnabled);
                    }
                });
        }
    };

    private String replyText(long postNos[]) {
        StringBuilder replyText = new StringBuilder();
        for (long postNo : postNos) {
            replyText.append(">>").append(postNo).append("\n");
        }
        return replyText.toString();
    }

    private void postReply(String replyText, String quotesText) {
        PostReplyActivity.startActivity(getActivityContext(), boardCode, threadNo, 0,
                ChanPost.planifyText(replyText),
                ChanPost.planifyText(quotesText));
    }

    protected boolean isThreadPlayable() {
        return adapter != null
                && adapter.getCount() > 0
                && !getLoaderManager().hasRunningLoaders()
                && !progressVisible;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
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
        setDeadStatusAsync();
        setWatchMenuAsync();
        setupShareActionProviderOPMenu(menu);
        if (getActivity() != null) {
            ((ThreadActivity)getActivity()).createSearchView(menu);
        }
        super.onPrepareOptionsMenu(menu);
    }

    protected void setDeadStatusAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean undead = undead();
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (menu == null)
                                return;
                            MenuItem item;
                            //if ((item = menu.findItem(R.id.refresh_menu)) != null)
                            //    item.setVisible(undead);
                            if (mPullToRefreshAttacher != null)
                                mPullToRefreshAttacher.setEnabled(undead);
                            if ((item = menu.findItem(R.id.post_reply_all_menu)) != null)
                                item.setVisible(undead);
                            //if ((item = menu.findItem(R.id.web_menu)) != null)
                            //    item.setVisible(undead);
                        }
                    });
            }
        }).start();
    }

    protected void setWatchMenuAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
                final boolean watched = ChanFileStorage.isThreadWatched(getActivityContext(), thread);
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (menu == null)
                                return;
                            MenuItem item;
                            if ((item = menu.findItem(R.id.watch_thread_menu)) != null)
                                item.setVisible(!watched);
                            if ((item = menu.findItem(R.id.watch_remove_thread_menu)) != null)
                                item.setVisible(watched);
                        }
                    });
            }
        }).start();
    }

    protected void setupShareActionProviderOPMenu(final Menu menu) {
        updateSharedIntentOP(shareActionProviderOP);
        if (menu == null)
            return;
        MenuItem shareItem = menu.findItem(R.id.thread_share_menu);
        shareActionProviderOP = shareItem == null ? null : (ShareActionProvider) shareItem.getActionProvider();
        if (DEBUG) Log.i(TAG, "setupShareActionProviderOP() shareActionProviderOP=" + shareActionProviderOP);
    }

    protected boolean undead() {
        ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
        return !(thread != null && thread.isDead);
    }

    protected void navigateUp() {
        Activity activity = getActivity();
        if (activity == null)
            return;
        if (activity instanceof ThreadActivity)
            ((ThreadActivity)activity).navigateUp();
        else if (activity instanceof BoardActivity)
            ((BoardActivity)activity).navigateUp();
        else
            activity.finish();
    }

    protected void setActivityIdToFragment() {
        if (!(getActivity() instanceof ThreadActivity))
            return;
        ThreadActivity ta = (ThreadActivity)getActivity();
        ta.setChanActivityId(getChanActivityId());
    }

    protected void manualRefresh() {
        if (handler != null)
            handler.removeCallbacks(autoUpdateRunnable); // deschedule autoupdates while refreshing
        setProgress(true);
        setActivityIdToFragment();
        NetworkProfileManager.instance().manualRefresh(getChanActivity());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ThreadActivity a = getActivity() != null && getActivity() instanceof ThreadActivity ? (ThreadActivity)getActivity() : null;
        ActionBarDrawerToggle t = a != null ? a.getDrawerToggle() : null;
        if (t != null && t.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.refresh_menu:
                manualRefresh();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(getActivityContext(), boardCode, threadNo);
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.watch_remove_thread_menu:
                removeFromWatchlist();
                return true;
            case R.id.scroll_to_top_menu:
                jumpToTop();
                return true;
            case R.id.scroll_to_bottom_menu:
                jumpToBottom();
                return true;
            case R.id.post_reply_all_menu:
                postReply("", selectQuoteText(0));
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadViaThreadMenu(getActivityContext(), boardCode, threadNo, new long[]{});
                Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.play_thread_menu:
                return playThreadMenu();
            case R.id.web_menu:
                String url = ChanThread.threadUrl(getActivityContext(), boardCode, threadNo);
                ActivityDispatcher.launchUrlInBrowser(getActivityContext(), url);
                return true;
            case R.id.font_size_menu:
                new PreferenceDialogs(getActivity()).showFontSizeDialog();
                return true;
            case R.id.autoload_images_menu:
                new PreferenceDialogs(getActivity()).showAutoloadImagesDialog();
                return true;
            case R.id.theme_menu:
                new PreferenceDialogs(getActivity()).showThemeDialog();
                return true;
            case R.id.use_volume_scroll_menu:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean pref = prefs.getBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_USE_VOLUME_SCROLL, pref).apply();
                getActivity().recreate();
                return true;
            case R.id.use_fast_scroll_menu:
                prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                pref = prefs.getBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, false);
                pref = !pref;
                prefs.edit().putBoolean(SettingsActivity.PREF_USE_FAST_SCROLL, pref).apply();
                if (absListView != null)
                    absListView.setFastScrollEnabled(pref);
                return true;
            case R.id.blocklist_menu:
                List<Pair<String, ChanBlocklist.BlockType>> blocks = ChanBlocklist.getSorted(getActivity());
                (new BlocklistViewAllDialogFragment(blocks, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                        if (onTablet() && getActivity() != null)
                            ((ThreadActivity)getActivity()).restartLoader();
                    }
                })).show(getActivity().getFragmentManager(), TAG);
                return true;
            default:
                ThreadActivity activity = (ThreadActivity)getActivity();
                if (activity != null)
                    return activity.onOptionsItemSelected(item);
                else
                    return super.onOptionsItemSelected(item);
        }
    }

    protected void addToWatchlist() {
        addToWatchlist(getActivityContext(), handler, boardCode, threadNo);
        setWatchMenuAsync();
    }

    protected void removeFromWatchlist() {
        removeFromWatchlist(getActivityContext(), handler, boardCode, threadNo);
        setWatchMenuAsync();
    }

    public static void addToWatchlist(final Context context, final Handler handler,
                                      final String boardCode, final long threadNo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't add null thread /" + boardCode + "/" + threadNo + " to watchlist");
                        msgId = R.string.thread_not_added_to_watchlist;
                    }
                    else {
                        ChanFileStorage.addWatchedThread(context, thread);
                        BoardActivity.refreshWatchlist(context);
                        WidgetProviderUtils.scheduleGlobalAlarm(context); // insure watchlist is updated
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
                            Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }).start();
    }

    public static void removeFromWatchlist(final Context context, final Handler handler,
                                      final String boardCode, final long threadNo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int msgId;
                try {
                    final ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                    if (thread == null) {
                        Log.e(TAG, "Couldn't remove thread /" + boardCode + "/" + threadNo + " from watchlist");
                        msgId = R.string.thread_watchlist_not_deleted_thread;
                    }
                    else {
                        boolean isDead = thread.isDead;
                        ChanFileStorage.deleteWatchedThread(context, thread);
                        BoardActivity.refreshWatchlist(context);
                        if (isDead)
                            BoardActivity.updateBoard(context, boardCode);
                        msgId = R.string.thread_deleted_from_watchlist;
                        if (DEBUG) Log.i(TAG, "Deleted /" + boardCode + "/" + threadNo + " from watchlist");
                    }
                }
                catch (IOException e) {
                    msgId = R.string.thread_watchlist_not_deleted_thread;
                    Log.e(TAG, "Exception deleting /" + boardCode + "/" + threadNo + " from watchlist", e);
                }
                final int stringId = msgId;
                /*
                if (handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                        }
                    });
                    */
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
            String subject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            String message = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            text = subject
                    + (!subject.isEmpty() && !message.isEmpty() ? "<br/>" : "")
                    + message;
            if (DEBUG) Log.i(TAG, "selectText() raw text=" + text);
            break;
        }
        text = text.replaceAll("(</?br/?>)+", "\n").replaceAll("<[^>]*>", "");
        if (DEBUG) Log.i(TAG, "selectText() returning filtered text=" + text);
        return text;
    }

    protected String selectQuoteText(SparseBooleanArray postPos) {
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            return selectQuoteText(i);
        }
        return "";
    }

    protected String selectQuoteText(int i) {
        Cursor cursor = adapter.getCursor();
        if (cursor == null)
            return "";
        cursor.moveToPosition(i);
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        String t = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        String u = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
        String itemText = (t == null ? "" : t)
                + (t != null && u != null && !t.isEmpty() && !u.isEmpty() ? "<br/>" : "")
                + (u == null ? "" : u);
        if (itemText == null)
            itemText = "";
        String postPrefix = ">>" + postNo + "\n";
        String text = postPrefix + ChanPost.quoteText(itemText, resto);
        if (DEBUG) Log.i(TAG, "Selected itemText=" + itemText + " resulting quoteText=" + text);
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

            //absListView.setItemChecked(pos, true);

            if (actionMode == null) {
                if (DEBUG) Log.i(TAG, "starting action mode...");
                getActivity().startActionMode(actionModeCallback);
                if (DEBUG) Log.i(TAG, "started action mode");
            }
            else {
                if (DEBUG) Log.i(TAG, "action mode already started, updating share intent");
                //updateSharedIntent(shareActionProvider, absListView.getCheckedItemPositions());
            }
            return true;
        }
    };

    protected Set<Pair<String, ChanBlocklist.BlockType>> extractBlocklist(SparseBooleanArray postPos) {
        Set<Pair<String, ChanBlocklist.BlockType>> blocklist = new HashSet<Pair<String, ChanBlocklist.BlockType>>();
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
                blocklist.add(new Pair<String, ChanBlocklist.BlockType>(tripcode, ChanBlocklist.BlockType.TRIPCODE));

            String name = cursor.getString(cursor.getColumnIndex(ChanPost.POST_NAME));
            if (name != null && !name.isEmpty() && !name.equals("Anonymous"))
                blocklist.add(new Pair<String, ChanBlocklist.BlockType>(tripcode, ChanBlocklist.BlockType.NAME));

            String email = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EMAIL));
            if (email != null && !email.isEmpty() && !email.equals("sage"))
                blocklist.add(new Pair<String, ChanBlocklist.BlockType>(tripcode, ChanBlocklist.BlockType.EMAIL));

            String userId = cursor.getString(cursor.getColumnIndex(ChanPost.POST_USER_ID));
            if (userId != null && !userId.isEmpty() && !userId.equals("Heaven"))
                blocklist.add(new Pair<String, ChanBlocklist.BlockType>(tripcode, ChanBlocklist.BlockType.ID));
        }

        return blocklist;
    }

    protected boolean translatePosts(SparseBooleanArray postPos) {
        final Locale locale = getResources().getConfiguration().locale;
        final String localeCode = locale.getLanguage();
        final String text = selectText(postPos);
        final String strippedText = text.replaceAll("<br/?>", "\n").replaceAll("<[^>]*>", "").trim();
        if (DEBUG) Log.i(TAG, "translatePosts() translating text=" + strippedText);
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
        String translateUrl = String.format(
                URLFormatComponent.getUrl(getActivityContext(), URLFormatComponent.GOOGLE_TRANSLATE_URL_FORMAT),
                localeCode, localeCode, escaped);
        if (translateUrl.length() > MAX_HTTP_GET_URL_LEN)
            translateUrl = translateUrl.substring(0, MAX_HTTP_GET_URL_LEN);
        if (DEBUG) Log.i(TAG, "translatePosts() launching url=" + translateUrl);
        ActivityDispatcher.launchUrlInBrowser(getActivityContext(), translateUrl);
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
                                //absListView.setFastScrollEnabled(false);
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
                                //absListView.setFastScrollEnabled(true);
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
        //if (absListView.getLastVisiblePosition() == adapter.getCount() - 1)
        //    return false; // stop
        //It is scrolled all the way down here
        if (absListView.getLastVisiblePosition() >= absListView.getAdapter().getCount() - 1)
            return false;
        if (handler == null)
            return false;
        return true;
    }

    private void setShareIntent(final ShareActionProvider provider, final Intent intent) {
        if (ActivityDispatcher.onUIThread())
            synchronized (this) {
                if (provider != null && intent != null)
                    provider.setShareIntent(intent);
            }
        else if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (provider != null && intent != null)
                            provider.setShareIntent(intent);
                    }
                }
            });
    }

    protected void updateSharedIntent(ShareActionProvider provider, SparseBooleanArray postPos) {
        if (postPos == null)
            return;
        if (DEBUG) Log.i(TAG, "updateSharedIntent() checked count=" + postPos.size());
        if (postPos.size() < 1)
            return;
        Cursor cursor = adapter.getCursor();
        if (cursor == null)
            return;

        // construct paths and add files
        //ArrayList<String> paths = new ArrayList<String>();
        //long firstPost = -1;
        //ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        String url = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            if (!cursor.moveToPosition(i))
                continue;
            //if (firstPost == -1)
            //    firstPost = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            //File file = ThreadViewer.fullSizeImageFile(getActivityContext(), cursor); // try for full size first
            //if (file == null) { // if can't find it, fall back to thumbnail
            url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL)); // thumbnail
            if (url != null && !url.isEmpty())
                break;
                //if (DEBUG) Log.i(TAG, "Couldn't find full image, falling back to thumbnail=" + url);
                //file = (url == null || url.isEmpty()) ? null : imageLoader.getDiscCache().get(url);
            //}
            //if (file == null || !file.exists() || !file.canRead() || file.length() <= 0)
            //    continue;
            //paths.add(file.getAbsolutePath());
        }
        if (url == null || url.isEmpty())
            return;

        // set share text
        //if (DEBUG) Log.i(TAG, "updateSharedIntent() found postNo=" + firstPost + " for threadNo=" + threadNo);
        /*
        String linkUrl = (firstPost > 0 && firstPost != threadNo)
                ? ChanPost.postUrl(getActivityContext(), boardCode, threadNo, firstPost)
                : ChanThread.threadUrl(getActivityContext(), boardCode, threadNo);
        */
        // create intent
        Intent intent;
        intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setType("text/plain");
        setShareIntent(provider, intent);
    }

    protected void updateSharedIntentOP(ShareActionProvider provider) {
        String url = ChanThread.threadUrl(getActivityContext(), boardCode, threadNo);
        Intent intent;
        intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setType("text/plain");
        setShareIntent(provider, intent);
    }

    protected void asyncUpdateSharedIntent(ArrayList<String> pathList) {
        String[] paths = new String[pathList.size()];
        String[] types = new String[pathList.size()];
        for (int i = 0; i < pathList.size(); i++) {
            paths[i] = pathList.get(i);
            types[i] = "image/jpeg";
        }
    }

    public void onRefresh() {
        if (DEBUG) Log.i(TAG, "onRefresh() /" + boardCode + "/" + threadNo);
        if (getActivity() != null)
            getActivity().invalidateOptionsMenu(); // in case spinner needs to be reset
        refreshThread();
        if (actionMode != null)
            actionMode.finish();
    }

    public void refreshThread() {
        refreshThread(null);
    }

    public void refreshThread(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanThread thread = ChanFileStorage.loadThreadData(getActivityContext(), boardCode, threadNo);
                if (DEBUG) Log.i(TAG, "refreshThread /" + boardCode + "/" + threadNo + " checking status");
                if (thread != null && thread.isDead) {
                    if (DEBUG) Log.i(TAG, "refreshThread /" + boardCode + "/" + threadNo + " found dead thread");
                }
                if (handler != null && getActivity() != null && getActivity().getLoaderManager() != null) {
                    if (DEBUG) Log.i(TAG, "refreshThread /" + boardCode + "/" + threadNo + " scheduling handler post");
                    if (handler != null)
                        handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.i(TAG, "refreshThread /" + boardCode + "/" + threadNo + " restarting loader");
                            if (getActivity() != null && getActivity().getLoaderManager() != null) {
                                getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                                if (message != null && !message.isEmpty())
                                    Toast.makeText(getActivityContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    protected View.OnClickListener overflowListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == null)
                return;
            int pos = -1;
            SparseBooleanArray checked;
            synchronized (this) {
                if (absListView == null)
                    return;
                pos = absListView == null ? -1 : absListView.getPositionForView(v);
                if (absListView != null && pos >= 0) {
                    absListView.setItemChecked(pos, true);
                    postNo = absListView == null ? -1 : absListView.getItemIdAtPosition(pos);
                }
                checked = absListView == null ? null : absListView.getCheckedItemPositions();
            }
            if (pos == -1)
                return;
            //updateSharedIntent(shareActionProvider, checked);
            PopupMenu popup = new PopupMenu(getActivityContext(), v);
            Cursor cursor = adapter.getCursor();
            boolean hasImage = cursor != null
                    && (cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS)) & ChanPost.FLAG_HAS_IMAGE) > 0;
                boolean isHeader = pos == 0;
                int menuId;
            if (!undead())
                menuId = R.menu.thread_dead_context_menu;
            else if (isHeader)
                menuId = R.menu.thread_header_context_menu;
            else if (hasImage)
                menuId = R.menu.thread_image_context_menu;
            else
                menuId = R.menu.thread_text_context_menu;
            popup.inflate(menuId);
            popup.setOnMenuItemClickListener(popupListener);
            popup.setOnDismissListener(popupDismissListener);
            MenuItem shareItem = popup.getMenu().findItem(R.id.thread_context_share_action_menu);
            //shareActionProvider = shareItem == null ? null : (ShareActionProvider) shareItem.getActionProvider();
            //if (DEBUG) Log.i(TAG, "overflowListener.onClick() popup called shareActionProvider=" + shareActionProvider);
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
                    postReply(replyText(postNos), selectQuoteText(postPos));
                    return true;
                case R.id.copy_text_menu:
                    String selectText = selectText(postPos);
                    copyToClipboard(selectText);
                    //(new SelectTextDialogFragment(text)).show(getFragmentManager(), SelectTextDialogFragment.TAG);
                    return true;
                case R.id.download_images_to_gallery_menu:
                    ThreadImageDownloadService.startDownloadViaThreadMenu(
                            getActivityContext(), boardCode, threadNo, postNos);
                    Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
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
                case R.id.web_menu:
                    String url = ChanPost.postUrl(getActivityContext(), boardCode, threadNo, postNos[0]);
                    ActivityDispatcher.launchUrlInBrowser(getActivityContext(), url);
                default:
                    return false;
            }
        }
    };

    protected PopupMenu.OnDismissListener popupDismissListener = new PopupMenu.OnDismissListener() {
        @Override
        public void onDismiss(PopupMenu menu) {
        }
    };

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/" + threadNo + " q=" + query + " id=" + id);
            setProgress(true);
            boolean showRelatedBoards;
            if (onTablet())
                showRelatedBoards = false;
            else showRelatedBoards = true;
            return new ThreadCursorLoader(getActivityContext(), boardCode, threadNo, query, showRelatedBoards);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/" + threadNo + " id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()) + " loader=" + loader);
            onThreadLoadFinished(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/" + threadNo + " id=" + loader.getId());
            //adapter.swapCursor(null);
            adapter.changeCursor(null);
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
            inflater.inflate(R.menu.thread_text_context_menu, menu);
            MenuItem shareItem = menu.findItem(R.id.thread_context_share_action_menu);
            //if (shareItem != null) {
            //    shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
            //} else {
            //    shareActionProvider = null;
            //}
            mode.setTitle(R.string.thread_context_select);
            actionMode = mode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (DEBUG) Log.i(TAG, "onPrepareActionMode");
            //updateSharedIntent(shareActionProvider, absListView.getCheckedItemPositions());
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
                    postReply(replyText(postNos), selectQuoteText(postPos));
                    return true;
                /*
                case R.id.post_reply_all_quote_menu:
                    String quotesText = selectQuoteText(postPos);
                    postReply(quotesText);
                    return true;
                */
                case R.id.copy_text_menu:
                    String selectText = selectText(postPos);
                    copyToClipboard(selectText);
                    //(new SelectTextDialogFragment(text)).show(getFragmentManager(), SelectTextDialogFragment.TAG);
                    return true;
                case R.id.download_images_to_gallery_menu:
                    ThreadImageDownloadService.startDownloadViaThreadMenu(
                            getActivityContext(), boardCode, threadNo, postNos);
                    Toast.makeText(getActivityContext(), R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
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
                case R.id.web_menu:
                    String url = ChanPost.postUrl(getActivityContext(), boardCode, threadNo, postNos[0]);
                    ActivityDispatcher.launchUrlInBrowser(getActivityContext(), url);
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
            //updateSharedIntent(shareActionProvider, absListView.getCheckedItemPositions());
        }
    };

    @Override
    public void showDialog(String boardCode, long threadNo, long postNo, int pos, ThreadPopupDialogFragment.PopupType popupType) {
        if (DEBUG) Log.i(TAG, "showDialog /" + boardCode + "/" + threadNo + "#p" + postNo + " pos=" + pos);
        //(new ThreadPopupDialogFragment(this, boardCode, threadNo, postNo, pos, popupType, query))
        (new ThreadPopupDialogFragment(this, boardCode, threadNo, postNo, popupType, query))
                .show(getFragmentManager(), ThreadPopupDialogFragment.TAG);
    }

    public String toString() {
        return "ThreadFragment[] " + getChanActivityId().toString();
    }

    protected View.OnClickListener goToThreadUrlListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (getActivityContext() != null) {
                String url = ChanThread.threadUrl(getActivityContext(), boardCode, threadNo);
                ActivityDispatcher.launchUrlInBrowser(getActivityContext(), url);
            }
        }
    };

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            return ThreadViewer.setViewValue(view, cursor, boardCode,
                    true,
                    0,
                    0,
                    threadListener.thumbOnClickListener,
                    threadListener.backlinkOnClickListener,
                    commentsOnClickListener,
                    imagesOnClickListener,
                    threadListener.repliesOnClickListener,
                    threadListener.sameIdOnClickListener,
                    threadListener.exifOnClickListener,
                    overflowListener,
                    threadListener.expandedImageListener,
                    startActionModeListener,
                    goToThreadUrlListener
            );
        }
    };

    protected void displaySearchTitle() {
        if (getActivity() == null)
            return;
        displayTitleBar(getString(R.string.search_results_title), R.drawable.search, R.drawable.search_light);
        displayResultsBar();
    }

    protected void displayResultsBar() {
        if (boardSearchResultsBar == null)
            return;
        if (query == null || query.isEmpty()) {
            boardSearchResultsBar.setVisibility(View.GONE);
            return;
        }
        int resultsId = adapter != null && adapter.getCount() > 0
                ? R.string.thread_search_results
                : R.string.thread_search_no_results;
        String results = String.format(getString(resultsId), query);
        TextView searchResultsTextView = (TextView)boardSearchResultsBar.findViewById(R.id.board_search_results_text);
        searchResultsTextView.setText(results);
        boardSearchResultsBar.setVisibility(View.VISIBLE);
    }

    @TargetApi(16)
    protected void displayTitleBar(String title, int lightIconId, int darkIconId) {
        if (boardTitleBar == null)
            return;
        if (query == null || query.isEmpty()) {
            boardTitleBar.setVisibility(View.GONE);
            return;
        }
        TextView boardTitle = (TextView)boardTitleBar.findViewById(R.id.board_title_text);
        ImageView boardIcon = (ImageView)boardTitleBar.findViewById(R.id.board_title_icon);
        if (boardTitle == null || boardIcon == null)
            return;
        boardTitle.setText(title);
        boolean isDark = ThemeSelector.instance(getActivity().getApplicationContext()).isDark();
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
    }

    @SuppressWarnings("deprecation")
    protected void deprecatedSetAlpha(ImageView v, int a) {
        v.setAlpha(a);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.thread_menu, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    protected void jumpToTop() {
        ThreadViewer.jumpToTop(absListView, handler);
    }

    protected void jumpToBottom() {
        ThreadViewer.jumpToBottom(absListView, handler);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void onUpdateFastScroll(final boolean enabled) {
        final Handler gridHandler = handler != null ? handler : new Handler();
        if (gridHandler != null)
            gridHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (absListView != null)
                        absListView.setFastScrollEnabled(enabled);
                }
            });
    }

    protected void loadViewPositionAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Context c = getActivityContext();
                if (c == null)
                    return;
                ChanThread thread = ChanFileStorage.loadThreadData(c, boardCode, threadNo);
                if (thread == null)
                    return;
                final int firstVisiblePosition = thread.viewPosition;
                final int firstVisibleOffset = thread.viewOffset;
                if (firstVisiblePosition >= 0 && handler != null)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.i(TAG, "loaded view position /" + boardCode + "/" + threadNo
                                    + " pos=" + firstVisiblePosition + " offset=" + firstVisibleOffset);
                            if (absListView == null)
                                return;
                            if (absListView instanceof ListView) {
                                ((ListView)absListView).setSelectionFromTop(firstVisiblePosition, firstVisibleOffset);
                            }
                            else {
                                absListView.requestFocusFromTouch();
                                absListView.setSelection(firstVisiblePosition);
                            }
                        }
                    });
            }
        }).start();
    }

    protected void saveViewPositionAsync() {
        if (absListView == null)
            return;
        final int firstVisiblePosition = absListView.getFirstVisiblePosition();
        final int firstVisibleOffset = absListView.getChildAt(firstVisiblePosition) == null
                ? 0
                : absListView.getChildAt(firstVisiblePosition).getTop();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Context c = getActivityContext();
                if (c == null)
                    return;
                ChanThread thread = ChanFileStorage.loadThreadData(c, boardCode, threadNo);
                if (thread == null)
                    return;
                thread.viewPosition = firstVisiblePosition;
                thread.viewOffset = firstVisibleOffset;
                try {
                    ChanFileStorage.storeThreadData(c, thread);
                    if (DEBUG) Log.i(TAG, "saved view position /" + boardCode + "/" + threadNo
                            + " pos=" + firstVisiblePosition + " offset=" + firstVisibleOffset);
                }
                catch (IOException e) {
                    Log.e(TAG, "Exception saving thread view position /" + boardCode + "/" + threadNo);
                }
            }
        }).start();
    }

}
