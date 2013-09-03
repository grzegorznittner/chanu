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
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.ChanBoard;
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

    public static final int SMALL_GRID = 0x01;

    private static String TAG = BoardGridViewer.class.getSimpleName();
    private static boolean DEBUG = false;

    private static ImageLoader imageLoader;
    private static DisplayImageOptions displayImageOptions;

    public static void initStatics(Context context, boolean isDark) {
        imageLoader = ChanImageLoader.getInstance(context);
        int stub = isDark
                ? R.drawable.stub_image_background_dark
                : R.drawable.stub_image_background;
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.NONE)
                .cacheOnDisc()
                //.cacheInMemory()
                .resetViewBeforeLoading()
                .showStubImage(stub)
                .build();
    }

    public static boolean setViewValue(View view, Cursor cursor, String groupBoardCode,
                                       int columnWidth, int columnHeight,
                                       View.OnClickListener overlayListener,
                                       View.OnClickListener overflowListener,
                                       int options)
    {
        if (imageLoader == null)
            throw new IllegalStateException("Must call initStatics() before calling setViewValue()");
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        BoardGridViewHolder viewHolder = (BoardGridViewHolder)view.getTag(R.id.VIEW_HOLDER);
        setItem(viewHolder, overlayListener, overflowListener);
        setGridSubject(viewHolder, cursor);
        setInfo(viewHolder, cursor, groupBoardCode, flags);
        setCountryFlag(viewHolder, cursor);
        setImage(viewHolder, cursor, flags, columnWidth, columnHeight, options);
        return true;
    }

    protected static boolean setItem(BoardGridViewHolder viewHolder,
                                     View.OnClickListener overlayListener,
                                     View.OnClickListener overflowListener) {
        View overflow = viewHolder.grid_item_overflow_icon;
        if (overflow != null) {
            if (overflowListener != null) {
                overflow.setOnClickListener(overflowListener);
                overflow.setVisibility(View.VISIBLE);
            }
            else {
                overflow.setVisibility(View.GONE);
            }
        }
        ViewGroup overlay = viewHolder.grid_item;
        if (overlay != null) {
            if (overlayListener != null) {
                overlay.setOnClickListener(overlayListener);
                overlay.setClickable(true);
            }
            else {
                overlay.setClickable(false);
            }
        }
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

    protected static boolean setGridSubject(BoardGridViewHolder viewHolder, Cursor cursor) {
        TextView tv = viewHolder.grid_item_thread_subject;
        if (tv == null)
            return false;
        String s = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        String t = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        String u = (s != null && !s.isEmpty() ? "<b>" + s + "</b><br/>" : "")
                + t;
        if (DEBUG) Log.i(TAG, "setGridSubject tv=" + tv + " u=" + u);
        if (u != null && !u.isEmpty()) {
            tv.setText(Html.fromHtml(u));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }
    /*
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
    */
    protected static boolean setImage(BoardGridViewHolder viewHolder, Cursor cursor, int flags,
                                      int columnWidth, int columnHeight, int options) {
        ImageView iv = viewHolder.grid_item_thread_thumb;
        if (iv == null)
            return false;
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
        iv.setVisibility(View.VISIBLE);
        if (url != null && !url.isEmpty()) {
            if ((options & SMALL_GRID) > 0) {
                ViewParent parent = iv.getParent();
                if (parent != null && parent instanceof View) {
                    View v = (View)parent;
                    ViewGroup.LayoutParams params = v.getLayoutParams();
                    if (columnWidth > 0 && params != null) {
                        params.width = columnWidth; // force square
                        params.height = columnWidth; // force square
                    }
                }
                ViewGroup.LayoutParams params = iv.getLayoutParams();
                if (columnWidth > 0 && params != null) {
                    params.width = columnWidth; // force square
                    params.height = columnWidth; // force square
                }
            }
            imageLoader.displayImage(url, iv, displayImageOptions, thumbLoadingListener);
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

    protected static boolean setCountryFlag(BoardGridViewHolder viewHolder, Cursor cursor) {
        ImageView iv = viewHolder.grid_item_country_flag;
        if (iv == null)
            return false;
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

    protected static boolean setInfo(BoardGridViewHolder viewHolder, Cursor cursor, String groupBoardCode, int flags) {
        TextView tv = viewHolder.grid_item_thread_info;
        if (tv == null)
            return false;
        String s = (flags & ChanThread.THREAD_FLAG_BOARD) == 0
                ? getBoardAbbrev(tv.getContext(), cursor, groupBoardCode)
                : "";
        String t = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_HEADLINE));
        if (t == null)
            t = "";
        //String u = s + (s.isEmpty() || t.isEmpty() ? "" : "<br/>") + t;
        String u = s + (s.isEmpty() || t.isEmpty() ? "" : " ") + t;
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