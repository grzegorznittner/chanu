package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/10/13
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class BoardViewer {

    public static boolean setViewValue(View view,
                                       Cursor cursor,
                                       int columnIndex,
                                       ImageLoader imageLoader,
                                       DisplayImageOptions options,
                                       String groupBoardCode,
                                       ViewType viewType,
                                       Typeface subjectTypeface,
                                       int padding4DP)
    {
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) { // special case it to avoid needing a separate item layout
            return setTitleView(view, cursor, flags, viewType);
        }
        else if ((flags & ChanThread.THREAD_FLAG_AD) > 0 && viewType == ViewType.AS_LIST) {
            return setBannerAdView(view, cursor, imageLoader, options, flags, viewType);
        }
        view.setVisibility(View.VISIBLE);
        switch (view.getId()) {
            case R.id.list_item:
                return setItem(view);
            case R.id.grid_item_board_abbrev:
                return setBoardAbbrev((TextView) view, cursor, groupBoardCode, flags);
            case R.id.grid_item_thread_title:
                return setTitle((TextView) view, cursor, flags, viewType);
            case R.id.grid_item_thread_subject:
                return setSubject((TextView) view, cursor, viewType, subjectTypeface);
            case R.id.grid_item_thread_headline:
                return setHeadline((TextView) view, cursor, padding4DP);
            case R.id.grid_item_thread_text:
                return setText((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setThumb((ImageView) view, cursor, imageLoader, options, flags, viewType);
            case R.id.grid_item_country_flag:
                return setCountryFlag((ImageView) view, cursor, imageLoader, options);
            case R.id.grid_item_num_replies:
                return setNumReplies((TextView) view, cursor, flags);
            case R.id.grid_item_num_images:
                return setNumImages((TextView) view, cursor, flags);
            case R.id.grid_item_thread_banner_ad:
                return setBannerAd((ImageView) view, cursor, imageLoader, options, flags, viewType);
        }
        return false;
    }

    protected static boolean setItem(View view) {
        View v = view.findViewById(R.id.grid_item_thread_image_wrapper);
        if (v != null)
            v.setVisibility(View.VISIBLE);
        return true;
    }

    protected static boolean setBoardAbbrev(TextView tv, Cursor cursor, String groupBoardCode, int flags) {
        String threadAbbrev = "";
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (boardCode != null && !boardCode.isEmpty() && !boardCode.equals(groupBoardCode))
            threadAbbrev += "/" + boardCode + "/";
        if ((flags & ChanThread.THREAD_FLAG_DEAD) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.dead_thread_abbrev);
        if ((flags & ChanThread.THREAD_FLAG_CLOSED) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.closed_thread_abbrev);
        if ((flags & ChanThread.THREAD_FLAG_STICKY) > 0)
            threadAbbrev += (threadAbbrev.isEmpty()?"":" ") + tv.getContext().getString(R.string.sticky_thread_abbrev);
        tv.setText(threadAbbrev);
        if (!threadAbbrev.isEmpty())
            tv.setVisibility(View.VISIBLE);
        else
            tv.setVisibility(View.GONE);
        return true;
    }

    protected static boolean setTitle(TextView tv, Cursor cursor, int flags, ViewType viewType) {
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
            if (viewType == ViewType.AS_LIST) {
                text = text.toUpperCase();
            }
            tv.setText(Html.fromHtml(text));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setSubject(TextView tv, Cursor cursor, ViewType viewType, Typeface subjectTypeface) {
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if (text != null && !text.isEmpty()) {
            tv.setText(Html.fromHtml(text));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setHeadline(TextView tv, Cursor cursor, int padding4DP) {
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if (text != null && !text.isEmpty()) {
            tv.setPadding(tv.getPaddingLeft(), 0, tv.getPaddingRight(), tv.getPaddingBottom());
        }
        else {
            tv.setPadding(tv.getPaddingLeft(), padding4DP, tv.getPaddingRight(), tv.getPaddingBottom());
        }
        tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_HEADLINE))));
        return true;
    }

    protected static boolean setText(TextView tv, Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        if (text != null && !text.isEmpty()) {
            tv.setText(Html.fromHtml(text));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setThumb(ImageView iv, Cursor cursor, ImageLoader imageLoader,
                                            DisplayImageOptions options, int flags, ViewType viewType) {
        if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            iv.setImageBitmap(null);
        }
        else if ((flags & ChanThread.THREAD_FLAG_AD) > 0 && viewType == ViewType.AS_LIST) { // hide thumbnail
            iv.setImageBitmap(null);
        }
        else if ((flags & ChanThread.THREAD_FLAG_AD) > 0 && viewType == ViewType.AS_GRID) {
            String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL))
                    .split(ChanThread.AD_DELIMITER)[0];
            imageLoader.displayImage(url, iv, options);
        }
        else {
            String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL));
            if (url == null || url.isEmpty()) {
                String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                int i = (new Long(threadNo % 3)).intValue();
                url = ChanBoard.getIndexedImageDrawableUrl(boardCode, i);
            }
            imageLoader.displayImage(url, iv, options);
        }
        return true;
    }

    protected static boolean setCountryFlag(ImageView iv, Cursor cursor, ImageLoader imageLoader, DisplayImageOptions options) {
        String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL));
        if (url != null && !url.isEmpty()) {
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(url, iv, options);
        }
        else {
            iv.setVisibility(View.GONE);
            iv.setImageResource(0);
        }
        return true;
    }

    protected static boolean setNumReplies(TextView tv, Cursor cursor, int flags) {
        int n = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_REPLIES));
        if ((flags & (ChanThread.THREAD_FLAG_AD
                | ChanThread.THREAD_FLAG_BOARD
                | ChanThread.THREAD_FLAG_TITLE)) == 0
                && n >= 0)
        {
            tv.setText(n + "r");
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setNumImages(TextView tv, Cursor cursor, int flags) {
        int n = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_IMAGES));
        if ((flags & (ChanThread.THREAD_FLAG_AD
                | ChanThread.THREAD_FLAG_BOARD
                | ChanThread.THREAD_FLAG_TITLE)) == 0
                && n >= 0)
        {
            tv.setText(n + "i");
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setTitleView(View view, Cursor cursor, int flags, ViewType viewType) {
        if (view.getId() == R.id.grid_item_thread_title) {
            setTitle((TextView) view, cursor, flags, viewType);
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.grid_item) {
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.list_item) {
            view.setVisibility(View.VISIBLE);
            View v = view.findViewById(R.id.grid_item_thread_image_wrapper);
            if (v != null)
                v.setVisibility(View.GONE);
        }
        else {
            view.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setBannerAdView(View view, Cursor cursor, ImageLoader imageLoader,
                                             DisplayImageOptions options, int flags, ViewType viewType) {
        if (view.getId() == R.id.grid_item_thread_banner_ad) {
            setBannerAd((ImageView) view, cursor, imageLoader, options, flags, viewType);
        }
        else if (view.getId() == R.id.list_item) {
            view.setVisibility(View.VISIBLE);
            View v = view.findViewById(R.id.grid_item_thread_image_wrapper);
            if (v != null)
                v.setVisibility(View.GONE);
        }
        else {
            view.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setBannerAd(final ImageView iv, Cursor cursor, ImageLoader imageLoader,
                                         DisplayImageOptions options, int flags, ViewType viewType) {
        iv.setImageBitmap(null);
        iv.setVisibility(View.GONE);
        if ((flags & ChanThread.THREAD_FLAG_AD) > 0 && viewType == ViewType.AS_LIST) {
            String url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL))
                    .split(ChanThread.AD_DELIMITER)[1];
            imageLoader.displayImage(url, iv, options, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    iv.setVisibility(View.VISIBLE);
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
        }
        return true;
    }

}