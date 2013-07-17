package com.chanapps.four.viewer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/10/13
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridViewer {

    private static String TAG = BoardGridViewer.class.getSimpleName();
    private static boolean DEBUG = true;

    private static ImageLoader imageLoader;
    private static DisplayImageOptions displayImageOptions;

    private static void initStatics(View view) {
        imageLoader = ChanImageLoader.getInstance(view.getContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.NONE)
                .cacheOnDisc()
                //.cacheInMemory()
                //.resetViewBeforeLoading()
                .showStubImage(R.drawable.stub_image_background)
                .build();
    }

    public static boolean setViewValue(View view, Cursor cursor, String groupBoardCode,
                                       int columnWidth, int columnHeight,
                                       View.OnClickListener overlayListener,
                                       View.OnClickListener overflowListener)
    {
        if (imageLoader == null)
            initStatics(view);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        switch (view.getId()) {
            case R.id.grid_item:
                return setItem(view, cursor, groupBoardCode, overlayListener, overflowListener);
            case R.id.grid_item_thread_subject:
                return setGridSubject((TextView) view, cursor);
            case R.id.grid_item_thread_info:
                return setInfo((TextView) view, cursor, groupBoardCode, flags);
            case R.id.grid_item_country_flag:
                return setCountryFlag((ImageView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setGridThumb((ImageView) view, cursor, flags, columnWidth, columnHeight);
        }
        return false;
    }

    protected static boolean setItem(View item, Cursor cursor, String groupBoardCode,
                                     View.OnClickListener overlayListener,
                                     View.OnClickListener overflowListener) {
        View overflow = item.findViewById(R.id.grid_item_overflow_icon);
        if (overflow != null) {
            if (overflowListener != null) {
                overflow.setOnClickListener(overflowListener);
                overflow.setVisibility(View.VISIBLE);
            }
            else {
                overflow.setVisibility(View.GONE);
            }
        }
        ViewGroup overlay = (ViewGroup)item.findViewById(R.id.grid_item_overlay);
        if (overlay != null)
            overlay.setOnClickListener(overlayListener);
        if (DEBUG) Log.i(TAG, "setitemValue item=" + item + " visible=" + (item.getVisibility() == View.VISIBLE)
                + " size=" + item.getMeasuredWidth() + "x" + item.getMeasuredHeight());
        return true;
    }
    
    protected static String getBoardAbbrev(Context context, Cursor cursor, String groupBoardCode) {
        String threadAbbrev = "";
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (boardCode != null && !boardCode.isEmpty() && !boardCode.equals(groupBoardCode)) {
            ChanBoard board = ChanBoard.getBoardByCode(context, boardCode);
            if (board != null)
                threadAbbrev += board.name;
            else
                threadAbbrev += "/" + boardCode + "/";
        }
        return threadAbbrev;
    }

    protected static boolean setGridSubject(TextView tv, Cursor cursor) {
        String s = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if (s != null && !s.isEmpty()) {
            if (DEBUG) Log.i(TAG, "setGridSubject tv=" + tv + " text=" + s);
            tv.setText(Html.fromHtml(s));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

    protected static boolean setText(TextView tv, Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        if (text != null && !text.isEmpty()) {
            tv.setText(Html.fromHtml(text));
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setGridThumb(ImageView iv, Cursor cursor, int flags, int columnWidth, int columnHeight) {
        String url;
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            url = "";
        }
        else if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
            url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL))
                    .split(ChanThread.AD_DELIMITER)[0];
        }
        else {
            url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL));
            String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            int i = (new Long(threadNo % 3)).intValue();
            String defaultUrl = ChanBoard.getIndexedImageDrawableUrl(boardCode, i);
            iv.setTag(R.id.BOARD_GRID_VIEW_DEFAULT_DRAWABLE, defaultUrl);
            if (url == null || url.isEmpty())
                url = ChanBoard.getIndexedImageDrawableUrl(boardCode, i);
        }
        if (url != null && !url.isEmpty()) {
            ViewParent parent = iv.getParent();
            if (parent != null && parent instanceof View) {
                View v = (View)parent;
                ViewGroup.LayoutParams params = v.getLayoutParams();
                if (columnWidth > 0 && columnHeight > 0 && params != null) {
                    params.width = columnWidth;
                    params.height = columnWidth; // force square
                }
            }
            ViewGroup.LayoutParams params = iv.getLayoutParams();
            if (columnWidth > 0 && columnHeight > 0 && params != null) {
                params.width = columnWidth;
                params.height = columnWidth; // force square
            }
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(url, iv, displayImageOptions, thumbLoadingListener);
        }
        else {
            iv.setVisibility(View.VISIBLE);
        }
        return true;
    }

    protected static ImageLoadingListener thumbLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }
        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            if (DEBUG) Log.e(TAG, "Loading failed uri=" + imageUri + " reason=" + failReason.getType());
            //displayDefaultItem(imageUri, view);
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        }
        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            if (DEBUG) Log.e(TAG, "Loading cancelled uri=" + imageUri);
        }
    };

    /*
    protected static void displayDefaultItem(String imageUri, View view) {
        if (imageUri.matches("drawable://.*")) {
            displayItem(imageUri, view, null);
            return;
        }
        String defaultUrl = (String)view.getTag(R.id.BOARD_GRID_VIEW_DEFAULT_DRAWABLE);
        if (defaultUrl == null || defaultUrl.isEmpty())
            defaultUrl = ChanBoard.getIndexedImageDrawableUrl(ChanBoard.DEFAULT_BOARD_CODE, 0);
        imageLoader.displayImage(defaultUrl, (ImageView)view, displayImageOptions, thumbLoadingListener);
    }
    */

    protected static boolean setCountryFlag(ImageView iv, Cursor cursor) {
        String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL));
        if (url != null && !url.isEmpty()) {
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(url, iv, displayImageOptions);
        }
        else {
            iv.setVisibility(View.GONE);
            iv.setImageDrawable(null);
        }
        return true;
    }

    protected static boolean setInfo(TextView tv, Cursor cursor, String groupBoardCode, int flags) {
        String s = (flags & ChanThread.THREAD_FLAG_BOARD) == 0
                ? getBoardAbbrev(tv.getContext(), cursor, groupBoardCode)
                : "";
        String t = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_HEADLINE));
        if (t == null)
            t = "";
        String u = s + (s.isEmpty() || t.isEmpty() ? "" : "<br/>") + t;
        if ((flags & (ChanThread.THREAD_FLAG_AD | ChanThread.THREAD_FLAG_TITLE)) == 0 && !u.isEmpty()) {
            tv.setText(Html.fromHtml(u));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

}