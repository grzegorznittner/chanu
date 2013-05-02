package com.chanapps.four.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.v4.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
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

import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.task.HighlightRepliesTask;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.io.File;
import java.net.URI;
import java.util.*;

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
        MediaScannerConnection.OnScanCompletedListener
{
    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final int WATCHLIST_ACTIVITY_THRESHOLD = 7; // arbitrary from experience
    protected static final int ITEM_THUMB_WIDTH_DP = 80;
    protected static final int ITEM_THUMB_MAXHEIGHT_DP = ITEM_THUMB_WIDTH_DP;
    protected static final int ITEM_THUMB_EMPTY_DP = 8;
    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final int MAX_HTTP_GET_URL_LEN = 2000;

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected UserStatistics userStats = null;
    protected boolean inWatchlist = false;
    protected ChanThread thread = null;
    protected boolean shouldPlayThread = false;
    protected ShareActionProvider shareActionProvider = null;
    protected Map<String, Uri> checkedImageUris = new HashMap<String, Uri>(); // used for tracking what's in the media store
    protected ActionMode actionMode = null;
    protected Typeface subjectTypeface = null;
    protected int padding8DP = 0;
    protected MenuItem searchMenuItem;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (threadNo > 0) {
        	cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, query, absListView);
            if (DEBUG) Log.i(TAG, "Started loader for " + boardCode + "/" + threadNo);
            setProgressBarIndeterminateVisibility(true);
        }
        else {
            cursorLoader = null;
            setProgressBarIndeterminateVisibility(false);
        }
        return cursorLoader;
    }

    public static void startActivity(Activity from, String boardCode, long threadNo) {
        if (threadNo <= 0)
            startActivity(from, boardCode);
	    else
            from.startActivity(createIntentForActivity(from, boardCode, threadNo, ""));
    }

    public static void startActivityForSearch(Activity from, String boardCode, long threadNo, String query) {
        from.startActivity(createIntentForActivity(from, boardCode, threadNo, query));
    }

    public AbsListView getAbsListView() {
        return absListView;
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
            selectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(selectorIntent);
            finish();
        }
        if (threadNo == 0) {
            Intent boardIntent = createIntentForActivity(this, boardCode);
            boardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(boardIntent);
            finish();
        }
        // normal processing resumes
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
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .resetViewBeforeLoading()
                .build();
    }

    @Override
    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(this,
                R.layout.thread_list_item,
                this,
                new String[] {
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
                new int[] {
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
        setupContextMenu();
        absListView.setOnItemClickListener(this);
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected void setupContextMenu() {
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        absListView.setMultiChoiceModeListener(this);
        absListView.setOnCreateContextMenuListener(this);
    }

    protected Typeface ensureSubjectTypeface() {
        if (subjectTypeface == null)
            subjectTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Condensed.ttf");
        return subjectTypeface;
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        /*
        if (DEBUG) Log.v(TAG, "setViewValue for  position=" + cursor.getPosition());
        if (DEBUG) Log.v(TAG, "                 boardCode=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE)));
        if (DEBUG) Log.v(TAG, "                    postId=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_ID)));
        if (DEBUG) Log.v(TAG, "                      text=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT)));
        */
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        switch (view.getId()) {
            case R.id.list_item :
                return setItem((ViewGroup)view, cursor, flags);
            case R.id.list_item_header_wrapper:
                return setItemHeaderWrapper((ViewGroup)view, flags);
            case R.id.list_item_image_expanded:
                return setItemImageExpanded((ImageView) view, cursor, flags);
            case R.id.list_item_expanded_progress_bar:
                return setItemImageExpandedProgressBar((ProgressBar) view);
            case R.id.list_item_image_wrapper:
                return setItemImageWrapper((ViewGroup)view, cursor, flags);
            case R.id.list_item_image:
                return setItemImage((ImageView) view, cursor, flags);
            case R.id.list_item_country_flag:
                return setItemCountryFlag((ImageView) view, cursor, flags);
            case R.id.list_item_header:
                return setItemHeaderValue((TextView) view, cursor);
            case R.id.list_item_subject:
                return setItemSubject((TextView) view, cursor, flags);
            case R.id.list_item_title:
                return setItemTitle((TextView) view, cursor, flags);
            case R.id.list_item_text:
                return setItemText((TextView) view, cursor, flags);
            case R.id.list_item_image_exif:
                return setItemImageExifValue((TextView) view);
            default:
                return false;
        }
    }

    protected boolean setItem(ViewGroup item, Cursor cursor, int flags) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        item.setTag((flags | ChanPost.FLAG_IS_AD) > 0 ? null : postId);
        item.setTag(R.id.THREAD_VIEW_IS_EXPANDED, new Boolean(false));
        /*
        boolean clickable = (flags & (
                        ChanPost.FLAG_IS_AD |
                        ChanPost.FLAG_IS_THREADLINK |
                        ChanPost.FLAG_HAS_IMAGE |
                        ChanPost.FLAG_HAS_EXIF |
                        ChanPost.FLAG_HAS_SPOILER))
                        > 0;
        //item.setClickable(clickable);
        if (DEBUG) Log.i(TAG, "Exception postId=" + postId + " isClickable=" + clickable + " flags=" + flags);
        */
        return true;
    }

    protected boolean setItemHeaderWrapper(ViewGroup wrapper, int flags) {
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_ad_bg_gradient);
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_button_gradient_bg);
        else
            wrapper.setBackgroundResource(0);
        return true;
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
            if (linkedBoardCode != null && !linkedBoardCode.isEmpty() && linkedThreadNo > 0)
                ThreadActivity.startActivity(ThreadActivity.this, linkedBoardCode, linkedThreadNo);
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
            if ((Boolean)itemView.getTag(R.id.THREAD_VIEW_IS_EXPANDED))
                return;

            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_IMAGE | ChanPost.FLAG_HAS_EXIF | ChanPost.FLAG_HAS_SPOILER)) > 0) {
                (new ThreadExpandImageOnClickListener(ThreadActivity.this, cursor, itemView)).onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_EXPANDED, new Boolean(true));
            }
        }
    };

    private boolean setItemHeaderValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        return true;
    }

    private boolean setItemSubject(final TextView tv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_HAS_SUBJECT) == 0
                || (flags & (ChanPost.FLAG_IS_AD | ChanPost.FLAG_IS_TITLE)) > 0) {
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        if (cursor.getPosition() == 0) {
            ensureSubjectTypeface();
            tv.setTypeface(subjectTypeface);
        }
        else {
            tv.setTypeface(Typeface.DEFAULT);
        }
        tv.setText(spanned);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    private boolean setItemTitle(final TextView tv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_IS_TITLE) == 0) {
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    private boolean setItemText(final TextView tv, final Cursor cursor, int flags) {
        if (padding8DP == 0)
            padding8DP = ChanGridSizer.dpToPx(getResources().getDisplayMetrics(), 8);
        if ((flags & ChanPost.FLAG_IS_AD) == 0 && (flags & ChanPost.FLAG_HAS_TEXT) > 0) {
            String postText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            tv.setText(Html.fromHtml(postText));
            if (cursor.getPosition() != 0 && (flags & ChanPost.FLAG_HAS_IMAGE) > 0) // has image
                tv.setPadding(0, padding8DP, 0, padding8DP);
            else
                tv.setPadding(0, 0, 0, padding8DP);
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    private boolean setItemImageExifValue(final TextView tv) {
        tv.setVisibility(View.GONE);
        return true;
    }

    private boolean setItemImageWrapper(final ViewGroup layout, final Cursor cursor, int flags) {
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int widthDp = (flags & ChanPost.FLAG_HAS_IMAGE) > 0 ? ITEM_THUMB_WIDTH_DP : ITEM_THUMB_EMPTY_DP;
        params.width = ChanGridSizer.dpToPx(displayMetrics, widthDp);
        return true;
    }

    private ImageLoadingListener adImageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {}
        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            if (view instanceof ImageView) {
                imageLoader.displayImage(ChanAd.defaultImageUrl(), (ImageView)view, displayImageOptions);
            }
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {}
        @Override
        public void onLoadingCancelled(String imageUri, View view) {}
    };

    private boolean setItemImage(final ImageView iv, final Cursor cursor, int flags) {
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        if (params == null) { // something wrong in layout
            iv.setImageBitmap(null);
            return true;
        }
        if ((flags & ChanPost.FLAG_HAS_IMAGE) == 0) {
            iv.setImageBitmap(null);
            params.width = 0;
            params.height = 0;
            iv.setLayoutParams(params);
            return true;
        }

        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (tn_w == 0 || tn_h == 0) { // we don't have height and width, so just show unscaled image
            params.width = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
            params.height = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageLoader.displayImage(imageUrl, iv, displayImageOptions);
            return true;
        }

        // scale image
        boolean isFirst = cursor.getPosition() == 0;
        double scaleFactor = (double)tn_w / (double)tn_h;
        if (scaleFactor < 0.5 || (isFirst && scaleFactor < 1)) { // tall image, restrict by height
            //params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int desiredHeight = isFirst
                    ? getResources().getDisplayMetrics().heightPixels / 2
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
            params.width = (int)(scaleFactor * (double)desiredHeight);
            params.height = desiredHeight;
        }
        else {
            int desiredWidth = isFirst
                    ? displayMetrics.widthPixels
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
            params.width = desiredWidth; // restrict by width normally
            params.height = (int)((double)desiredWidth / scaleFactor);
            //params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        if (DEBUG) Log.i(TAG, "Input size=" + tn_w + "x" + tn_h + " output size=" + params.width + "x" + params.height);
        iv.setLayoutParams(params);
        //iv.setScaleType(ImageView.ScaleType.FIT_XY);
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            imageLoader.displayImage(imageUrl, iv, displayImageOptions, adImageLoadingListener);
        else
            imageLoader.displayImage(imageUrl, iv, displayImageOptions);

        return true;
    }

    private boolean setItemImageExpanded(final ImageView iv, final Cursor cursor, int flags) {
        iv.setVisibility(View.GONE);
        if ((flags & (ChanPost.FLAG_IS_AD | ChanPost.FLAG_IS_TITLE | ChanPost.FLAG_IS_THREADLINK)) == 0)
            iv.setOnClickListener(new ThreadImageOnClickListener(this, cursor));
        return true;
    }

    private boolean setItemImageExpandedProgressBar(final ProgressBar progressBar) {
        progressBar.setVisibility(View.GONE);
        return true;
    }

    private boolean setItemCountryFlag(final ImageView iv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_HAS_COUNTRY) > 0) {
            iv.setImageBitmap(null);
            iv.setVisibility(View.VISIBLE);
            String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_COUNTRY_URL));
            imageLoader.displayImage(countryFlagImageUrl, iv, displayImageOptions);
        }
        else {
            iv.setVisibility(View.GONE);
            iv.setImageBitmap(null);
        }
        return true;
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

    private void setupSearch(Menu menu) {
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.thread_search_menu);
        SearchView searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }

    protected UserStatistics ensureUserStats() {
        if (userStats == null) {
            userStats = ChanFileStorage.loadUserStats(getBaseContext());
        }
        return userStats;
    }

    public void incrementCounterAndAddToWatchlistIfActive() {
        ensureUserStats().threadUse(boardCode, threadNo);
        String key = boardCode + "/" + threadNo;
        ChanThreadStat stat = ensureUserStats().boardThreadStats.get(key);
        if (stat != null && stat.usage >= WATCHLIST_ACTIVITY_THRESHOLD && !inWatchlist)
            addToWatchlistIfNotAlreadyIn();
    }

    protected void addToWatchlistIfNotAlreadyIn() {
        int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        if (stringId == R.string.thread_added_to_watchlist)
            Toast.makeText(this, R.string.thread_added_to_watchlist_activity_based, Toast.LENGTH_SHORT).show();
        inWatchlist = true;
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (DEBUG) Log.i(TAG, "onCreateActionMode");
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thread_context_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.thread_context_share_action_menu);
        if (shareItem != null) {
            shareActionProvider = (ShareActionProvider)shareItem.getActionProvider();
        }
        else {
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
            Cursor cursor = (Cursor)adapter.getItem(i);
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
            Cursor cursor = (Cursor)adapter.getItem(i);
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

    protected String[] extractUrlsFromPosts(SparseBooleanArray postPos) {
        String text = "";
        for (int i = 0; i < absListView.getCount(); i++) {
            if (!postPos.get(i))
                continue;
            Cursor cursor = (Cursor)adapter.getItem(i);
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
    };

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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            itemAdListener.onClick(view);
        else if ((flags & ChanPost.FLAG_IS_THREADLINK) > 0)
            itemThreadLinkListener.onClick(view);
        else if ((flags & (ChanPost.FLAG_HAS_IMAGE | ChanPost.FLAG_HAS_EXIF | ChanPost.FLAG_HAS_SPOILER)) > 0)
            itemExpandListener.onClick(view);
    }

    @Override
    public void setActionBarTitle() {
        final ActionBar a = getActionBar();
        if (a == null)
            return;
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        String boardTitle = (board == null ? "Board" : board.name) + " /" + boardCode + "/";
        /*
        ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
        String threadTitle = (thread == null || thread.posts == null || thread.posts.length == 0 || thread.posts[0] == null)
                ? " Thread " + threadNo
                : thread.posts[0].threadSubject(getApplicationContext())
                    .replaceAll("<br/?>", " ")
                    .replaceAll("<[^>]*>", "");
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
        a.setTitle(boardTitle + ": " + threadTitle);
        a.setSubtitle(timeSpan);
        */
        a.setTitle(boardTitle);
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
        }
        catch (UnsupportedEncodingException e) {
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
                        }
                        catch (InterruptedException e) {
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
        if (absListView.getLastVisiblePosition() >= absListView.getAdapter().getCount() -1)
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
        }
        else {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            ArrayList<String> missingPaths = new ArrayList<String>();
            for (String path : paths) {
                if (checkedImageUris.containsKey(path)) {
                    Uri uri = checkedImageUris.get(path);
                    uris.add(uri);
                    if (DEBUG) Log.i(TAG, "Added uri=" + uri);
                }
                else {
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
        super.refresh();
        if (actionMode != null)
            actionMode.finish();
    }

    public void closeSearch() {
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

}
