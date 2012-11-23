package com.chanapps.four.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanLoadService;
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

    private static final String TAG = ChanViewHelper.class.getSimpleName();

    private Activity activity;
    private DisplayImageOptions options;
    private ImageLoader imageLoader;
    private String boardCode;
    private long threadNo = 0;
    private boolean hideAllText = false;

    public enum ViewType {
        LIST,
        GRID
    }

    public ChanViewHelper(Activity activity, ViewType viewType) {
        this.activity = activity;
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
        saveViewType(viewType);
    }

    public boolean setGridViewValue(View view, Cursor cursor, int columnIndex) {
        return setViewValue(view, cursor, columnIndex, ViewType.GRID);
    }

    public boolean setListViewValue(View view, Cursor cursor, int columnIndex) {
        return setViewValue(view, cursor, columnIndex, ViewType.LIST);
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex, ViewType viewType) {
        String rawText = cursor.getString(cursor.getColumnIndex("text"));
        String rawImageUrl = cursor.getString(cursor.getColumnIndex("image_url"));
        String text = rawText == null ? "" : rawText;
        String imageUrl = rawImageUrl == null ? "" : rawImageUrl;
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
            Log.e(TAG, "setting text: " + text);
            switch (viewType) {
                case GRID:
                    if (hideAllText || text == null || text.isEmpty()) {
                        tv.setVisibility(View.INVISIBLE);
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
            // making this invisible causes display problems
            //if (imageUrl != null && !imageUrl.isEmpty()) {
                setViewImage(iv, imageUrl, cursor);
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
        int tn_w = cursor.getInt(cursor.getColumnIndex("tn_w"));
        int tn_h = cursor.getInt(cursor.getColumnIndex("tn_h"));
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
        text = text.substring(0, Math.min(text.length(), 22));
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

    private enum ServiceType {
        BOARD,
        THREAD
    }

    public void startBoardService() {
        startService(ServiceType.BOARD);
    }

    public void startThreadService() {
        startService(ServiceType.THREAD);
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
        if (intent == null || !intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(boardCode)) {
            boardCode = oldBoardCode;
            Log.i(TAG, "Board code loaded from prefs: " + boardCode);
        }
        if (!oldBoardCode.equals(boardCode)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.commit();
        }
    }

    private void loadThreadNo(SharedPreferences prefs) {
        long oldThreadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
        threadNo = 0;
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            Log.i(TAG, "Thread no read from intent: " + boardCode);
        }
        if (intent == null || !intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = oldThreadNo;
            Log.i(TAG, "Thread no loaded from prefs: " + threadNo);
        }
        if (oldThreadNo != threadNo) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.putLong(ChanHelper.THREAD_NO, threadNo);
            ed.commit();
        }
    }

    public ViewType getViewType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return ChanViewHelper.ViewType.valueOf(prefs.getString(ChanHelper.VIEW_TYPE, ViewType.LIST.toString()));
    }

    public void saveViewType(ViewType viewType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        ViewType oldViewType = ChanViewHelper.ViewType.valueOf(prefs.getString(ChanHelper.VIEW_TYPE, ViewType.LIST.toString()));
        if (viewType != oldViewType) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.VIEW_TYPE, viewType.toString());
            ed.commit();
        }
    }

    private void startService(ServiceType serviceType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        reloadPrefs(prefs);
        loadBoardCode(prefs);
        if (serviceType == ServiceType.THREAD) {
            loadThreadNo(prefs);
        }
        setBoardMenu();
        Log.i(TAG, "Starting ChanLoadService");
        Intent threadIntent = new Intent(activity, ChanLoadService.class);
        threadIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        threadIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        activity.startService(threadIntent);
    }

    public void startBoardActivity(AdapterView<?> adapterView, View view, int position, long id) {
        startBoardActivityWithType(adapterView, view, position, id, getViewType());
    }

    private void startBoardActivityWithType(AdapterView<?> adapterView, View view, int position, long id, ViewType viewType) {
        Intent intent = new Intent(activity, viewType == ViewType.LIST ? BoardListActivity.class : BoardGridActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        activity.startActivity(intent);
    }
    public void startThreadActivity(AdapterView<?> adapterView, View view, int position, long id) {
        startThreadActivityWithType(adapterView, view, position, id, getViewType());
    }

    private void startThreadActivityWithType(AdapterView<?> adapterView, View view, int position, long id, ViewType viewType) {
        Intent intent = new Intent(activity, viewType == ViewType.LIST ? ThreadListActivity.class : ThreadGridActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, id);
        activity.startActivity(intent);
    }

    public void startFullImageActivityFromList(AdapterView<?> adapterView, View view, int position, long id) {
        startFullImageActivity(adapterView, view, position, id, ViewType.LIST);
    }

    public void startFullImageActivityFromGrid(AdapterView<?> adapterView, View view, int position, long id) {
        startFullImageActivity(adapterView, view, position, id, ViewType.GRID);
    }

    private void startFullImageActivity(AdapterView<?> adapterView, View view, int position, long id, ViewType viewType) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex("_id"));
        final int w = cursor.getInt(cursor.getColumnIndex("w"));
        final int h = cursor.getInt(cursor.getColumnIndex("h"));
        Intent intent = new Intent(activity, FullScreenImageActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postId);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, h);
        intent.putExtra(ChanHelper.VIEW_TYPE, viewType.toString());
        activity.startActivity(intent);
    }

    public String getBoardCode() {
        return boardCode;
    }

    public long getThreadNo() {
        return threadNo;
    }

}
