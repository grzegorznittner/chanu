package com.chanapps.four.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.*;
import com.chanapps.four.service.ChanLoadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/23/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanViewHelper {

    public enum ViewType {
        LIST,
        GRID
    }

    public enum ServiceType {
        SELECTOR,
        BOARD,
        THREAD,
        WATCHLIST
    }

    private static final String TAG = ChanViewHelper.class.getSimpleName();

    private Activity activity;
    private DisplayImageOptions options;
    private ImageLoader imageLoader;
    private long tim;
    private String boardCode;
    private int pageNo = 0;
    private long threadNo = 0;
    private String text;
    private String imageUrl;
    private int imageWidth;
    private int imageHeight;
    private boolean hideAllText = false;
    private ServiceType serviceType;

    private View popupView;
    private TextView popupText;
    private PopupWindow popupWindow;
    private Button replyButton;
    private Button quoteButton;
    private Button dismissButton;

    public ChanViewHelper(Activity activity, ServiceType serviceType) {
        this(activity, getViewTypeFromOrientation(activity), serviceType);
    }

    public ChanViewHelper(Activity activity, ViewType viewType, ServiceType serviceType) {
        this.activity = activity;
        this.serviceType = serviceType;
        initFieldsFromIntent();
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(activity));
        if (viewType == ViewType.LIST) {
            options = new DisplayImageOptions.Builder()
    // for text-only posts this image is still there, causing display problems
    //			.showImageForEmptyUri(R.drawable.stub_image)
    			.cacheOnDisc()
    			.imageScaleType(ImageScaleType.EXACT)
    			.build();
            }
        else {
            options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.stub_image)
			    .cacheOnDisc()
			    .imageScaleType(ImageScaleType.EXACT)
			    .build();
        }
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
        final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
        String rawShortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        String rawText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        String rawImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        String shortText = rawShortText == null ? "" : rawShortText;
        String text = rawText == null ? "" : rawText;
        String imageUrl = rawImageUrl == null ? "" : rawImageUrl;
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
            switch (getViewType()) {
                case GRID:
                    if (loadPage > 0) {
                        // doesn't have text
                    }
                    else if (lastPage > 0) {
                        // text is already set
                    }
                    else if ((threadNo != 0 && hideAllText) || shortText == null || shortText.isEmpty()) {
                        tv.setVisibility(View.INVISIBLE);
                    }
                    else if (!imageUrl.isEmpty()) {
                        setGridViewText(tv, shortText, cursor);
                    }
                    else {
                        setGridViewText(tv, text, cursor);
                    }
                    break;
                default:
                    setListViewText(tv, text, cursor);
                    break;
            }
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            if (loadPage > 0) {
                Animation rotation = AnimationUtils.loadAnimation(activity, R.animator.clockwise_refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
            }
            else if (lastPage > 0) {
                // nothing
            }
            else {
                setViewImage(iv, imageUrl, cursor);
            }
            // making this invisible causes display problems
            //if (imageUrl != null && !imageUrl.isEmpty()) {
            //setViewImage(iv, imageUrl, cursor);
            //}
            //else {
            //    iv.setVisibility(View.INVISIBLE);
            //}
            return true;
        } else {
            return false;
        }
    }

    public void setListViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_H));
        //Log.i(TAG, "tn_w=" + tn_w + ", tn_h=" + tn_h);
        Point imageDimensions = new Point(tn_w, tn_h);
        if (imageDimensions != null && imageDimensions.x > 0 && imageDimensions.y > 0) {
            text = text == null ? "" : text;
            FlowTextHelper.tryFlowText(text, imageDimensions, textView);
        } else {
            textView.setText(text);
        }
    }

    public void setGridViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        textView.setText(text);
    }

    public void setViewImage(ImageView imageView, final String thumbnailImageUrl, Cursor cursor) {
        try {
            this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
        } catch (NumberFormatException nfe) {
            imageView.setImageURI(Uri.parse(thumbnailImageUrl));
        }
    }

    private void setBoardMenu() {
        ActionBar a = activity.getActionBar();
        if (a == null) {
            return;
        }
        int stringId = threadNo == 0
                ? R.string.board_activity
                : R.string.thread_activity;
        String title = "/" + boardCode + " " + activity.getString(stringId);
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    private void reloadPrefs(SharedPreferences prefs) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        }
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
    }

    public void onRefresh() {
        reloadPrefs(null);
    }

    private void loadBoardCode(SharedPreferences prefs) {
        String oldBoardCode = prefs.getString(ChanHelper.BOARD_CODE, "s");
        boardCode = "s";
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            if (boardCode == null || boardCode.isEmpty()) {
                boardCode = "s";
            }
            Log.i(TAG, "Board code read from intent: " + boardCode);
        }
        if (intent == null || !intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(activity, boardCode)) {
            boardCode = oldBoardCode;
            Log.i(TAG, "Board code loaded from prefs: " + boardCode);
        }
        if (!oldBoardCode.equals(boardCode)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.commit();
        }
    }

    private void loadPageNo(SharedPreferences prefs) {
        int oldPageNo = prefs.getInt(ChanHelper.PAGE, 0);
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.PAGE)) {
            pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        }
        if (oldPageNo != pageNo) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.putInt(ChanHelper.PAGE, pageNo);
            ed.commit();
        }
    }

    private void loadThreadNo(SharedPreferences prefs) {
        long oldThreadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
        threadNo = 0;
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            Log.i(TAG, "Thread no read from intent: " + threadNo);
            tim = intent.getLongExtra(ChanHelper.TIM, 0);
            text = intent.getStringExtra(ChanHelper.TEXT);
            imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
            imageWidth = intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0);
            imageHeight = intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0);
        }
        if (intent == null || !intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = oldThreadNo;
            Log.i(TAG, "Thread no loaded from prefs: " + threadNo);
        }
        if (oldThreadNo != threadNo) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putLong(ChanHelper.TIM, tim);
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.putInt(ChanHelper.PAGE, pageNo);
            ed.putLong(ChanHelper.THREAD_NO, threadNo);
            ed.putString(ChanHelper.TEXT, text);
            ed.putString(ChanHelper.IMAGE_URL, imageUrl);
            ed.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
            ed.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
            ed.commit();
        }
    }

    public ViewType getViewType() {
        //return getViewTypeFromOrientation(activity);
        return ViewType.GRID;
    }

    public void startService() {
        if (serviceType == ServiceType.BOARD) {
            pageNo = 0;
        }
        startService(serviceType);
    }

    public void resetPageNo() {
        pageNo = 0;

    }
    public void loadNextPage() {
        if (serviceType == ServiceType.BOARD) {
            pageNo++;
            startService(ServiceType.BOARD);
        }
    }

    private void startService(ServiceType serviceType) {
        if (serviceType == ServiceType.WATCHLIST) {
            //startWatchlistService();
            return;
        }
        initFieldsFromIntent();
        Log.i(TAG, "Starting ChanLoadService board " + boardCode + " page " + pageNo + " thread " + threadNo );
        Intent intent = new Intent(activity, ChanLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        activity.startService(intent);
    }

    private void initFieldsFromIntent() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        reloadPrefs(prefs);
        loadBoardCode(prefs);
        if (serviceType == ServiceType.BOARD) {
            loadPageNo(prefs);
        }
        if (serviceType == ServiceType.THREAD) {
            loadThreadNo(prefs);
        }
        else {
            threadNo = 0;
        }
        setBoardMenu();
    }

    public static final void startBoardActivity(AdapterView<?> adapterView, View view, int position, long id, Activity activity, String boardCode) {
        Intent intent = new Intent(activity, BoardActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        activity.startActivity(intent);
    }

    public void startThreadActivity(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long threadTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        Log.d(TAG, "threadTim: " + threadTim);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final String boardName = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        final int tn_w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_W));
        final int tn_h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_H));
        Intent intent = new Intent(activity, ThreadActivity.class);
        intent.putExtra(ChanHelper.TIM, threadTim);
        intent.putExtra(ChanHelper.BOARD_CODE, boardName != null ? boardName : boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, postId);
        intent.putExtra(ChanHelper.TEXT, text);
        intent.putExtra(ChanHelper.IMAGE_URL, imageUrl);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, tn_w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, tn_h);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, adapterView.getFirstVisiblePosition());
        Log.i(TAG, "Calling thread activity with id=" + id);
        activity.startActivity(intent);
    }

    public void startFullImageActivity(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final int w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
        final int h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
        Intent intent = new Intent(activity, FullScreenImageActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postId);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, h);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, activity.getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, adapterView.getFirstVisiblePosition());
        activity.startActivity(intent);
    }

    private void ensurePopupWindow() {
        if (popupView == null) {
            popupView = activity.getLayoutInflater().inflate(R.layout.popup_full_text_layout, null);
            popupText = (TextView)popupView.findViewById(R.id.popup_full_text);
            popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
            quoteButton = (Button)popupView.findViewById(R.id.popup_quote_button);
            dismissButton = (Button)popupView.findViewById(R.id.popup_dismiss_button);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
        }
    }

    public boolean showPopupText(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        Log.i(TAG, "Calling popup with id=" + id);
        if (text != null && !text.trim().isEmpty()) {
            final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
            final long clickedThreadNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            final int currentPosition = adapterView.getFirstVisiblePosition();
            ensurePopupWindow();
            popupText.setText(text);
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(activity, PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, currentPosition);
                    activity.startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(activity, PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.TEXT, text);
                    replyIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, currentPosition);
                    activity.startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
            popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
            return true;
        }
        else {
            return false;
        }
    }

    public long getTim() {
        return tim;
    }

    public String getBoardCode() {
        return boardCode;
    }

    public int getPageNo() {
        return pageNo;
    }

    public long getThreadNo() {
        return threadNo;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public ViewType getViewTypeFromOrientation() {
        return getViewTypeFromOrientation(activity);
    }

    public static final ViewType getViewTypeFromOrientation(Context activity) {
        return
                ChanHelper.getOrientation(activity) == ChanHelper.Orientation.PORTRAIT
                ? ChanViewHelper.ViewType.GRID
                : ChanViewHelper.ViewType.LIST;
    }
}
