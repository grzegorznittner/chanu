package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.text.*;
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
import com.chanapps.four.fragment.ListOfLinksDialogFragment;
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
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        AbsListView.MultiChoiceModeListener
{

    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final int WATCHLIST_ACTIVITY_THRESHOLD = 7; // arbitrary from experience
    private static final int SNIPPET_LINES_DEFAULT = 3;
    private static final int TEXT_HORIZ_PADDING_DP = 8 + 28;
    private static final int IMAGE_WIDTH_DP = 80;
    private static final int SNIPPET_HEIGHT_DP = ((80 - 8)*3)/4; // three lines used for snippet

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected UserStatistics userStats = null;
    protected boolean inWatchlist = false;
    protected ThreadPostPopup threadPostPopup;
    protected ChanThread thread = null;

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
                        ChanHelper.POST_IMAGE_URL,
                        ChanHelper.POST_SHORT_TEXT,
                        ChanHelper.POST_TEXT,
                        ChanHelper.POST_TEXT,
                        ChanHelper.POST_COUNTRY_URL,
                        ChanHelper.POST_DATE_TEXT,
                        ChanHelper.POST_IMAGE_DIMENSIONS,
                        ChanHelper.POST_EXPAND_BUTTON,
                        ChanHelper.POST_EXPAND_BUTTON
                },
                new int[] {
                        R.id.list_item_header_bar,
                        R.id.list_item_expanded_progress_bar,
                        R.id.list_item_image_expanded,
                        R.id.list_item_image,
                        R.id.list_item_header,
                        R.id.list_item_snippet,
                        R.id.list_item_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_date,
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
    }

    protected void setupContextMenu() {
        absListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        absListView.setMultiChoiceModeListener(this);
        absListView.setOnCreateContextMenuListener(this);
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        if (DEBUG) Log.v(TAG, "setViewValue for  position=" + cursor.getPosition());
        if (DEBUG) Log.v(TAG, "                 boardCode=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_CODE)));
        if (DEBUG) Log.v(TAG, "                    postId=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_ID)));
        if (DEBUG) Log.v(TAG, "                      text=" + cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT)));

        switch (view.getId()) {
            case R.id.list_item :
                return setItem((ViewGroup)view, cursor);
            case R.id.list_item_header_bar:
                return setItemHeaderBar(view, cursor);
            case R.id.list_item_image_expanded:
                return setItemImageExpanded((ImageView) view, cursor);
            case R.id.list_item_expanded_progress_bar:
                return setItemImageExpandedProgressBar((ProgressBar) view, cursor);
            case R.id.list_item_image:
                return setItemImage((ImageView) view, cursor);
            case R.id.list_item_country_flag:
                return setItemCountryFlag((ImageView) view, cursor);
            case R.id.list_item_header:
                return setItemHeaderValue((TextView) view, cursor);
            case R.id.list_item_snippet:
                return setItemSnippetValue((TextView) view, cursor);
            case R.id.list_item_text:
                return setItemMessageValue((TextView)view, cursor);
            case R.id.list_item_date:
                return setItemDateValue((TextView)view, cursor);
            case R.id.list_item_image_exif:
                return setItemImageExifValue((TextView) view, cursor);
            default:
                return false;
        }
    }

    protected boolean setItem(ViewGroup item, Cursor cursor) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        item.setTag(adItem > 0 ? null : postId);
        int expandable = itemExpandable(cursor, item);
        ImageView expander = (ImageView)item.findViewById(R.id.list_item_expander);
        ImageView collapse = (ImageView)item.findViewById(R.id.list_item_collapse);
        if (DEBUG) Log.i(TAG, "pos=" + cursor.getPosition() + " expandable=" + expandable);
        if (adItem > 0) {
            item.setBackgroundColor(R.color.PaletteLighterGray);
            if (expander != null)
                expander.setVisibility(View.GONE);
        }
        else if (expandable > 0) {
            item.setBackgroundDrawable(null);
            if (expander != null)
                expander.setVisibility(View.VISIBLE);
        }
        else {
            item.setBackgroundDrawable(null);
            if (expander != null)
                expander.setVisibility(View.GONE);
        }
        if (collapse != null)
            collapse.setVisibility(View.GONE);
        return true;
    }

    protected boolean setItemHeaderBar(View view, Cursor cursor) {
        if (cursor.getPosition() == 0)
            view.setVisibility(View.VISIBLE);
        else
            view.setVisibility(View.GONE);
        return true;
    }

    protected View.OnClickListener itemAdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
            String adUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            if (adItem > 0 && adUrl != null && !adUrl.isEmpty())
                ChanHelper.launchUrlInBrowser(ThreadActivity.this, adUrl);
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

            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            int expandable = itemExpandable(cursor, itemView);
            if (DEBUG) Log.i(TAG, "clicked expandable=" + expandable);
            if (expandable == 0)
                return;

            (new ExpandImageOnClickListener(cursor, expandable, itemView)).onClick(itemView);
        }
    };

    private static final int TEXT_EXPANDABLE = 0x01;
    private static final int IMAGE_EXPANDABLE = 0x02;

    private int itemExpandable(Cursor cursor, View itemView) {
        final String postText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        TextView itemHeader = (TextView)itemView.findViewById(R.id.list_item_header);
        Spanned spanned = Html.fromHtml(postText);
        boolean textExpandable = textExpandable(itemHeader.getPaint(), spanned.toString(), imageUrl);
        boolean imageExpandable = imageUrl != null && !imageUrl.isEmpty();
        int expandable = 0;
        if (textExpandable)
            expandable |= TEXT_EXPANDABLE;
        if (imageExpandable)
            expandable |= IMAGE_EXPANDABLE;
        return expandable;
    }

    private boolean setItemHeaderValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        return true;
    }

    private boolean setItemSnippetValue(final TextView tv, final Cursor cursor) {
        int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0) {
            tv.setVisibility(View.INVISIBLE);
        }
        else {
            String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            Spanned spanned = Html.fromHtml(text);
            tv.setText(spanned);
            tv.setLines(SNIPPET_LINES_DEFAULT); // default num lines
            tv.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private boolean textExpandable(TextPaint tp, String text, String imageUrl) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int paddingWidth = ChanGridSizer.dpToPx(metrics, TEXT_HORIZ_PADDING_DP);
        int imageWidth = (imageUrl == null || imageUrl.isEmpty()) ? 0 : ChanGridSizer.dpToPx(metrics, IMAGE_WIDTH_DP);
        int textWidth = screenWidth - paddingWidth - imageWidth;
        int actualHeight = ChanGridSizer.dpToPx(metrics, SNIPPET_HEIGHT_DP);
        StaticLayout sl = new StaticLayout(text, tp, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        int fullHeight = sl.getHeight();
        boolean textExpandable = fullHeight > actualHeight;
        if (DEBUG) Log.v(TAG, "Header height actual=" + actualHeight + " full=" + fullHeight);
        return textExpandable;
    }

    private boolean setItemMessageValue(final TextView tv, final Cursor cursor) {
        tv.setVisibility(View.GONE);
        return true;
    }

    private boolean setItemDateValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_DATE_TEXT));
        tv.setText(text);
        return true;
    }

    private boolean setItemImageExifValue(final TextView tv, final Cursor cursor)
    {
        tv.setVisibility(View.GONE);
        return true;
    }

    private boolean setItemImage(final ImageView iv, final Cursor cursor)
    {
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl != null && !imageUrl.isEmpty()) {
            ViewGroup.LayoutParams params = iv.getLayoutParams();
            if (params != null) {
                int px = ChanGridSizer.dpToPx(getResources().getDisplayMetrics(), 80);
                params.width = px;
                iv.setLayoutParams(params);
            }
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(imageUrl, iv, displayImageOptions.modifyCenterCrop(true));
        }
        else {
            iv.setImageBitmap(null);
            ViewGroup.LayoutParams params = iv.getLayoutParams();
            if (params != null) {
                params.width = 0;
                iv.setLayoutParams(params);
            }
            iv.setVisibility(View.VISIBLE);
        }
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

    private boolean setItemImageExpandedProgressBar(final ProgressBar progressBar, final Cursor cursor) {
        progressBar.setVisibility(View.GONE);
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            return false;
        return true;
    }

    private class ExpandImageOnClickListener implements View.OnClickListener {

        private int expandable = 0;
        private ImageView itemExpander;
        private ImageView itemCollapse;
        private ImageView itemExpandedImage;
        private ProgressBar itemExpandedProgressBar;
        private TextView itemExpandedSnippet;
        private TextView itemExpandedText;
        private TextView itemExpandedExifText;
        private String postText = null;
        private String postImageUrl = null;
        private String postExifText = null;
        int postW = 0;
        int postH = 0;
        int listPosition = 0;
        String fullImageLocation = null;

        public ExpandImageOnClickListener(final Cursor cursor, final int expandable, final View itemView) {
            this.expandable = expandable;
            itemExpander = (ImageView)itemView.findViewById(R.id.list_item_expander);
            itemCollapse = (ImageView)itemView.findViewById(R.id.list_item_collapse);
            itemExpandedImage = (ImageView)itemView.findViewById(R.id.list_item_image_expanded);
            itemExpandedProgressBar = (ProgressBar)itemView.findViewById(R.id.list_item_expanded_progress_bar);
            itemExpandedSnippet = (TextView)itemView.findViewById(R.id.list_item_snippet);
            itemExpandedText = (TextView)itemView.findViewById(R.id.list_item_text);
            itemExpandedExifText = (TextView)itemView.findViewById(R.id.list_item_image_exif);
            
            listPosition = cursor.getPosition();
            postW = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
            postH = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
            postText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            postExifText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXIF_TEXT));
            long postTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
            String postExt = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXT));
            postImageUrl = postTim > 0 ? ChanPost.imageUrl(boardCode, postTim, postExt) : null;
            long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            fullImageLocation = ChanFileStorage.getBoardCacheDirectory(getBaseContext(), boardCode) + "/" + postId + postExt;
        }

        @Override
        public void onClick(View v) {
            if (DEBUG) Log.i(TAG, "handling click for pos=" + listPosition);
            if (itemCollapse.getVisibility() == View.VISIBLE) { // toggle expansion
                ChanHelper.clearBigImageView(itemExpandedImage);
                itemCollapse.setVisibility(View.GONE);
                itemExpander.setVisibility(View.VISIBLE);
                itemExpandedImage.setVisibility(View.GONE);
                itemExpandedText.setVisibility(View.GONE);
                itemExpandedExifText.setVisibility(View.GONE);
                itemExpandedSnippet.setLines(SNIPPET_LINES_DEFAULT); // default num lines
                itemExpandedSnippet.setVisibility(View.VISIBLE);
                if (DEBUG) Log.i(TAG, "collapsed pos=" + listPosition);
                return;
            }

            if (DEBUG) Log.i(TAG, "expanding pos=" + listPosition);
            // show that we can collapse view
            itemExpander.setVisibility(View.GONE);
            itemCollapse.setVisibility(View.VISIBLE);

            // set text visibility
            if (DEBUG) Log.i(TAG, "Setting post text len=" + (postText == null ? 0 : postText.length()));
            if ((expandable & TEXT_EXPANDABLE) > 0 && postText != null && !postText.isEmpty()) {
                if ((expandable & IMAGE_EXPANDABLE) > 0) { // image visible, remove the duplicate top text
                    itemExpandedSnippet.setVisibility(View.INVISIBLE);
                    itemExpandedText.setText(Html.fromHtml(postText));
                    itemExpandedText.setVisibility(View.VISIBLE);
                    if (DEBUG) Log.i(TAG, "Set image expand to visible, text to bottom");
                }
                else { // no image, so just expand to fill rest of space
                    int lc = itemExpandedSnippet.getLineCount();
                    itemExpandedSnippet.setLines(Math.max(lc, SNIPPET_LINES_DEFAULT));
                    itemExpandedSnippet.setVisibility(View.VISIBLE);
                    itemExpandedText.setVisibility(View.GONE);
                    if (DEBUG) Log.i(TAG, "No image to expand, set text to full height");
                }
            }
            else {
                itemExpandedText.setVisibility(View.GONE);
                if (DEBUG) Log.i(TAG, "No text to expand, setting text to gone");
            }

            if (DEBUG) Log.i(TAG, "Clearing existing image");
            ChanHelper.clearBigImageView(itemExpandedImage); // clear old image
            if (DEBUG) Log.i(TAG, "Existing image cleared");

            if (DEBUG) Log.i(TAG, "Found postImageUrl=" + postImageUrl);
            if ((expandable & IMAGE_EXPANDABLE) == 0 || postImageUrl == null || postImageUrl.isEmpty()) {// no image to display
                itemExpandedImage.setVisibility(View.GONE);
                itemExpandedExifText.setVisibility(View.GONE);
                if (DEBUG) Log.i(TAG, "No image found to expand, collapsing");
                return;
            }

            if (DEBUG) Log.i(TAG, "Post exif text len=" + (postExifText == null ? 0 : postExifText.length()));
            if (postExifText != null && !postExifText.isEmpty()) {
                itemExpandedExifText.setText(Html.fromHtml(postExifText));
                itemExpandedExifText.setVisibility(View.VISIBLE);
            }
            else {
                itemExpandedExifText.setVisibility(View.GONE);
            }

            // calculate image dimensions
            if (DEBUG) Log.i(TAG, "post size " + postW + "x" + postH);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            //int padding = ChanGridSizer.dpToPx(displayMetrics, 16);
            int maxWidth = displayMetrics.widthPixels;
            int maxHeight = maxWidth; // to avoid excessively big images
            itemExpandedImage.setMaxWidth(maxWidth);
            itemExpandedImage.setMaxHeight(maxHeight);
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
            ViewGroup.LayoutParams params = itemExpandedImage.getLayoutParams();
            if (params != null) {
                params.width = width;
                params.height = height;
                if (DEBUG) Log.i(TAG, "set expanded image size=" + width + "x" + height);
            }
            itemExpandedImage.setVisibility(View.VISIBLE);
            if (DEBUG) Log.i(TAG, "Set expanded image to visible");

            int lastPosition = absListView.getLastVisiblePosition();
            boolean shouldMove = listPosition >= lastPosition - 1;
            final int parentOffset = shouldMove ? 100 : 0; // allow for margin

            // set visibility delayed
            if (itemExpandedProgressBar != null)
                itemExpandedProgressBar.setVisibility(View.VISIBLE);
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
                    .fullSizeImageLocation(fullImageLocation)
                    .resetViewBeforeLoading()
                    .build();

            // display image async
            imageLoader.displayImage(postImageUrl, itemExpandedImage, expandedDisplayImageOptions, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted() {
                }

                @Override
                public void onLoadingFailed(FailReason failReason) {
                    if (itemExpandedProgressBar != null)
                        itemExpandedProgressBar.setVisibility(View.GONE);
                    itemExpandedImage.setVisibility(View.GONE);
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
                    if (itemExpandedProgressBar != null)
                        itemExpandedProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingCancelled() {
                    if (itemExpandedProgressBar != null)
                        itemExpandedProgressBar.setVisibility(View.GONE);
                    itemExpandedImage.setVisibility(View.GONE);
                    Toast.makeText(ThreadActivity.this, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
                }
            }); // load async
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (DEBUG) Log.i(TAG, "onCreateActionMode");
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thread_context_menu, menu);
        mode.setTitle(R.string.thread_context_select);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id, final boolean checked) {
        Log.i(TAG, "onItemCheckedStateChanged pos=" + position + " checked=" + checked);
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
                mode.finish();
                postReply(postNos);
                return true;
            case R.id.post_reply_all_quote_menu:
                String quoteText = selectQuoteText(postPos);
                mode.finish();
                postReply(quoteText);
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
            case R.id.go_to_link_menu:
                String[] urls = extractUrlsFromPosts(postPos);
                mode.finish();
                (new ListOfLinksDialogFragment(urls)).show(getSupportFragmentManager(), ListOfLinksDialogFragment.TAG);
                return true;
            case R.id.copy_text_menu:
                String selectText = selectText(postPos);
                mode.finish();
                copyToClipboard(selectText);
                //(new SelectTextDialogFragment(text)).show(getSupportFragmentManager(), SelectTextDialogFragment.TAG);
                return true;
            case R.id.download_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null, postNos);
                mode.finish();
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
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

    protected String[] extractUrlsFromPosts(SparseBooleanArray postPos) {
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
        int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            itemAdListener.onClick(view);
        else
            itemExpandListener.onClick(view);
    }
}
