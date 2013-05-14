package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.gallery3d.ui.Log;
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
    public static final int MAX_HEADER_SCALE = 3;

    private static final String TAG = ThreadViewer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static boolean setViewValue(final View view, final Cursor cursor,
                                       final ImageLoader imageLoader, final DisplayImageOptions options,
                                       String groupBoardCode, Typeface subjectTypeface, int padding4DP) {
        int flagIdx = cursor.getColumnIndex(ChanPost.POST_FLAGS);
        if (flagIdx == -1) { // we are on board list
            return BoardGridViewer.setViewValue(view, cursor, imageLoader, options, groupBoardCode);
        }
        int flags = cursor.getInt(flagIdx);
        if ((flags & ChanPost.FLAG_IS_TITLE) > 0) { // special case it to avoid needing a separate item layout
            return setTitleView(view, cursor, flags);
        }
        if ((flags & ChanPost.FLAG_IS_AD) > 0) {
            return setBannerAdView(view, cursor, imageLoader, options, padding4DP);
        }
        return setListItemView(view, cursor, imageLoader, options, subjectTypeface, flags);
    }

    public static boolean setListItemView(final View view, final Cursor cursor,
                                       final ImageLoader imageLoader, final DisplayImageOptions options,
                                       Typeface subjectTypeface, int flags) {
        switch (view.getId()) {
            case R.id.list_item:
                return setItem((ViewGroup) view, cursor, flags);
            case R.id.list_item_header_wrapper:
                return setHeaderWrapper((ViewGroup) view, flags);
            case R.id.list_item_image_expanded:
                return setImageExpanded((ImageView) view, cursor, flags);
            case R.id.list_item_image_expanded_click_effect:
                return setImageExpandedClickEffect(view, cursor, flags);
            case R.id.list_item_expanded_progress_bar:
                return setImageExpandedProgressBar((ProgressBar) view);
            case R.id.list_item_image_wrapper:
                return setImageWrapper((ViewGroup) view, cursor, flags);
            case R.id.list_item_image:
                return setImage((ImageView) view, cursor, flags, imageLoader, options);
            case R.id.list_item_country_flag:
                return setCountryFlag((ImageView) view, cursor, flags, imageLoader, options);
            case R.id.list_item_header:
                return setHeaderValue((TextView) view, cursor);
            case R.id.list_item_subject:
                return setSubject((TextView) view, cursor, subjectTypeface, flags);
            case R.id.list_item_title:
                return setTitle((TextView) view, cursor, flags);
            case R.id.list_item_text:
                return setText((TextView) view, cursor, flags);
            case R.id.list_item_image_exif:
                return setImageExifValue((TextView) view);
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
        item.setVisibility(View.VISIBLE);
        return true;
    }

    static protected boolean setHeaderWrapper(ViewGroup wrapper, int flags) {
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_ad_bg_gradient);
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            wrapper.setBackgroundResource(R.drawable.thread_button_gradient_bg);
        else
            wrapper.setBackgroundResource(0);
        wrapper.setVisibility(View.VISIBLE);
        return true;
    }


    static private boolean setHeaderValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    static private boolean setSubject(final TextView tv, final Cursor cursor, Typeface subjectTypeface, int flags) {
        if ((flags & (ChanPost.FLAG_IS_AD | ChanPost.FLAG_IS_TITLE)) > 0) {
            tv.setText("");
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = "";
        if ((flags & ChanPost.FLAG_HAS_SUBJECT) > 0)
            text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            if ((flags & ChanPost.FLAG_IS_CLOSED) > 0)
                text = tv.getResources().getString(R.string.thread_is_closed) + (text.isEmpty() ? "" : " ") + text;
            if ((flags & ChanPost.FLAG_IS_DEAD) > 0)
                text = tv.getResources().getString(R.string.thread_is_dead) + (text.isEmpty() ? "" : " ") + text;
        }
        if (text.length() > 0) {
            tv.setText(Html.fromHtml(text));
            if ((flags & ChanPost.FLAG_IS_HEADER) > 0)
                tv.setTypeface(subjectTypeface);
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    static private boolean setTitle(final TextView tv, final Cursor cursor, int flags) {
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

    static private boolean setText(final TextView tv, final Cursor cursor, int flags) {
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

    static private boolean setImageExifValue(final TextView tv) {
        tv.setText("");
        tv.setVisibility(View.GONE);
        return true;
    }

    static private boolean setImageWrapper(final ViewGroup layout, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            layout.setVisibility(View.VISIBLE);
        else
            layout.setVisibility(View.GONE);
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

    static private ImageLoadingListener bannerAdImageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            view.setVisibility(View.GONE);
            //if (view instanceof ImageView) {
            //    ChanImageLoader.getInstance(view.getContext()).displayImage(ChanAd.defaultImageUrl(), (ImageView) view);
            //}
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null)
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
        }
    };

    static private boolean setImage(final ImageView iv, final Cursor cursor, int flags,
                                        ImageLoader imageLoader, DisplayImageOptions options) {
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        if (params == null) { // something wrong in layout
            iv.setImageBitmap(null);
            iv.setVisibility(View.VISIBLE);
            return true;
        }
        if ((flags & ChanPost.FLAG_HAS_IMAGE) == 0) {
            iv.setImageBitmap(null);
            params.width = 0;
            params.height = 0;
            iv.setLayoutParams(params);
            iv.setVisibility(View.VISIBLE);
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
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(imageUrl, iv, options);
            return true;
        }

        // scale image
        boolean isHeader = (flags & ChanPost.FLAG_IS_HEADER) > 0;
        double scaleFactor = (double) tn_w / (double) tn_h;
        if (scaleFactor < 0.5 || (isHeader && scaleFactor < 1)) { // tall image, restrict by height
            //params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int maxHeaderHeight = Math.min(iv.getResources().getDisplayMetrics().heightPixels / 2, tn_h * MAX_HEADER_SCALE); // prevent excessive scaling
            int desiredHeight = isHeader
                    ? maxHeaderHeight
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
            params.width = (int) (scaleFactor * (double) desiredHeight);
            params.height = desiredHeight;
        } else {
            int maxHeaderWidth = Math.min(displayMetrics.widthPixels, tn_w * MAX_HEADER_SCALE); // prevent excessive scaling
            int desiredWidth = isHeader
                    ? maxHeaderWidth
                    : ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
            params.width = desiredWidth; // restrict by width normally
            params.height = (int) ((double) desiredWidth / scaleFactor);
            //params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        //if (DEBUG) Log.i(TAG, "Input size=" + tn_w + "x" + tn_h + " output size=" + params.width + "x" + params.height);
        iv.setLayoutParams(params);
        //iv.setScaleType(ImageView.ScaleType.FIT_XY);
        iv.setVisibility(View.VISIBLE);
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            imageLoader.displayImage(imageUrl, iv, options, adImageLoadingListener);
        else
            imageLoader.displayImage(imageUrl, iv, options);

        return true;
    }

    static private boolean setImageExpanded(final ImageView iv, final Cursor cursor, int flags) {
        ChanHelper.clearBigImageView(iv);
        iv.setVisibility(View.GONE);
        /*
        if ((flags & (ChanPost.FLAG_IS_AD
                | ChanPost.FLAG_IS_TITLE
                | ChanPost.FLAG_IS_THREADLINK
                | ChanPost.FLAG_IS_BOARDLINK)) == 0)
            iv.setOnClickListener(new ThreadImageOnClickListener(cursor));
        */
        return true;
    }

    static private boolean setImageExpandedClickEffect(final View view, final Cursor cursor, int flags) {
        view.setVisibility(View.GONE);
        if ((flags & (ChanPost.FLAG_IS_AD
                | ChanPost.FLAG_IS_TITLE
                | ChanPost.FLAG_IS_THREADLINK
                | ChanPost.FLAG_IS_BOARDLINK)) == 0)
            view.setOnClickListener(new ThreadImageOnClickListener(cursor));
        return true;
    }

    static private boolean setImageExpandedProgressBar(final ProgressBar progressBar) {
        progressBar.setVisibility(View.GONE);
        return true;
    }

    static private boolean setCountryFlag(final ImageView iv, final Cursor cursor, int flags,
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

    protected static boolean setTitleView(View view, Cursor cursor, int flags) {
        if (view.getId() == R.id.list_item_title) {
            setTitle((TextView) view, cursor, flags);
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.list_item) {
            view.setVisibility(View.VISIBLE);
        }
        else if (view.getId() == R.id.list_item_header_wrapper) {
            setHeaderWrapper((ViewGroup) view, flags);
        }
        else {
            view.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setBannerAdView(View view, Cursor cursor, ImageLoader imageLoader,
                                             DisplayImageOptions options, int padding4DP) {
        switch (view.getId()) {
            case R.id.list_item_thread_banner_ad:
                return setBannerAd((ImageView) view, cursor, imageLoader, options, padding4DP);
            case R.id.list_item_thread_banner_ad_click_effect:
                return setBannerAdLayoutParams(view, cursor, padding4DP);
            default:
                return true;
        }
    }

    protected static boolean setBannerAd(final ImageView iv, Cursor cursor, ImageLoader imageLoader,
                                         DisplayImageOptions options, int padding4DP) {
        setBannerAdLayoutParams(iv, cursor, padding4DP);
        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (DEBUG) Log.i(TAG, "Displaying ad image iv=" + iv + " url=" + url);
        imageLoader.displayImage(url, iv, options, bannerAdImageLoadingListener);
        return true;
    }

    protected static boolean setBannerAdLayoutParams(final View v, Cursor cursor, int padding4DP) {
        ViewParent parent = v.getParent();
        View parentView = parent == null ? null : (View)parent;
        if (parentView != null) {
            int measuredWidth = parentView.getMeasuredWidth();
            int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
            int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null)
                params.height = 12 * padding4DP; // 48dp to avoid big jumps, precalc would be better
        }
        else { // approximate
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null)
                params.height = 12 * padding4DP; // 48dp to avoid big jumps, precalc would be better
        }
        return true;
    }

}
