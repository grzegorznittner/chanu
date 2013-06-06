package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Debug;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.GenericDialogFragment;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
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
                .cacheInMemory()
                .resetViewBeforeLoading()
                .build();
    }

    public static boolean setViewValue(View view, Cursor cursor, String groupBoardCode)
    {
        if (imageLoader == null)
            initStatics(view);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) { // special case it to avoid needing a separate item layout
            return setTitleView(view, cursor, flags);
        }
        if ((flags & ChanThread.THREAD_FLAG_BUTTON) > 0) { // special case it to avoid needing a separate item layout
            return setButtonView(view, cursor, flags);
        }
        switch (view.getId()) {
            case R.id.grid_item_board_abbrev:
                return setBoardAbbrev((TextView) view, cursor, groupBoardCode, flags);
            case R.id.grid_item_thread_title:
                return setTitle((TextView) view, cursor, flags);
            case R.id.grid_item_thread_button:
                return setButton((TextView) view, cursor, flags);
            case R.id.grid_item_text_wrapper:
                return setGone(view);
            case R.id.grid_item_thread_subject:
                return setGridSubject((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setGridThumb((ImageView) view, cursor, flags);
            case R.id.grid_item_country_flag:
                return setCountryFlag((ImageView) view, cursor);
            case R.id.grid_item_thread_info:
                return setInfo((TextView) view, cursor, flags);
        }
        return false;
    }

    static boolean setBoardAbbrev(TextView tv, Cursor cursor, String groupBoardCode, int flags) {
        tv.setVisibility(View.GONE);
        String threadAbbrev = "";
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (boardCode != null && !boardCode.isEmpty() && !boardCode.equals(groupBoardCode)) {
            ChanBoard board = ChanBoard.getBoardByCode(tv.getContext(), boardCode);
            if (board != null)
                threadAbbrev += board.name;
            else
                threadAbbrev += "/" + boardCode + "/";
        }
        if ((flags & ChanThread.THREAD_FLAG_DEAD) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.thread_is_dead);
        if ((flags & ChanThread.THREAD_FLAG_CLOSED) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.thread_is_closed);
        if ((flags & ChanThread.THREAD_FLAG_STICKY) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.thread_is_sticky);
        tv.setText(threadAbbrev);
        return true;
    }

    protected static boolean setTitle(TextView tv, Cursor cursor, int flags) {
        tv.setVisibility(View.GONE);
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
            tv.setText(Html.fromHtml(text));
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setButton(TextView tv, Cursor cursor, int flags) {
        tv.setVisibility(View.GONE);
        if ((flags & ChanThread.THREAD_FLAG_BUTTON) > 0) {
            String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
            tv.setText(Html.fromHtml(text));
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setGridSubject(TextView tv, Cursor cursor) {
        tv.setVisibility(View.GONE);
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if (text != null && !text.isEmpty())
            tv.setText(Html.fromHtml(text));
        else
            tv.setText("");
        return true;
    }

    protected static boolean setText(TextView tv, Cursor cursor) {
        tv.setVisibility(View.GONE);
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        if (text != null && !text.isEmpty()) {
            tv.setText(Html.fromHtml(text));
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setGridThumb(ImageView iv, Cursor cursor, int flags) {
        iv.setImageDrawable(null);
        iv.setVisibility(View.VISIBLE);
        String url;
        if ((flags & (ChanThread.THREAD_FLAG_TITLE | ChanThread.THREAD_FLAG_AD)) > 0) {
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
        imageLoader.displayImage(url, iv, displayImageOptions, thumbLoadingListener);
        return true;
    }

    protected static ImageLoadingListener thumbLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }
        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            if (DEBUG) Log.e(TAG, "Loading failed uri=" + imageUri + " reason=" + failReason.getType());
            displayDefaultItem(imageUri, view);
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            displayItem(imageUri, view, loadedImage);
        }
        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            if (DEBUG) Log.e(TAG, "Loading cancelled uri=" + imageUri);
        }
    };

    protected static void displayItem(String imageUri, View view, Bitmap loadedImage) {
        ViewParent parent = view.getParent();
        if (parent == null || !(parent instanceof  ViewGroup))
            return;

        ViewGroup parentView = (ViewGroup)parent;
        ViewGroup wrapper = (ViewGroup)parentView.findViewById(R.id.grid_item_text_wrapper);
        TextView subject = (TextView)parentView.findViewById(R.id.grid_item_thread_subject);
        TextView info = (TextView)parentView.findViewById(R.id.grid_item_thread_info);
        TextView abbrev = (TextView)parentView.findViewById(R.id.grid_item_board_abbrev);
        ImageView countryFlag = (ImageView)parentView.findViewById(R.id.grid_item_country_flag);
        if (DEBUG) Log.i(TAG, "onLoadingComplete subject=" + subject.getText() + " info=" + info.getText()
                + " abbrev=" + abbrev.getText()
                + " url=" + imageUri
                + " img=" + loadedImage
                + " byteCount=" + (loadedImage == null ? 0 : loadedImage.getByteCount()));
        boolean oneVisible = false;

        boolean overlayDetails = true;
        if (overlayDetails) {
            if (subject != null && subject.getText() != null && subject.getText().length() > 0) {
                subject.setVisibility(View.VISIBLE);
                oneVisible = true;
            }
            if (info != null && info.getText() != null && info.getText().length() > 0) {
                info.setVisibility(View.VISIBLE);
                oneVisible = true;
            }
            if (wrapper != null && oneVisible)
                wrapper.setVisibility(View.VISIBLE);
            if (abbrev != null && abbrev.getText() != null
                    && abbrev.getText().length() > 0
                    && !abbrev.getText().toString().equals(subject.getText().toString()))
                abbrev.setVisibility(View.VISIBLE);
            if (countryFlag.getDrawable() != null)
                countryFlag.setVisibility(View.VISIBLE);
        }
    }

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

    protected static boolean setCountryFlag(ImageView iv, Cursor cursor) {
        iv.setVisibility(View.GONE);
        String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL));
        if (url != null && !url.isEmpty()) {
            imageLoader.displayImage(url, iv, displayImageOptions);
        }
        else {
            iv.setImageDrawable(null);
        }
        return true;
    }

    protected static boolean setInfo(TextView tv, Cursor cursor, int flags) {
        tv.setVisibility(View.GONE);
        int r = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_REPLIES));
        if ((flags & (ChanThread.THREAD_FLAG_AD
                | ChanThread.THREAD_FLAG_BOARD
                | ChanThread.THREAD_FLAG_TITLE)) == 0
                && r >= 0)
        {
            String s = tv.getResources().getQuantityString(R.plurals.thread_num_replies, r, r);
            if (r > 0) {
                int i = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_IMAGES));
                s += " " + tv.getResources().getQuantityString(R.plurals.thread_num_imgs, i, i);
            }
            tv.setText(s);
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setTitleView(View view, Cursor cursor, int flags) {
        if (view.getId() == R.id.grid_item_thread_title) {
            setTitle((TextView) view, cursor, flags);
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.grid_item) {
            view.setVisibility(View.VISIBLE);
        }
        else {
            view.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setButtonView(View view, Cursor cursor, int flags) {
        if (view.getId() == R.id.grid_item_thread_button) {
            setButton((TextView) view, cursor, flags);
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.grid_item) {
            view.setVisibility(View.VISIBLE);
        }
        else {
            view.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setGone(View view) {
        view.setVisibility(View.GONE);
        return true;
    }

}