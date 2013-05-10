package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ThreadImageOnClickListener;
import com.chanapps.four.data.ChanAd;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/10/13
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadViewer {

    public static final int ITEM_THUMB_WIDTH_DP = 96;
    public static final int ITEM_THUMB_MAXHEIGHT_DP = ITEM_THUMB_WIDTH_DP;
    public static final int ITEM_THUMB_EMPTY_DP = 8;

    public static boolean setViewValue(final View view, final Cursor cursor, final int columnIndex,
                                       final ImageLoader imageLoader, final DisplayImageOptions options,
                                       String groupBoardCode, Typeface subjectTypeface, int padding4DP) {
        /*
        if (DEBUG) Log.v(TAG, "setViewValue for  position=" + cursor.getPosition());
        if (DEBUG) Log.v(TAG, "                 boardCode=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE)));
        if (DEBUG) Log.v(TAG, "                    postId=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_ID)));
        if (DEBUG) Log.v(TAG, "                      text=" + cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT)));
        */

        if (cursor.getColumnIndex(ChanPost.POST_FLAGS) == -1) { // we are on board list
            //if (view.getId() == R.id.grid_item_thread_text)
            //    return setEmptyItemText((TextView) view);
            //else
            return BoardViewer.setViewValue(view, cursor, columnIndex, imageLoader, options,
                    groupBoardCode, ViewType.AS_GRID, subjectTypeface, padding4DP);
        }
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        switch (view.getId()) {
            case R.id.list_item:
                return setItem((ViewGroup) view, cursor, flags);
            case R.id.list_item_header_wrapper:
                return setItemHeaderWrapper((ViewGroup) view, flags);
            case R.id.list_item_image_expanded:
                return setItemImageExpanded((ImageView) view, cursor, flags);
            case R.id.list_item_expanded_progress_bar:
                return setItemImageExpandedProgressBar((ProgressBar) view);
            case R.id.list_item_image_wrapper:
                return setItemImageWrapper((ViewGroup) view, cursor, flags);
            case R.id.list_item_image:
                return setItemImage((ImageView) view, cursor, flags, imageLoader, options);
            case R.id.list_item_country_flag:
                return setItemCountryFlag((ImageView) view, cursor, flags, imageLoader, options);
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

    /*
        protected boolean setEmptyItemText(TextView view) {
        view.setText("");
        view.setVisibility(View.GONE);
        return true;
    }
     */

    static protected boolean setItem(ViewGroup item, Cursor cursor, int flags) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        item.setTag((flags | ChanPost.FLAG_IS_AD) > 0 ? null : postId);
        item.setTag(R.id.THREAD_VIEW_IS_EXPANDED, new Boolean(false));
        ViewGroup itemHeaderWrapper = (ViewGroup) item.findViewById(R.id.list_item_header_wrapper);
        ViewGroup.LayoutParams params = itemHeaderWrapper.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
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

    static protected boolean setItemHeaderWrapper(ViewGroup wrapper, int flags) {
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_ad_bg_gradient);
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_button_gradient_bg);
        else
            wrapper.setBackgroundResource(0);
        return true;
    }


    static private boolean setItemHeaderValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        return true;
    }

    static private boolean setItemSubject(final TextView tv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_HAS_SUBJECT) == 0
                || (flags & (ChanPost.FLAG_IS_AD | ChanPost.FLAG_IS_TITLE)) > 0) {
            tv.setText("");
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        //if (cursor.getPosition() == 0) {
        //    ensureSubjectTypeface();
        //    tv.setTypeface(subjectTypeface);
        //} else {
        //    tv.setTypeface(Typeface.DEFAULT);
        //}
        tv.setText(spanned);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    static private boolean setItemTitle(final TextView tv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_IS_TITLE) == 0) {
            tv.setText("");
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    static private boolean setItemText(final TextView tv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_IS_AD) == 0 && (flags & ChanPost.FLAG_HAS_TEXT) > 0) {
            String postText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
            tv.setText(Html.fromHtml(postText));
            /*
            if (cursor.getPosition() != 0 && (flags & ChanPost.FLAG_HAS_IMAGE) > 0) // has image
                tv.setPadding(0, padding8DP, 0, padding8DP);
            else
                tv.setPadding(0, 0, 0, padding8DP);
            */
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    static private boolean setItemImageExifValue(final TextView tv) {
        tv.setText("");
        tv.setVisibility(View.GONE);
        return true;
    }

    static private boolean setItemImageWrapper(final ViewGroup layout, final Cursor cursor, int flags) {
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        DisplayMetrics displayMetrics = layout.getResources().getDisplayMetrics();
        int widthDp = (flags & ChanPost.FLAG_HAS_IMAGE) > 0 ? ITEM_THUMB_WIDTH_DP : ITEM_THUMB_EMPTY_DP;
        params.width = ChanGridSizer.dpToPx(displayMetrics, widthDp);
        return true;
    }

    static private ImageLoadingListener adImageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            if (view instanceof ImageView) {
                ChanImageLoader.getInstance(view.getContext()).displayImage(ChanAd.defaultImageUrl(), (ImageView) view);
            }
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
        }
    };

    static private boolean setItemImage(final ImageView iv, final Cursor cursor, int flags,
                                        ImageLoader imageLoader, DisplayImageOptions options) {
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
        DisplayMetrics displayMetrics = iv.getResources().getDisplayMetrics();
        if (tn_w == 0 || tn_h == 0) { // we don't have height and width, so just show unscaled image
            params.width = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
            params.height = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageLoader.displayImage(imageUrl, iv, options);
            return true;
        }

        // scale image
        boolean isFirst = cursor.getPosition() == 0;
        double scaleFactor = (double) tn_w / (double) tn_h;
        if (scaleFactor < 0.5 || (isFirst && scaleFactor < 1)) { // tall image, restrict by height
            //params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int desiredHeight = isFirst
                    ? iv.getResources().getDisplayMetrics().heightPixels / 2
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
            params.width = (int) (scaleFactor * (double) desiredHeight);
            params.height = desiredHeight;
        } else {
            int desiredWidth = isFirst
                    ? displayMetrics.widthPixels
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
            params.width = desiredWidth; // restrict by width normally
            params.height = (int) ((double) desiredWidth / scaleFactor);
            //params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        //if (DEBUG) Log.i(TAG, "Input size=" + tn_w + "x" + tn_h + " output size=" + params.width + "x" + params.height);
        iv.setLayoutParams(params);
        //iv.setScaleType(ImageView.ScaleType.FIT_XY);
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            imageLoader.displayImage(imageUrl, iv, options, adImageLoadingListener);
        else
            imageLoader.displayImage(imageUrl, iv, options);

        return true;
    }

    static private boolean setItemImageExpanded(final ImageView iv, final Cursor cursor, int flags) {
        ChanHelper.clearBigImageView(iv);
        iv.setVisibility(View.GONE);
        if ((flags & (ChanPost.FLAG_IS_AD
                | ChanPost.FLAG_IS_TITLE
                | ChanPost.FLAG_IS_THREADLINK
                | ChanPost.FLAG_IS_BOARDLINK)) == 0)
            iv.setOnClickListener(new ThreadImageOnClickListener(cursor));
        return true;
    }

    static private boolean setItemImageExpandedProgressBar(final ProgressBar progressBar) {
        progressBar.setVisibility(View.GONE);
        return true;
    }

    static private boolean setItemCountryFlag(final ImageView iv, final Cursor cursor, int flags,
                                              ImageLoader imageLoader, DisplayImageOptions options) {
        if ((flags & ChanPost.FLAG_HAS_COUNTRY) > 0) {
            iv.setImageBitmap(null);
            iv.setVisibility(View.VISIBLE);
            String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_COUNTRY_URL));
            imageLoader.displayImage(countryFlagImageUrl, iv, options);
        } else {
            iv.setImageBitmap(null);
            iv.setVisibility(View.GONE);
        }
        return true;
    }

}
