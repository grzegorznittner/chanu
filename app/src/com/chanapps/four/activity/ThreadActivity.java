package com.chanapps.four.activity;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.ThreadExpandImageOnClickListener;
import com.chanapps.four.component.ThreadImageOnClickListener;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.UserStatistics.ChanFeature;
import com.chanapps.four.fragment.BlocklistSelectToAddDialogFragment;
import com.chanapps.four.fragment.DeletePostDialogFragment;
import com.chanapps.four.fragment.ListOfLinksDialogFragment;
import com.chanapps.four.fragment.ReportPostDialogFragment;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.task.HighlightRepliesTask;
import com.chanapps.four.viewer.BoardViewer;
import com.chanapps.four.viewer.ThreadViewer;
import com.chanapps.four.viewer.ViewType;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
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
        extends BoardActivity
        implements ChanIdentifiedActivity,
        RefreshableActivity,
        AbsListView.OnItemClickListener,
        AbsListView.MultiChoiceModeListener,
        PopupMenu.OnMenuItemClickListener,
        MediaScannerConnection.OnScanCompletedListener {
    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static final int WATCHLIST_ACTIVITY_THRESHOLD = 7; // arbitrary from experience
    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final int MAX_HTTP_GET_URL_LEN = 2000;
    protected static final int THREAD_DONE = 0x1;
    protected static final int BOARD_DONE = 0x2;

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected ChanThread thread = null;
    protected boolean shouldPlayThread = false;
    protected ShareActionProvider shareActionProvider = null;
    protected Map<String, Uri> checkedImageUris = new HashMap<String, Uri>(); // used for tracking what's in the media store
    protected ActionMode actionMode = null;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected BoardCursorLoader cursorLoaderBoardsTablet;
    protected AbsListView absBoardListView;
    protected int loadingStatusFlags = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (id == 0) {
            if (threadNo > 0) {
                loadingStatusFlags &= ~THREAD_DONE;
                cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, query, absListView, absBoardListView == null);
                if (DEBUG) Log.i(TAG, "Started loader for " + boardCode + "/" + threadNo);
                setProgressBarIndeterminateVisibility(true);
            } else {
                cursorLoader = null;
                setProgressBarIndeterminateVisibility(false);
            }
            return cursorLoader;
        } else {
            loadingStatusFlags &= ~BOARD_DONE;
            cursorLoaderBoardsTablet = new BoardCursorLoader(this, boardCode, "");
            setProgressBarIndeterminateVisibility(true);
            return cursorLoaderBoardsTablet;
        }
    }

    public static void startActivity(Activity from, String boardCode, long threadNo) {
        if (threadNo <= 0)
            startActivity(from, boardCode);
        else
            from.startActivity(createIntentForActivity(from, boardCode, threadNo, ""));
    }

    public static void startActivityForSearch(Activity from, String boardCode, long threadNo, String query) {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.SEARCH_THREAD);
        from.startActivity(createIntentForActivity(from, boardCode, threadNo, query));
    }

    public static Intent createIntentForActivity(Context context, final String boardCode, final long threadNo) {
        return createIntentForActivity(context, boardCode, threadNo, "");
    }

    public static Intent createIntentForActivity(Context context, final String boardCode, final long threadNo, String query) {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(SearchManager.QUERY, query);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, 0); // reset it
        editor.commit();
        return intent;
    }

    @Override
    protected void loadFromIntentOrPrefs() {
        ensurePrefs();
        Intent intent = getIntent();
        boardCode = intent.hasExtra(ChanHelper.BOARD_CODE)
                ? intent.getStringExtra(ChanHelper.BOARD_CODE)
                : prefs.getString(ChanHelper.BOARD_CODE, "a");
        threadNo = intent.hasExtra(ChanHelper.THREAD_NO)
                ? intent.getLongExtra(ChanHelper.THREAD_NO, 0)
                : prefs.getLong(ChanHelper.THREAD_NO, 0);
        query = intent.hasExtra(SearchManager.QUERY)
                ? intent.getStringExtra(SearchManager.QUERY)
                : prefs.getString(SearchManager.QUERY, "");
        tim = intent.hasExtra(ChanHelper.TIM)
                ? intent.getLongExtra(ChanHelper.TIM, 0)
                : prefs.getLong(ChanHelper.TIM, 0);
        text = intent.hasExtra(ChanHelper.TEXT)
                ? intent.getStringExtra(ChanHelper.TEXT)
                : prefs.getString(ChanHelper.TEXT, null);
        imageUrl = intent.hasExtra(ChanHelper.IMAGE_URL)
                ? intent.getStringExtra(ChanHelper.IMAGE_URL)
                : prefs.getString(ChanHelper.IMAGE_URL, null);
        imageWidth = intent.hasExtra(ChanHelper.IMAGE_WIDTH)
                ? intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0)
                : prefs.getInt(ChanHelper.IMAGE_WIDTH, 0);
        imageHeight = intent.hasExtra(ChanHelper.IMAGE_HEIGHT)
                ? intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0)
                : prefs.getInt(ChanHelper.IMAGE_HEIGHT, 0);
        // backup in case we are missing stuff
        if (boardCode == null || boardCode.isEmpty()) {
            Intent selectorIntent = new Intent(this, BoardSelectorActivity.class);
            selectorIntent.putExtra(ChanHelper.BOARD_TYPE, BoardType.JAPANESE_CULTURE.toString());
            selectorIntent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
            selectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(selectorIntent);
            finish();
        }
        if (threadNo == 0) {
            Intent boardIntent = createIntentForActivity(this, boardCode, "");
            boardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(boardIntent);
            finish();
        }
        // normal processing resumes
        if (intent.getBooleanExtra(ChanHelper.TRIGGER_BOARD_REFRESH, false)) {
            FetchChanDataService.scheduleBoardFetch(getBaseContext(), boardCode);
        }
        if (DEBUG)
            Log.i(TAG, "Thread intent is: " + intent.getStringExtra(ChanHelper.BOARD_CODE) + "/" + intent.getLongExtra(ChanHelper.THREAD_NO, 0));
        if (DEBUG) Log.i(TAG, "Thread loaded: " + boardCode + "/" + threadNo);
    }

    @Override
    protected void saveInstanceState() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, threadNo);
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, absListView.getFirstVisiblePosition());
        editor.putLong(ChanHelper.TIM, tim);
        editor.putString(ChanHelper.TEXT, text);
        editor.putString(ChanHelper.IMAGE_URL, imageUrl);
        editor.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
        editor.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void initImageLoader() {
        imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc()
                .imageScaleType(ImageScaleType.EXACTLY)
                .resetViewBeforeLoading()
                .build();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (absBoardListView != null)
            getLoaderManager().destroyLoader(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (absBoardListView != null) {
            getSupportLoaderManager().restartLoader(1, null, this);
        }
    }

    @Override
    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(this,
                R.layout.thread_list_item,
                this,
                new String[]{
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_HEADLINE_TEXT,
                        ChanPost.POST_SUBJECT_TEXT,
                        ChanPost.POST_SUBJECT_TEXT,
                        ChanPost.POST_TEXT,
                        ChanPost.POST_COUNTRY_URL,
                        ChanPost.POST_IMAGE_URL
                },
                new int[]{
                        R.id.list_item_header_wrapper,
                        R.id.list_item_expanded_progress_bar,
                        R.id.list_item_image_expanded,
                        R.id.list_item_image_wrapper,
                        R.id.list_item_image,
                        R.id.list_item_header,
                        R.id.list_item_subject,
                        R.id.list_item_title,
                        R.id.list_item_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_image_exif
                });
        absListView.setAdapter(adapter);

        if (absBoardListView != null && absBoardListView instanceof GridView) {
            adapterBoardsTablet = new BoardGridCursorAdapter(this,
                    R.layout.board_grid_item,
                    this,
                    new String[]{
                            ChanThread.THREAD_THUMBNAIL_URL,
                            ChanThread.THREAD_TITLE,
                            ChanThread.THREAD_SUBJECT,
                            ChanThread.THREAD_HEADLINE,
                            ChanThread.THREAD_TEXT,
                            ChanThread.THREAD_COUNTRY_FLAG_URL,
                            ChanThread.THREAD_NUM_REPLIES,
                            ChanThread.THREAD_NUM_IMAGES
                    },
                    new int[]{
                            R.id.grid_item_thread_thumb,
                            R.id.grid_item_thread_title,
                            R.id.grid_item_thread_subject,
                            R.id.grid_item_thread_headline,
                            R.id.grid_item_thread_text,
                            R.id.grid_item_country_flag,
                            R.id.grid_item_num_replies,
                            R.id.grid_item_num_images
                    },
                    columnWidth,
                    columnHeight);
            absBoardListView.setAdapter(adapterBoardsTablet);
        }
        /*
        else if (absBoardListView != null && absBoardListView instanceof ListView) {
            adapterBoardsTablet = new BoardListCursorAdapter(this,
                    R.layout.board_list_item,
                    this,
                    new String[]{
                            ChanThread.THREAD_THUMBNAIL_URL,
                            ChanThread.THREAD_TITLE,
                            ChanThread.THREAD_SUBJECT,
                            ChanThread.THREAD_HEADLINE,
                            ChanThread.THREAD_TEXT,
                            ChanThread.THREAD_COUNTRY_FLAG_URL,
                            ChanThread.THREAD_NUM_REPLIES,
                            ChanThread.THREAD_NUM_IMAGES
                    },
                    new int[]{
                            R.id.grid_item_thread_thumb,
                            R.id.grid_item_thread_title,
                            R.id.grid_item_thread_subject,
                            R.id.grid_item_thread_headline,
                            R.id.grid_item_thread_text,
                            R.id.grid_item_country_flag,
                            R.id.grid_item_num_replies,
                            R.id.grid_item_num_images
                    });
            absBoardListView.setAdapter(adapterBoardsTablet);
        }
        */
    }

    @Override
    protected String getLastPositionName() {
        return ChanHelper.LAST_THREAD_POSITION;
    }

    @Override
    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.THREAD);
        cg.sizeGridToDisplay();
    }

    @Override
    protected void initAbsListView() {
        absListView = (ListView) findViewById(R.id.thread_list_view);
        absBoardListView = (GridView) findViewById(R.id.board_grid_view_tablet);
        if (absBoardListView != null) {
            sizeTabletGridToDisplay();
            resetImageOptions(new ImageSize(columnWidth, columnHeight));
        }
        //else {
        //    absBoardListView = (ListView) findViewById(R.id.board_list_view_tablet);
        //}
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

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader == this.cursorLoader) {
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
            ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
            if ((data == null || data.getCount() < 1
                    || (thread != null && thread.replies > 0 && !thread.isDead && data.getCount() < 2))
                    && handler != null)
            {
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
                loadingStatusFlags |= THREAD_DONE;
                stopProgressBarIfLoadersDone();
            }
        }
        else if (loader == this.cursorLoaderBoardsTablet) {
            this.adapterBoardsTablet.swapCursor(data);

            // retry load if maybe data wasn't there yet
            if (data != null && data.getCount() < 1 && handler != null) {
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                    stopProgressBarIfLoadersDone();
                    String msg = String.format(getString(R.string.mobile_profile_health_status),
                            health.toString().toLowerCase().replaceAll("_", " "));
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    handler.sendEmptyMessageDelayed(1, LOADER_RESTART_INTERVAL_SHORT_MS);
                }
            } else {
                handleUpdatedThreads(); // see if we need to update
                loadingStatusFlags |= BOARD_DONE;
                stopProgressBarIfLoadersDone();
            }

        }
    }

    @Override
    protected void stopProgressBarIfLoadersDone() {
        if (absBoardListView == null && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgressBarIndeterminateVisibility(false);
        else if (absBoardListView != null && (loadingStatusFlags & BOARD_DONE) > 0 && (loadingStatusFlags & THREAD_DONE) > 0)
            setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void createAbsListView() {
        setAbsListViewClass();
        layout = View.inflate(getApplicationContext(), getLayoutId(), null);
        setContentView(layout);
        initAbsListView();
        initAdapter();
        setupContextMenu();
        absListView.setOnItemClickListener(this);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        if (absBoardListView != null) {
            absBoardListView.setOnItemClickListener(absBoardListViewListener);
            absBoardListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        }
    }

    protected void setupContextMenu() {
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        absListView.setMultiChoiceModeListener(this);
        absListView.setOnCreateContextMenuListener(this);
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        return ThreadViewer.setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions,
                boardCode, subjectTypeface, padding4DP);
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

    @Override
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
        replyIntent.putExtra(ChanHelper.TIM, tim);
        replyIntent.putExtra(ChanHelper.TEXT, planifyText(replyText));
        startActivity(replyIntent);
    }

    protected boolean navigateUp() {
        // FIXME: know that I'm coming from watching and return there
        Intent upIntent = new Intent(this, BoardActivity.class);
        upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem playMenuItem = menu.findItem(R.id.play_thread_menu);
        if (playMenuItem != null)
            synchronized (this) {
                playMenuItem.setIcon(shouldPlayThread ? R.drawable.av_stop : R.drawable.av_play);
                playMenuItem.setTitle(shouldPlayThread ? R.string.play_thread_stop_menu : R.string.play_thread_menu);
            }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return navigateUp();
            case R.id.refresh_menu:
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            // thread_reply_popup_menu
            case R.id.post_reply_menu:
                postReply("");
                return true;
            /*
            case R.id.post_reply_quote_menu:
                SparseBooleanArray pos = new SparseBooleanArray();
                pos.append(0, true);
                String quoteText = selectQuoteText(pos);
                postReply(quoteText);
                return true;
            */
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;

            // thread_image_popup_menu
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                addToWatchlistIfNotAlreadyIn();
                return true;
            case R.id.download_all_images_menu:
                ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                addToWatchlistIfNotAlreadyIn();
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                addToWatchlistIfNotAlreadyIn();
                return true;
            /*
            case R.id.thread_reply_popup_button_menu:
                return showPopupMenu(R.id.thread_list_layout, R.id.thread_reply_popup_button_menu, R.menu.thread_reply_popup_menu);
            case R.id.thread_image_popup_button_menu:
                return showPopupMenu(R.id.thread_list_layout, R.id.thread_image_popup_button_menu, R.menu.thread_image_popup_menu);
            */
            case R.id.play_thread_menu:
                return playThreadMenu();
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
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

    protected boolean showPopupMenu(int layoutId, int menuItemId, int popupMenuId) {
        View v = findViewById(menuItemId);
        if (v == null)
            v = findViewById(layoutId);
        if (v == null)
            return false;
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(popupMenuId);
        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuId = ChanBoard.showNSFW(this) ? R.menu.thread_menu_adult : R.menu.thread_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        setupSearch(menu);
        this.menu = menu;
        return true;
    }

    protected void addToWatchlistIfNotAlreadyIn() {
        int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        if (stringId == R.string.thread_added_to_watchlist)
            Toast.makeText(this, R.string.thread_added_to_watchlist_activity_based, Toast.LENGTH_SHORT).show();
    }

    protected void addToWatchlist() {
        int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
    }

    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo);
    }

    @Override
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
        updateSharedIntent();
        return true;
    }

    @Override
    public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id, final boolean checked) {
        if (DEBUG) Log.i(TAG, "Updating shared intent pos=" + position + " checked=" + checked);
        updateSharedIntent();
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

            case R.id.highlight_replies_menu:
                (new HighlightRepliesTask(getApplicationContext(), absListView, boardCode, threadNo, HighlightRepliesTask.SearchType.POST_REPLIES))
                        .execute(postNos);
                return true;
            case R.id.highlight_previous_menu:
                (new HighlightRepliesTask(getApplicationContext(), absListView, boardCode, threadNo, HighlightRepliesTask.SearchType.PREVIOUS_POSTS))
                        .execute(postNos);
                return true;
            case R.id.highlight_ids_menu:
                (new HighlightRepliesTask(getApplicationContext(), absListView, boardCode, threadNo, HighlightRepliesTask.SearchType.SAME_POSTERS))
                        .execute(postNos);
                return true;
            case R.id.copy_text_menu:
                String selectText = selectText(postPos);
                copyToClipboard(selectText);
                //(new SelectTextDialogFragment(text)).show(getSupportFragmentManager(), SelectTextDialogFragment.TAG);
                return true;

            case R.id.download_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null, postNos);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                addToWatchlistIfNotAlreadyIn();
                return true;
            case R.id.go_to_link_menu:
                String[] urls = extractUrlsFromPosts(postPos);
                if (urls != null && urls.length > 0)
                    (new ListOfLinksDialogFragment(urls)).show(getSupportFragmentManager(), ListOfLinksDialogFragment.TAG);
                else
                    Toast.makeText(getApplicationContext(), R.string.go_to_link_not_found, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.translate_posts_menu:
                return translatePosts(postPos);

            case R.id.delete_posts_menu:
                (new DeletePostDialogFragment(this, boardCode, threadNo, postNos))
                        .show(getSupportFragmentManager(), DeletePostDialogFragment.TAG);
                return true;
            case R.id.report_posts_menu:
                (new ReportPostDialogFragment(this, boardCode, threadNo, postNos))
                        .show(getSupportFragmentManager(), ReportPostDialogFragment.TAG);
                return true;
            case R.id.block_posts_menu:
                Map<ChanBlocklist.BlockType, List<String>> blocklist = extractBlocklist(postPos);
                (new BlocklistSelectToAddDialogFragment(this, blocklist)).show(getSupportFragmentManager(), TAG);
                return true;

            //case R.id.thread_context_reply_popup_button_menu:
            //    return showPopupMenu(R.id.thread_list_layout, R.id.thread_context_reply_popup_button_menu, R.menu.thread_context_reply_popup_menu);
            //case R.id.thread_context_replies_popup_button_menu:
            //    return showPopupMenu(R.id.thread_list_layout, R.id.thread_context_replies_popup_button_menu, R.menu.thread_context_replies_popup_menu);
            //case R.id.thread_context_info_popup_button_menu:
            //    return showPopupMenu(R.id.thread_list_layout, R.id.thread_context_info_popup_button_menu, R.menu.thread_context_info_popup_menu);
            //case R.id.thread_context_delete_report_popup_button_menu:
            //    return showPopupMenu(R.id.thread_list_layout, R.id.thread_context_delete_report_popup_button_menu, R.menu.thread_context_delete_report_popup_menu);
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
            String itemText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            if (itemText == null)
                itemText = "";
            text += (text.isEmpty() ? "" : "<br/><br/>") + itemText;
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

    protected String planifyText(String text) {
        return text.replaceAll("<br/?>", "\n").replaceAll("<[^>]*>", "");
    }

    protected void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                getApplicationContext().getString(R.string.app_name),
                planifyText(text));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
    }

    protected String[] extractUrlsFromPosts(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor) adapter.getItem(i);
            if (cursor == null)
                continue;
            String itemText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT))
                    + " " + cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            if (itemText == null)
                itemText = "";
            text += (text.isEmpty() ? "" : "\n") + itemText;
        }
        text = text.replaceAll("\n", ""); // convert to single line
        if (DEBUG) Log.i(TAG, "extracted text: " + text);
        List<String> urlList = extractUrls(text);
        String[] urls = urlList.toArray(new String[urlList.size()]);
        return urls;
    }

    ;

    protected static List<String> extractUrls(String input) {
        List<String> result = new ArrayList<String>();

        Pattern pattern = Pattern.compile(
                "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                        "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
                        "|mil|biz|info|mobi|name|aero|jobs|museum" +
                        "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                        "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                        "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                        "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                        "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                        "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                        "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");

        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    protected AdapterView.OnItemClickListener absBoardListViewListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            ChanHelper.simulateClickAnim(ThreadActivity.this.getApplicationContext(), view);
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
                ChanHelper.launchUrlInBrowser(ThreadActivity.this, clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                startActivity(ThreadActivity.this, boardLink);
            }
            else {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                final long threadNoLink = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (boardCode.equals(boardLink) && threadNo == threadNoLink) { // already on this, do nothing
                }
                else if (boardCode.equals(boardLink)) { // just redisplay right tab
                    threadNo = threadNoLink;
                    refresh();
                }
                else {
                    startActivity(ThreadActivity.this, boardLink, threadNoLink);
                }
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            itemAdListener.onClick(view);
        else if ((flags & ChanPost.FLAG_IS_THREADLINK) > 0)
            itemThreadLinkListener.onClick(view);
        else if ((flags & ChanPost.FLAG_IS_BOARDLINK) > 0)
            itemBoardLinkListener.onClick(view);
        else if ((flags & (ChanPost.FLAG_HAS_IMAGE | ChanPost.FLAG_HAS_EXIF | ChanPost.FLAG_HAS_SPOILER)) > 0)
            itemExpandListener.onClick(view);
    }

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

    protected View.OnClickListener itemThreadLinkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            String linkedBoardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
            long linkedThreadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            if (linkedBoardCode == null || linkedBoardCode.isEmpty() || linkedThreadNo <= 0)
                return;
            if (absBoardListView != null && boardCode.equals(linkedBoardCode)) {
                threadNo = linkedThreadNo;
                refresh();
            }
            else {
                ThreadActivity.startActivity(ThreadActivity.this, linkedBoardCode, linkedThreadNo);
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
            if (linkedBoardCode != null && !linkedBoardCode.isEmpty())
                BoardActivity.startActivity(ThreadActivity.this, linkedBoardCode);
        }
    };

    protected View.OnClickListener itemExpandListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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
                return;
            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_EXPANDED))
                return;

            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_IMAGE | ChanPost.FLAG_HAS_EXIF | ChanPost.FLAG_HAS_SPOILER)) > 0) {
                (new ThreadExpandImageOnClickListener(getApplicationContext(), cursor, itemView)).onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_EXPANDED, new Boolean(true));
            }
        }
    };

    @Override
    public void setActionBarTitle() {
        final ActionBar a = getActionBar();
        if (a == null)
            return;
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        String boardTitle = (board == null ? "Board" : board.name) + " /" + boardCode + "/";

        ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
        String threadTitle = (thread == null || thread.posts == null || thread.posts.length == 0 || thread.posts[0] == null)
                ? " Thread " + threadNo
                : thread.posts[0].threadSubject(getApplicationContext())
                .replaceAll("<br/?>", " ")
                .replaceAll("<[^>]*>", "");
        a.setTitle(boardTitle + ": " + threadTitle);
        /*
        long lastFetched = 0;
        if (thread != null && thread.lastFetched > 0)
            lastFetched = thread.lastFetched;
        else if (board != null && board.lastFetched > 0)
            lastFetched = board.lastFetched;
        String timeSpan;
        long now = (new Date()).getTime();
        if (lastFetched <= 0)
            timeSpan = "last fetch unknown";
        else if (Math.abs(lastFetched - now) < 60000)
            timeSpan = "fetched just now";
        else
            timeSpan = "fetched " + DateUtils.getRelativeTimeSpanString(lastFetched,
                    now, 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        a.setSubtitle(timeSpan);
        */
        //a.setTitle(boardTitle);
        a.setDisplayShowTitleEnabled(true);
        a.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == null)
            return false;
        long[] postNos = absListView.getCheckedItemIds();
        SparseBooleanArray postPos = absListView.getCheckedItemPositions();
        switch (item.getItemId()) {
            // thread context info popup menu

            default:
                return false;
        }
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
        NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.PLAY_THREAD);
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
                                    //if (DEBUG) Log.v(TAG, "pos=" + pos + " listItem=" + listItem + " expandButton=" + expandButton);
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
        String text = "/" + boardCode + "/ " + getActionBar().getTitle().toString();
        String extraText = selectText(postPos);
        if (extraText != null && !extraText.isEmpty())
            text += "\n\n" + extraText.replaceAll("</?br/?>", "\n").replaceAll("<[^>]*>", "");
        ArrayList<String> paths = new ArrayList<String>();
        Cursor cursor = adapter.getCursor();
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
        handleUpdatedThreads();
        setActionBarTitle(); // for update time
        invalidateOptionsMenu(); // in case spinner needs to be reset
        if (handler != null) {
            handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
            if (absBoardListView != null)
                handler.sendEmptyMessageDelayed(1, LOADER_RESTART_INTERVAL_SHORT_MS);
        }

        if (actionMode != null)
            actionMode.finish();
    }

    @Override
    protected TutorialOverlay.Page tutorialPage() {
        return TutorialOverlay.Page.THREAD;
    }

    @Override
    protected void handleUpdatedThreads() {
        // we always auto-update at thread level
        if (absBoardListView != null)
            super.handleUpdatedThreads();
    }

}
