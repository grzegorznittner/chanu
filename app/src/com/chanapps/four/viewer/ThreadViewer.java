package com.chanapps.four.viewer;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final int DEFAULT_AD_HEIGHT_DP = 48;
    public static final int MAX_HEADER_SCALE = 2;
    public static final String SUBJECT_FONT = "fonts/Roboto-BoldCondensed.ttf";

    private static final String TAG = ThreadViewer.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static DisplayMetrics displayMetrics = null;
    private static Typeface subjectTypeface = null;
    private static int defaultAdHeight = 0;
    private static int itemThumbWidth = 0;
    private static int itemThumbMaxHeight = 0;
    private static ImageLoader imageLoader = null;
    private static DisplayImageOptions displayImageOptions = null;

    private static void initStatics(View view) {
        Resources res = view.getResources();
        displayMetrics = res.getDisplayMetrics();
        subjectTypeface = Typeface.createFromAsset(res.getAssets(), SUBJECT_FONT);
        defaultAdHeight = ChanGridSizer.dpToPx(displayMetrics, DEFAULT_AD_HEIGHT_DP);
        itemThumbWidth = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
        itemThumbMaxHeight = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
        imageLoader = ChanImageLoader.getInstance(view.getContext());
        displayImageOptions = createDisplayImageOptions(null);
    }

    private static DisplayImageOptions createDisplayImageOptions(ImageSize imageSize) {
        return new DisplayImageOptions.Builder()
                .cacheOnDisc()
                .cacheInMemory()
                .imageSize(imageSize)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .resetViewBeforeLoading()
                .build();
    }

    public static boolean setViewValue(final View view, final Cursor cursor, String groupBoardCode,
                                       View.OnClickListener imageOnClickListener,
                                       View.OnClickListener backlinkOnClickListener,
                                       View.OnClickListener repliesOnClickListener,
                                       View.OnClickListener sameIdOnClickListener,
                                       View.OnClickListener exifOnClickListener,
                                       View.OnLongClickListener startActionModeListener
                                       ) {
        if (displayMetrics == null)
            initStatics(view);
        int flagIdx = cursor.getColumnIndex(ChanPost.POST_FLAGS);
        int flags = flagIdx >= 0 ? cursor.getInt(flagIdx) : -1;
        if (flags < 0) { // we are on board list
            return BoardGridViewer.setViewValue(view, cursor, groupBoardCode);
        }
        else if ((flags & ChanPost.FLAG_IS_URLLINK) > 0) {
            return setUrlLinkView(view, cursor);
        }
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0) {
            return setTitleView(view, cursor);
        }
        else if ((flags & ChanPost.FLAG_IS_AD) > 0) {
            return setBannerAdView(view, cursor);
        }
        else {
            return setListItemView(view, cursor, flags,
                    imageOnClickListener,
                    backlinkOnClickListener, repliesOnClickListener, sameIdOnClickListener,
                    exifOnClickListener,
                    startActionModeListener);
        }
    }

    public static boolean setListItemView(final View view, final Cursor cursor, int flags,
                                          View.OnClickListener imageOnClickListener,
                                          View.OnClickListener backlinkOnClickListener,
                                          View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener,
                                          View.OnClickListener exifOnClickListener,
                                          View.OnLongClickListener startActionModeListener) {
        view.setOnLongClickListener(startActionModeListener);
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
                return setImage((ImageView) view, cursor, flags, imageOnClickListener);
            case R.id.list_item_country_flag:
                return setCountryFlag((ImageView) view, cursor, flags);
            case R.id.list_item_header:
                return setHeaderValue((TextView) view, cursor, repliesOnClickListener, sameIdOnClickListener);
            case R.id.list_item_subject:
                return setSubject((TextView) view, cursor, flags, backlinkOnClickListener);
            case R.id.list_item_text:
                return setText((TextView) view, cursor, flags, backlinkOnClickListener, exifOnClickListener);
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
        item.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, new Boolean(false));
        item.setTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED, new Boolean(false));
        ViewGroup itemHeaderWrapper = (ViewGroup) item.findViewById(R.id.list_item_header_wrapper);
        ViewGroup.LayoutParams params = itemHeaderWrapper.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
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


    static private boolean setHeaderValue(final TextView tv, final Cursor cursor,
                                          View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener) {
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT));
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text));
        addLinkedSpans(tv, spannable, POST_PATTERN, repliesOnClickListener);
        if (cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_SAME_IDS_BLOB)) != null)
            addLinkedSpans(tv, spannable, ID_PATTERN, sameIdOnClickListener);
        tv.setText(spannable);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    static private boolean setSubject(final TextView tv, final Cursor cursor, int flags,
                                      View.OnClickListener backlinkOnClickListener) {
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
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text));
        if (spannable.length() > 0) {
            addLinkedSpans(tv, spannable, POST_PATTERN, backlinkOnClickListener);
            tv.setText(spannable);
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

    static private final String SHOW_EXIF_TEXT = "Show EXIF Data";
    static private final String SHOW_EXIF_HTML = "<b>" + SHOW_EXIF_TEXT + "</b>";

    static private boolean setText(final TextView tv, final Cursor cursor, int flags,
                                   final View.OnClickListener backlinkOnClickListener,
                                   final View.OnClickListener exifOnClickListener) {
        String text;
        if ((flags & ChanPost.FLAG_IS_AD) > 0)
            text = "";
        else if ((flags & ChanPost.FLAG_HAS_TEXT) > 0)
            text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
        else
            text = "";
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0 && exifOnClickListener != null)
            text += (text.isEmpty() ? "" : " ") + SHOW_EXIF_HTML;
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text));
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0 && exifOnClickListener != null)
            addExifSpan(tv, spannable, exifOnClickListener);
        if (spannable.length() > 0) {
            addLinkedSpans(tv, spannable, POST_PATTERN, backlinkOnClickListener);
            tv.setText(spannable);
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    static private void addExifSpan(TextView tv, Spannable spannable, final View.OnClickListener listener) {
        if (listener == null)
            return;
        ClickableSpan exif = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                listener.onClick(widget);
            }
        };
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        spannable.setSpan(exif, spannable.length() - SHOW_EXIF_TEXT.length(), spannable.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static private final Pattern POST_PATTERN = Pattern.compile("(>>\\d+)");
    static private final Pattern ID_PATTERN = Pattern.compile("Id: ([A-Za-z0-9+./_:!-]+)");

    static private void addLinkedSpans(TextView tv, Spannable spannable, Pattern pattern,
                                       final View.OnClickListener listener) {
        if (listener == null)
            return;
        Matcher m = pattern.matcher(spannable);
        boolean found = false;
        while (m.find()) {
            ClickableSpan popup = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    listener.onClick(widget);
                }
            };
            spannable.setSpan(popup, m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            found = true;
        }
        if (found)
            tv.setMovementMethod(LinkMovementMethod.getInstance());
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
                                    View.OnClickListener imageOnClickListener) {
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        if ((flags & ChanPost.FLAG_HAS_IMAGE) == 0 || params == null)
            return clearImage(iv, params);

        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
        if (tn_w == 0 || tn_h == 0)  // we don't have height and width, so just show unscaled image
            return displayImageAtDefaultSize(iv, params, url);

        // scale image
        Point imageSize;
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            imageSize = sizeHeaderImage(tn_w, tn_h);
            params.width = imageSize.x;
            params.height = imageSize.y;
            //params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            //params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        else {
            imageSize = sizeItemImage(tn_w, tn_h);
            params.width = imageSize.x;
            params.height = imageSize.y;
        }
        ImageSize displayImageSize = new ImageSize(imageSize.x, imageSize.y);
        DisplayImageOptions options = createDisplayImageOptions(displayImageSize);

        // display image
        iv.setOnClickListener(imageOnClickListener);
        iv.setVisibility(View.VISIBLE);
        ImageLoadingListener listener = ((flags & ChanPost.FLAG_IS_AD) > 0) ? adImageLoadingListener : null;
        imageLoader.displayImage(url, iv, options, listener);

        return true;
    }

    static private boolean clearImage(final ImageView iv, ViewGroup.LayoutParams params) {
        iv.setImageBitmap(null);
        iv.setVisibility(View.VISIBLE);
        if (params == null) // something wrong in layout
            return true;
        params.width = 0;
        params.height = 0;
        return true;
    }

    static private boolean displayImageAtDefaultSize(final ImageView iv, ViewGroup.LayoutParams params, String url) {
        params.width = itemThumbWidth;
        params.height = itemThumbMaxHeight;
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setVisibility(View.VISIBLE);
        imageLoader.displayImage(url, iv, displayImageOptions);
        return true;
    }

    static private Point sizeHeaderImage(final int tn_w, final int tn_h) {
        Point imageSize = new Point();
        double scaleFactor = (double) tn_w / (double) tn_h;
        if (scaleFactor < 1) { // tall image, restrict by height
            int desiredHeight = Math.min(displayMetrics.heightPixels / 2, tn_h * MAX_HEADER_SCALE); // prevent excessive scaling
            imageSize.x = (int) (scaleFactor * (double) desiredHeight);
            imageSize.y = desiredHeight;
        } else {
            int desiredWidth = Math.min(displayMetrics.widthPixels / 2, tn_w * MAX_HEADER_SCALE); // prevent excessive scaling
            imageSize.x = desiredWidth; // restrict by width normally
            imageSize.y = (int) ((double) desiredWidth / scaleFactor);
        }
        if (DEBUG) Log.i(TAG, "Input size=" + tn_w + "x" + tn_h + " output size=" + imageSize.x + "x" + imageSize.y);
        return imageSize;
    }

    static private Point sizeItemImage(int tn_w, int tn_h) {
        Point imageSize = new Point();
        double scaleFactor = (double) tn_w / (double) tn_h;
        if (scaleFactor < 0.5) { // tall image, restrict by height
            imageSize.x = (int) (scaleFactor * (double) itemThumbMaxHeight);
            imageSize.y = itemThumbMaxHeight;
        } else {
            imageSize.x = itemThumbWidth; // restrict by width normally
            imageSize.y = (int) ((double) itemThumbWidth / scaleFactor);
        }
        //if (DEBUG) Log.i(TAG, "Item Input size=" + tn_w + "x" + tn_h + " output size=" + imageSize.x + "x" + imageSize.y);
        return imageSize;
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
                | ChanPost.FLAG_IS_BOARDLINK
                | ChanPost.FLAG_NO_EXPAND)) == 0)
            view.setOnClickListener(new ThreadImageOnClickListener(cursor));
        return true;
    }

    static private boolean setImageExpandedProgressBar(final ProgressBar progressBar) {
        progressBar.setVisibility(View.GONE);
        return true;
    }

    static private boolean setCountryFlag(final ImageView iv, final Cursor cursor, int flags) {
        if ((flags & ChanPost.FLAG_HAS_COUNTRY) > 0) {
            iv.setImageBitmap(null);
            iv.setVisibility(View.VISIBLE);
            String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_COUNTRY_URL));
            imageLoader.displayImage(url, iv, displayImageOptions);
        } else {
            iv.setImageBitmap(null);
            iv.setVisibility(View.GONE);
        }
        return true;
    }

    protected static boolean setUrlLinkView(View view, Cursor cursor) {
        if (view.getId() == R.id.list_item_urllink) {
            String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            Spanned spanned = Html.fromHtml("<u>" + text + "</u>");
            ((TextView)view).setText(spanned);
        }
        return true;
    }

    protected static boolean setTitleView(View view, Cursor cursor) {
        if (view.getId() == R.id.list_item_title) {
            String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            Spanned spanned = Html.fromHtml(text);
            ((TextView)view).setText(spanned);
        }
        return true;
    }

    protected static boolean setBannerAdView(View view, Cursor cursor) {
        switch (view.getId()) {
            case R.id.list_item_thread_banner_ad:
                return setBannerAd((ImageView) view, cursor);
            case R.id.list_item_thread_banner_ad_click_effect:
                return setBannerAdLayoutParams(view, cursor);
            default:
                return true;
        }
    }

    protected static boolean setBannerAd(final ImageView iv, Cursor cursor) {
        setBannerAdLayoutParams(iv, cursor);
        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (DEBUG) Log.i(TAG, "Displaying ad image iv=" + iv + " url=" + url);
        imageLoader.displayImage(url, iv, displayImageOptions, bannerAdImageLoadingListener);
        return true;
    }

    protected static boolean setBannerAdLayoutParams(final View v, Cursor cursor) {
        ViewParent parent = v.getParent();
        View parentView = parent == null ? null : (View)parent;
        if (parentView != null) {
            int measuredWidth = parentView.getMeasuredWidth();
            int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
            int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null)
                params.height = defaultAdHeight; // 48dp to avoid big jumps, precalc would be better
        }
        else { // approximate
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null)
                params.height = defaultAdHeight; // 48dp to avoid big jumps, precalc would be better
        }
        return true;
    }

}
