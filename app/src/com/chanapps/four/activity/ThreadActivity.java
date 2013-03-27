package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractThreadCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.SelectTextDialogFragment;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.util.Arrays;

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
        AdapterView.OnItemClickListener,
        AbsListView.MultiChoiceModeListener
{

    protected static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final int WATCHLIST_ACTIVITY_THRESHOLD = 7; // arbitrary from experience

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected UserStatistics userStats = null;
    protected boolean inWatchlist = false;
    protected ThreadPostPopup threadPostPopup;
    protected ChanThread thread = null;
    protected int threadPos = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (threadNo > 0) {
        	cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, absListView);
            if (DEBUG) Log.i(TAG, "Started loader for " + boardCode + "/" + threadNo);
            setProgressOn(true);
        }
        else {
            cursorLoader = null;
            setProgressOn(false);
        }
        return cursorLoader;
    }

    public static void startActivity(Activity from, String boardCode, long threadNo) {
        if (threadNo <= 0)
            startActivity(from, boardCode);
	    else
            from.startActivity(createIntentForActivity(from, boardCode, threadNo));
    }

    public static Intent createIntentForActivity(Context context, final String boardCode, final long threadNo) {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
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
        if (intent.getBooleanExtra(ChanHelper.TRIGGER_BOARD_REFRESH, false)) {
        	FetchChanDataService.scheduleBoardFetch(getBaseContext(), boardCode);
        }
        if (DEBUG) Log.i(TAG, "Thread intent is: " + intent.getStringExtra(ChanHelper.BOARD_CODE) + "/" + intent.getLongExtra(ChanHelper.THREAD_NO, 0));
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
                .imageScaleType(ImageScaleType.POWER_OF_2)
                .resetViewBeforeLoading()
                .build();
    }

    @Override
    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(this,
                R.layout.thread_list_item,
                this,
                new String[] {
                        ChanHelper.POST_IMAGE_URL,
                        ChanHelper.POST_IMAGE_URL,
                        ChanHelper.POST_IMAGE_URL,
                        ChanHelper.POST_SHORT_TEXT,
                        ChanHelper.POST_TEXT,
                        ChanHelper.POST_COUNTRY_URL,
                        ChanHelper.POST_DATE_TEXT,
                        ChanHelper.POST_IMAGE_DIMENSIONS,
                        ChanHelper.POST_EXPAND_BUTTON,
                        ChanHelper.POST_EXPAND_BUTTON
                },
                new int[] {
                        R.id.list_item_expanded_progress_bar,
                        R.id.list_item_image_expanded,
                        R.id.list_item_image,
                        R.id.list_item_header,
                        R.id.list_item_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_date,
                        R.id.list_item_image_overlay
                });
        absListView.setAdapter(adapter);
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        absListView.setMultiChoiceModeListener(this);
        absListView.setOnCreateContextMenuListener(this);
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
        absListView = (ListView)findViewById(R.id.thread_list_view);
    }

    @Override
    protected void createAbsListView() {
        setAbsListViewClass();
        setContentView(getLayoutId());
        initAbsListView();
        initAdapter();
        absListView.setOnItemClickListener(this);
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        if (DEBUG) Log.v(TAG, "setViewValue for  position=" + cursor.getPosition());
        if (DEBUG) Log.v(TAG, "                 boardCode=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_CODE)));
        if (DEBUG) Log.v(TAG, "                    postId=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_ID)));
        if (DEBUG) Log.v(TAG, "                      text=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT)));

        switch (view.getId()) {
            case R.id.list_item :
                return setItemBackground((ViewGroup)view, cursor);
            case R.id.list_item_image_expanded:
                return setItemImageExpanded((ImageView)view, cursor);
            case R.id.list_item_image:
                return setItemImage((ImageView)view, cursor);
            case R.id.list_item_country_flag:
                return setItemCountryFlag((ImageView)view, cursor);
            case R.id.list_item_header:
                return setItemHeaderValue((TextView)view, cursor);
            case R.id.list_item_text:
                return setItemMessageValue((TextView)view, cursor);
            case R.id.list_item_date:
                return setItemDateValue((TextView)view, cursor);
            case R.id.list_item_image_overlay:
                return setItemImageOverlayValue((TextView) view, cursor);
            default:
                return false;
        }
    }

    protected boolean setItemBackground(ViewGroup item, Cursor cursor) {
        SparseBooleanArray positions = absListView.getCheckedItemPositions();
        if (positions.get(cursor.getPosition()))
            item.setBackgroundColor(R.color.PaletteBlueHalfOpacity);
        else
            item.setBackgroundDrawable(null);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = adapter.getCursor();
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (adItem > 0) {
            String adUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            ChanHelper.launchUrlInBrowser(ThreadActivity.this, adUrl);
        }
        else if (imageUrl != null && !imageUrl.isEmpty()) {
            LinearLayout threadListItem = (LinearLayout)view;
            ImageView itemExpandedImage = (ImageView)threadListItem.findViewById(R.id.list_item_image_expanded);
            ProgressBar itemExpandedProgressBar = (ProgressBar)threadListItem.findViewById(R.id.list_item_expanded_progress_bar);
            ExpandImageOnClickListener listener = new ExpandImageOnClickListener(cursor, itemExpandedImage, itemExpandedProgressBar);
            listener.onClick(view);
        }
    }

    private boolean setItemHeaderValue(final TextView tv, final Cursor cursor) {
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        tv.setText(Html.fromHtml(shortText));
        return true;
    }

    private boolean setItemMessageValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
        }
        else {
            tv.setText(Html.fromHtml(text));
            tv.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private boolean setItemDateValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_DATE_TEXT));
        tv.setText(text == null ? "" : text);
        return true;
    }


    private boolean setItemImageOverlayValue(final TextView tv, final Cursor cursor)
    {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_DIMENSIONS));
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
            return true;
        }
        tv.setText(text);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    private boolean setItemImage(final ImageView iv, final Cursor cursor)
    {
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl != null && !imageUrl.isEmpty())
            imageLoader.displayImage(imageUrl, iv, displayImageOptions);
        else
            iv.setImageBitmap(null);
        return true;
    }

    private boolean setItemImageExpanded(final ImageView iv, final Cursor cursor) {
        iv.setVisibility(View.GONE);
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            return false;
        iv.setOnClickListener(new ImageOnClickListener(cursor));
        return true;
    }

    private class ExpandImageOnClickListener implements View.OnClickListener {

        private ImageView itemExpandedImageHolder;
        private ProgressBar itemExpandedProgressBarHolder;
        private String postImageUrl = null;
        int postW = 0;
        int postH = 0;
        int listPosition = 0;

        public ExpandImageOnClickListener(final Cursor cursor,
                                          final ImageView itemExpandedImage,
                                          final ProgressBar itemExpandedProgressBar)
        {
            itemExpandedImageHolder = itemExpandedImage;
            itemExpandedProgressBarHolder = itemExpandedProgressBar;
            listPosition = cursor.getPosition();

            postW = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
            postH = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
            long postTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
            String postExt = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXT));
            postImageUrl = postTim > 0 ? ChanPost.getImageUrl(boardCode, postTim, postExt) : null;
        }

        @Override
        public void onClick(View v) {
            if (itemExpandedImageHolder == null)
                return;
            if (itemExpandedImageHolder.getVisibility() == View.VISIBLE) {
                ChanHelper.clearBigImageView(itemExpandedImageHolder);
                itemExpandedImageHolder.setVisibility(View.GONE);
            }
            else if (postImageUrl != null) {
                ChanHelper.clearBigImageView(itemExpandedImageHolder);

                // calculate image dimensions
                if (DEBUG) Log.i(TAG, "post size " + postW + "x" + postH);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                //int padding = ChanGridSizer.dpToPx(displayMetrics, 16);
                int maxWidth = displayMetrics.widthPixels;
                int maxHeight = maxWidth; // to avoid excessively big images
                itemExpandedImageHolder.setMaxWidth(maxWidth);
                itemExpandedImageHolder.setMaxHeight(maxHeight);
                if (DEBUG) Log.i(TAG, "max size " + maxWidth + "x" + maxHeight);
                float scaleFactor = 1;
                if (postW >= postH) {
                    // square or wide image, base sizing on width
                    if (postW > maxWidth)
                        scaleFactor = (float)maxWidth / (float)postW;
                }
                else {
                    // tall image
                    if (postH > maxHeight)
                        scaleFactor = (float)maxHeight / (float)postH;
                }
                int width = Math.round(scaleFactor * (float)postW);
                int height = Math.round(scaleFactor * (float)postH);
                if (DEBUG) Log.i(TAG, "target size " + width + "x" + height);
                // set layout dimensions
                ViewGroup.LayoutParams params = itemExpandedImageHolder.getLayoutParams();
                if (params != null) {
                    params.width = width;
                    params.height = height;
                }
                itemExpandedImageHolder.setVisibility(View.VISIBLE);
                /*
                if (itemExpandedProgressBarHolder != null) {
                    int progressBarPx = ChanGridSizer.dpToPx(getResources().getDisplayMetrics(), 96);
                    int progressBarPaddingWidth = Math.max(0, width - progressBarPx);
                    int progressBarPaddingHeight = Math.max(0, height - progressBarPx);
                    itemExpandedProgressBarHolder.setPadding(0, progressBarPaddingHeight/2, 0, 0);
                }
                */

                // calculate auto-scroll on image expand
                ViewParent parent = v.getParent();
                int parentHeight = 0;
                if (parent instanceof View) {
                    View parentView = (View)parent;
                    parentHeight = parentView.getHeight();
                }
                int lastPosition = absListView.getLastVisiblePosition();
                boolean shouldMove = listPosition >= lastPosition - 1;
                final int parentOffset = shouldMove ? parentHeight + 50 : 0; // allow for margin
                //final int imageOffset = shouldMove ? parentHeight + maxHeight : 0;
                final int imageOffset = 0;

                // set visibility delayed
                if (itemExpandedProgressBarHolder != null)
                    itemExpandedProgressBarHolder.setVisibility(View.VISIBLE);
                ensureHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        absListView.smoothScrollBy(parentOffset, 250);
                    }
                }, 250);

                ImageSize imageSize = new ImageSize(width, height);
                DisplayImageOptions expandedDisplayImageOptions = new DisplayImageOptions.Builder()
                        .imageScaleType(ImageScaleType.EXACT)
                        .cacheOnDisc()
                        .imageSize(imageSize)
                        .resetViewBeforeLoading()
                        .build();

                // display image async
                imageLoader.displayImage(postImageUrl, itemExpandedImageHolder, expandedDisplayImageOptions, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted() {
                    }

                    @Override
                    public void onLoadingFailed(FailReason failReason) {
                        if (itemExpandedProgressBarHolder != null)
                            itemExpandedProgressBarHolder.setVisibility(View.GONE);
                        itemExpandedImageHolder.setVisibility(View.GONE);
                        String reason = failReason.toString();
                        String msg;
                        if (reason.equalsIgnoreCase("io_error"))
                            msg = getString(R.string.thread_couldnt_download_image);
                        else
                            msg = String.format(getString(R.string.thread_couldnt_load_image), failReason.toString().toLowerCase().replaceAll("_", " "));
                        Toast.makeText(ThreadActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadingComplete(Bitmap loadedImage) {
                        if (itemExpandedProgressBarHolder != null)
                            itemExpandedProgressBarHolder.setVisibility(View.GONE);
                        absListView.smoothScrollBy(imageOffset, 250);
                        //absListView.smoothScrollToPositionFromTop(listPosition, parentOffset);
                    }

                    @Override
                    public void onLoadingCancelled() {
                        if (itemExpandedProgressBarHolder != null)
                            itemExpandedProgressBarHolder.setVisibility(View.GONE);
                        itemExpandedImageHolder.setVisibility(View.GONE);
                        Toast.makeText(ThreadActivity.this, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
                    }
                }); // load async
            }
        }
    }

    private class ImageOnClickListener implements View.OnClickListener {

        long postId = 0;
        long resto = 0;
        int w = 0;
        int h = 0;
        int position = 0;

        public ImageOnClickListener(Cursor cursor) {
            postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
            w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
            h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
            position = cursor.getPosition();
        }

        @Override
        public void onClick(View v) {
            ChanHelper.fadeout(ThreadActivity.this, v);
            incrementCounterAndAddToWatchlistIfActive();
            GalleryViewActivity.startActivity(
                    ThreadActivity.this, boardCode, threadNo, postId, position);
        }
    }

    private boolean setItemCountryFlag(final ImageView iv, final Cursor cursor) {
        String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
        if (countryFlagImageUrl != null && !countryFlagImageUrl.isEmpty())
            imageLoader.displayImage(countryFlagImageUrl, iv, displayImageOptions);
        else
            iv.setImageBitmap(null);
        return true;
    }


    private boolean ensureThreadCache() {
        if (thread == null)
            thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
        if (thread == null || thread.posts == null || thread.posts.length == 0)
            return false; // couldn't get thread
        return true;
    }

    private ChanPost getPost(final Cursor cursor) {
        int pos = cursor.getPosition();
        if (pos == 0) { // the thread
            threadPos = 0; // index for tracking current post
        }
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        while (threadPos < thread.posts.length && thread.posts[threadPos].no != postNo) // spin past removed posts
            threadPos++;
        if (threadPos >= thread.posts.length)
            return null; // couldn't find post
        return thread.posts[threadPos];
    }

    @Override
    protected void setAbsListViewClass() {
        absListViewClass = ListView.class;
    }

    private void postReply() {
        long[] postNos = {};
        postReply(postNos);
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
        replyIntent.putExtra(ChanHelper.POST_NO, 0);
        replyIntent.putExtra(ChanHelper.TIM, tim);
        replyIntent.putExtra(ChanHelper.TEXT, planifyText(replyText));
        startActivity(replyIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
            case R.id.refresh_menu:
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.post_reply_menu:
                postReply();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.download_all_images_menu:
            	ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.download_all_images_to_gallery_menu:
            	ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.go_to_top_position_menu:
                if (absListView != null && absListView.getAdapter() != null && absListView.getAdapter().getCount() > 0)
                    absListView.setSelection(0);
                return true;
            case R.id.go_to_end_position_menu:
                if (absListView != null && absListView.getAdapter() != null && absListView.getAdapter().getCount() > 0)
                    absListView.setSelection(absListView.getAdapter().getCount());
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_thread_list);
                rawResourceDialog.show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.i(TAG, "onCreateOptionsMenu called");
        int menuId = ChanBoard.showNSFW(this) ? R.menu.thread_menu_adult : R.menu.thread_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        return true;
    }

    @Override
    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null) {
            return;
        }
        //a.setTitle(String.valueOf(threadNo));
        //a.setDisplayShowTitleEnabled(true);
        a.setDisplayShowTitleEnabled(false);
        a.setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu();
    }

    protected void initPopup() {
        threadPostPopup = new ThreadPostPopup(this,
                this.getLayoutInflater(),
                imageLoader,
                displayImageOptions,
                (AbstractThreadCursorAdapter)adapter);
    }

    protected UserStatistics ensureUserStats() {
        if (userStats == null) {
            userStats = ChanFileStorage.loadUserStats(getBaseContext());
        }
        return userStats;
    }

    protected void incrementCounterAndAddToWatchlistIfActive() {
        ensureUserStats().threadUse(boardCode, threadNo);
        String key = boardCode + "/" + threadNo;
        ChanThreadStat stat = ensureUserStats().boardThreadStats.get(key);
        if (stat != null && stat.usage >= WATCHLIST_ACTIVITY_THRESHOLD && !inWatchlist) {
            int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
            if (stringId == R.string.thread_added_to_watchlist)
                Toast.makeText(this, R.string.thread_added_to_watchlist_activity_based, Toast.LENGTH_SHORT).show();
            inWatchlist = true;
        }
    }

    protected void addToWatchlist() {
        int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
        inWatchlist = true;
    }

    public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo);
	}

    @Override
    protected int getLayoutId() {
        return R.layout.thread_list_layout;
    }

    protected ThreadPostPopup ensurePopup() {
        if (threadPostPopup == null) {
            initPopup();
        }
        return threadPostPopup;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thread_context_menu, menu);
        mode.setTitle(R.string.thread_context_select);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (DEBUG) Log.i(TAG, "onPrepareActionMode");
        prehighlightItems();
        return true;
    }

    protected void prehighlightItems() {
        SparseBooleanArray positions = absListView.getCheckedItemPositions();
        if (DEBUG) Log.i(TAG, "prehighlightItems size=" + positions.size());
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i))
                highlightItem(i, true);
        }
    }

    protected void highlightItem(int position, boolean checked) {
        if (DEBUG) Log.i(TAG, "highlightItem pos=" + position + " checked=" + checked);
        for (int i = 0; i < absListView.getChildCount(); i++) {
            View child = absListView.getChildAt(i);
            if (child == null)
                continue;
            int childPosition = absListView.getPositionForView(child);
            if (DEBUG) Log.v(TAG, "child Id: " + child.getId() + " childPosition:" + childPosition);
            if (childPosition != position)
                continue;
            if (DEBUG) Log.i(TAG, "Found item to highlight pos=" + position + " child=" + child + " checked=" + checked);
            if (checked)
                child.setBackgroundColor(R.color.PaletteBlueHalfOpacity);
            else
                child.setBackgroundDrawable(null);

            break;
        }
    }

    @Override
    public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id, final boolean checked) {
        Log.i(TAG, "onItemCheckedStateChanged pos=" + position + " checked=" + checked);
        int delayMs = absListView.getCheckedItemCount() > 1 ? 10 : 250; // need to wait for list view to display
        if (handler != null)
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    highlightItem(position, checked);
                }
            }, delayMs);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.post_reply_all_menu:
                long[] postNos = absListView.getCheckedItemIds();
                if (DEBUG) Log.i(TAG, "Post nos: " + Arrays.toString(postNos));
                mode.finish();
                postReply(postNos);
                return true;
            case R.id.post_reply_all_quote_menu:
                SparseBooleanArray postPos = absListView.getCheckedItemPositions();
                String quoteText = selectQuoteText(postPos);
                mode.finish();
                postReply(quoteText);
                return true;
            case R.id.select_text_menu:
                SparseBooleanArray postCheckedPos = absListView.getCheckedItemPositions();
                String selectText = selectText(postCheckedPos);
                mode.finish();
                copyToClipboard(selectText);
                //(new SelectTextDialogFragment(text)).show(getSupportFragmentManager(), SelectTextDialogFragment.TAG);
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
                //highlightItem(i, false);
            }
        }
        // nothing
    }

    protected String selectText(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor)adapter.getItem(i);
            if (cursor == null)
                continue;
            String itemText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            if (itemText == null)
                itemText = "";
            text += (text.isEmpty() ? "" : "\n\n") + itemText;
        }
        if (DEBUG) Log.i(TAG, "Selected text: " + text);
        return text;
    }

    protected String selectQuoteText(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor)adapter.getItem(i);
            if (cursor == null)
                continue;
            String postNo = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_ID));
            String itemText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            if (itemText == null)
                itemText = "";
            String postPrefix = ">>" + postNo + "<br/>";
            text += (text.isEmpty() ? "" : "<br/><br/>") + postPrefix + ChanPost.quoteText(itemText);
        }
        if (DEBUG) Log.i(TAG, "Selected quote text: " + text);
        return text;
    }

    protected String planifyText(String text) {
        return text.replaceAll("<br/?>", "\n").replaceAll("<[^>]*>","");
    }

    protected void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                getApplicationContext().getString(R.string.app_name),
                planifyText(text));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
    }

}
