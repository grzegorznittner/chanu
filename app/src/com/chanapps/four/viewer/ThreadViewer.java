package com.chanapps.four.viewer;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.*;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ThreadExpandImageOnClickListener;
import com.chanapps.four.component.ThreadImageOnClickListener;
import com.chanapps.four.data.ChanAd;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import org.xml.sax.XMLReader;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
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
    private static DisplayImageOptions thumbDisplayImageOptions = null;

    public static void initStatics(View view) {
        if (displayMetrics != null)
            return;
        Resources res = view.getResources();
        displayMetrics = res.getDisplayMetrics();
        subjectTypeface = Typeface.createFromAsset(res.getAssets(), SUBJECT_FONT);
        defaultAdHeight = ChanGridSizer.dpToPx(displayMetrics, DEFAULT_AD_HEIGHT_DP);
        itemThumbWidth = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_WIDTH_DP);
        itemThumbMaxHeight = ChanGridSizer.dpToPx(displayMetrics, ITEM_THUMB_MAXHEIGHT_DP);
        imageLoader = ChanImageLoader.getInstance(view.getContext());
        displayImageOptions = createDisplayImageOptions(null);
        thumbDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc()
                .cacheInMemory()
                .imageScaleType(ImageScaleType.NONE)
                .resetViewBeforeLoading()
                .build();
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
                                       boolean isTablet,
                                       int columnWidth,
                                       int columnHeight,
                                       View.OnClickListener imageOnClickListener,
                                       View.OnClickListener backlinkOnClickListener,
                                       View.OnClickListener repliesOnClickListener,
                                       View.OnClickListener sameIdOnClickListener,
                                       View.OnClickListener exifOnClickListener,
                                       View.OnLongClickListener startActionModeListener
                                       ) {
        initStatics(view);
        int flagIdx = cursor.getColumnIndex(ChanPost.POST_FLAGS);
        int flags = flagIdx >= 0 ? cursor.getInt(flagIdx) : -1;
        if (flags < 0) // we are on board list
            return BoardGridViewer.setViewValue(view, cursor, groupBoardCode, columnWidth, columnHeight);
        else if ((flags & ChanPost.FLAG_IS_URLLINK) > 0)
            return setUrlLinkView(view, cursor);
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            return setTitleView(view, cursor);
        else if ((flags & ChanPost.FLAG_IS_BUTTON) > 0)
            return setButtonView(view, cursor);
        else if ((flags & ChanPost.FLAG_IS_AD) > 0)
            return setBannerAdView(view, cursor, isTablet);
        else if ((flags & (ChanPost.FLAG_IS_BOARDLINK | ChanPost.FLAG_IS_THREADLINK)) > 0)
            return setListLinkView(view, cursor, flags);
        else
            return setListItemView(view, cursor, flags,
                    imageOnClickListener,
                    backlinkOnClickListener, repliesOnClickListener, sameIdOnClickListener,
                    exifOnClickListener,
                    startActionModeListener);
    }

    public static boolean setListLinkView(final View view, final Cursor cursor, int flags) {
        switch (view.getId()) {
            case R.id.list_item:
                return setItem((ViewGroup) view, cursor, flags, null, null);
            case R.id.list_item_image_wrapper:
                return setImageWrapper((ViewGroup) view, cursor, flags);
            case R.id.list_item_image:
                return setImage((ImageView) view, cursor, flags, null);
            case R.id.list_item_country_flag:
                return setCountryFlag((ImageView) view, cursor, flags);
            case R.id.list_item_header:
                return setHeaderValue((TextView) view, cursor, null, null);
            case R.id.list_item_subject:
                return setSubject((TextView) view, cursor, flags, null);
            default:
                return false;
        }
    }

    public static boolean setListItemView(final View view, final Cursor cursor, int flags,
                                          View.OnClickListener imageOnClickListener,
                                          View.OnClickListener backlinkOnClickListener,
                                          View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener,
                                          View.OnClickListener exifOnClickListener,
                                          View.OnLongClickListener startActionModeListener) {
        if (startActionModeListener != null)
            view.setOnLongClickListener(startActionModeListener);
        switch (view.getId()) {
            case R.id.list_item:
                return setItem((ViewGroup) view, cursor, flags, backlinkOnClickListener, repliesOnClickListener);
            case R.id.list_item_image_expanded_wrapper:
                return setImageExpandedWrapper((ViewGroup) view);
            case R.id.list_item_image_expanded:
                return setImageExpanded((ImageView) view);
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
            case R.id.list_item_exif_text:
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

    static protected boolean setItem(ViewGroup item, Cursor cursor, int flags,
                                     View.OnClickListener backlinkOnClickListener,
                                     View.OnClickListener repliesOnClickListener) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        item.setTag((flags | ChanPost.FLAG_IS_AD) > 0 ? null : postId);
        item.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.FALSE);
        item.setTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED, Boolean.FALSE);
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0)
            displayHeaderCountFields(item, cursor, repliesOnClickListener);
        else
            displayItemCountFields(item, cursor, backlinkOnClickListener, repliesOnClickListener);
        item.setVisibility(View.VISIBLE);
        /*
        if (cursor.getPosition() == 1) {
            int top = item.getResources().getDimensionPixelSize(R.dimen.ThreadListLayout_paddingBottom);
            item.setPadding(item.getPaddingLeft(), top, item.getPaddingRight(), item.getPaddingBottom());
        }
        else {
            item.setPadding(item.getPaddingLeft(), 0, item.getPaddingRight(), item.getPaddingBottom());
        }
        */
        return true;
    }

    static protected void displayHeaderCountFields(View item, Cursor cursor,
                                                   View.OnClickListener repliesOnClickListener) {
        displayHeaderBarAgoNo(item, cursor);
        int r = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_NUM_REPLIES));
        int i = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_NUM_IMAGES));
        TextView numReplies = (TextView)item.findViewById(R.id.list_item_num_replies_text);
        TextView numImages = (TextView)item.findViewById(R.id.list_item_num_images_text);
        TextView numRepliesLabel = (TextView)item.findViewById(R.id.list_item_num_replies_label);
        TextView numImagesLabel = (TextView)item.findViewById(R.id.list_item_num_images_label);
        if (numReplies != null)
            numReplies.setText(String.valueOf(r));
        if (numImages != null)
            numImages.setText(String.valueOf(i));
        if (numRepliesLabel != null)
            numRepliesLabel.setText(item.getResources().getQuantityString(R.plurals.thread_num_replies_label, r));
        if (numImagesLabel != null)
            numImagesLabel.setText(item.getResources().getQuantityString(R.plurals.thread_num_images_label, i));
        displayNumDirectReplies(item, cursor, repliesOnClickListener, false);
    }

    static protected void displayHeaderBarAgoNo(View item, Cursor cursor) {
        String dateText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_DATE_TEXT));
        TextView ago = (TextView)item.findViewById(R.id.list_item_header_bar_ago);
        if (ago != null)
            ago.setText(dateText);
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        TextView no = (TextView)item.findViewById(R.id.list_item_header_bar_no);
        if (no != null)
            no.setText(String.valueOf(postNo));
    }

    static protected void displayItemCountFields(View item, Cursor cursor,
                                                 View.OnClickListener backlinkOnClickListener,
                                                 View.OnClickListener repliesOnClickListener) {
        displayHeaderBarAgoNo(item, cursor);
        //n += displayNumRefs(item, cursor, backlinkOnClickListener);
        displayNumDirectReplies(item, cursor, repliesOnClickListener, true);
    }

    static protected int displayNumDirectReplies(View item, Cursor cursor,
                                                  View.OnClickListener repliesOnClickListener,
                                                  boolean markVisibility) {
        View wrapper = item.findViewById(R.id.list_item_num_direct_replies);
        if (wrapper == null)
            return 0;
        TextView numDirectReplies = (TextView)item.findViewById(R.id.list_item_num_direct_replies_text);
        if (numDirectReplies == null)
            return 0;
        int directReplies = numDirectReplies(cursor);
        TextView numRepliesLabel = (TextView)item.findViewById(R.id.list_item_num_direct_replies_label);
        if (numRepliesLabel != null)
            numRepliesLabel.setText(
                    item.getResources().getQuantityString(R.plurals.thread_num_direct_replies_label, directReplies));

        numDirectReplies.setText(String.valueOf(directReplies));
        if (directReplies > 0) {
            wrapper.setOnClickListener(repliesOnClickListener);
            if (markVisibility)
                wrapper.setVisibility(View.VISIBLE);
        }
        else {
            wrapper.setOnClickListener(null);
            if (markVisibility)
                wrapper.setVisibility(View.GONE);
        }
        return directReplies;
    }
    /*
    static protected int displayNumRefs(View item, Cursor cursor,
                                                  View.OnClickListener backlinkOnClickListener) {
        View wrapper = item.findViewById(R.id.list_item_num_refs);
        TextView numRefs = (TextView)item.findViewById(R.id.list_item_num_refs_text);

        int refs = numRefs(cursor);

        numRefs.setText(String.valueOf(refs));
        if (refs > 0) {
            wrapper.setOnClickListener(backlinkOnClickListener);
            wrapper.setVisibility(View.VISIBLE);
        }
        else {
            wrapper.setOnClickListener(null);
            wrapper.setVisibility(View.GONE);
        }
        return refs;
    }
    */
    static protected int numDirectReplies(Cursor cursor) {
        byte[] b = cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_REPLIES_BLOB));
        if (b == null || b.length == 0)
            return 0;
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0)
            return 0;
        return links.size();
    }

    static protected int numRefs(Cursor cursor) {
        byte[] b = cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_BACKLINKS_BLOB));
        if (b == null || b.length == 0)
            return 0;
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0)
            return 0;
        return links.size();
    }

    static protected boolean setHeaderWrapper(ViewGroup wrapper, int flags) {
        if ((flags & ChanPost.FLAG_IS_AD) > 0) {
            wrapper.setBackgroundResource(R.drawable.thread_ad_bg_gradient);
            wrapper.setVisibility(View.VISIBLE);
        }
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0) {
            wrapper.setBackgroundResource(R.drawable.thread_button_gradient_bg);
            wrapper.setVisibility(View.VISIBLE);
        }
        else if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0) {
            wrapper.setBackgroundResource(0);
            wrapper.setVisibility(View.VISIBLE);
        }
        else if ((flags & ChanPost.FLAG_HAS_HEAD) > 0) {
            wrapper.setBackgroundResource(0);
            wrapper.setVisibility(View.VISIBLE);
        }
        else {
            wrapper.setBackgroundResource(0);
            wrapper.setVisibility(View.GONE);
        }
        return true;
    }


    static private boolean setHeaderValue(final TextView tv, final Cursor cursor,
                                          View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener) {
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_HEADLINE_TEXT));
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }
        //if (repliesOnClickListener != null || sameIdOnClickListener != null)
        //    tv.setMovementMethod(LinkMovementMethod.getInstance());
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text));
        //if (repliesOnClickListener != null)
        //    addLinkedSpans(spannable, REPLY_PATTERN, repliesOnClickListener);
        if (cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_SAME_IDS_BLOB)) != null
                && sameIdOnClickListener != null) {
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            addLinkedSpans(spannable, ID_PATTERN, sameIdOnClickListener);
        }
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
        if ((flags & ChanPost.FLAG_HAS_SUBJECT) == 0) {
            tv.setText("");
            tv.setVisibility(View.GONE);
            return true;
        }
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        /*
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            if ((flags & ChanPost.FLAG_IS_CLOSED) > 0)
                text = tv.getResources().getString(R.string.thread_is_closed) + (text.isEmpty() ? "" : " ") + text;
            if ((flags & ChanPost.FLAG_IS_DEAD) > 0)
                text = tv.getResources().getString(R.string.thread_is_dead) + (text.isEmpty() ? "" : " ") + text;
        }
        */
        if (DEBUG) Log.v(TAG, "setSubject text=" + text);
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text, null, spoilerTagHandler));
        if (spannable.length() > 0) {
            if (backlinkOnClickListener != null) {
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                addLinkedSpans(spannable, POST_PATTERN, backlinkOnClickListener);
            }
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
        if ((flags & ChanPost.FLAG_IS_AD) > 0) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }

        if ((flags & (ChanPost.FLAG_HAS_TEXT | ChanPost.FLAG_HAS_EXIF)) == 0) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }

        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_TEXT));
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0 && exifOnClickListener != null)
            text += (text.isEmpty() ? "" : " ") + SHOW_EXIF_HTML;

        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text, null, spoilerTagHandler));
        if (spannable.length() == 0) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }

        if (backlinkOnClickListener != null || exifOnClickListener != null)
            tv.setMovementMethod(LinkMovementMethod.getInstance());
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0 && exifOnClickListener != null)
            addExifSpan(tv, spannable, exifOnClickListener);
        if (backlinkOnClickListener != null)
            addLinkedSpans(spannable, POST_PATTERN, backlinkOnClickListener);

        if (DEBUG) Log.v(TAG, "setText spannable=" + spannable + " len=" + spannable.length());
        tv.setText(spannable);
        tv.setVisibility(View.VISIBLE);
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
        spannable.setSpan(exif, spannable.length() - SHOW_EXIF_TEXT.length(), spannable.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static private final Pattern POST_PATTERN = Pattern.compile("(>>\\d+)");
    static private final Pattern REPLY_PATTERN = Pattern.compile("(1 Reply|\\d+ Replies)");
    static private final Pattern ID_PATTERN = Pattern.compile("Id: ([A-Za-z0-9+./_:!-]+)");

    static private void addLinkedSpans(Spannable spannable, Pattern pattern,
                                       final View.OnClickListener listener) {
        if (listener == null)
            return;
        Matcher m = pattern.matcher(spannable);
        while (m.find()) {
            ClickableSpan popup = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    listener.onClick(widget);
                }
            };
            spannable.setSpan(popup, m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
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

        File file = fullSizeImageFile(iv.getContext(), cursor); // try for full size first
        if (file != null && file.exists() && file.canRead() && file.length() > 0) {
            View itemView = (flags & ChanPost.FLAG_IS_HEADER) > 0
                    ? (View)iv.getParent().getParent()
                    : (View)iv.getParent().getParent().getParent();
            if (itemView != null) {
                if (DEBUG) Log.i(TAG, "setImage file=" + file.getAbsolutePath());
                ThreadExpandImageOnClickListener expander =
                        (new ThreadExpandImageOnClickListener(iv.getContext(), cursor, itemView));
                expander.setShowProgressBar(false);
                expander.onClick(itemView);
                return true;
            }
        }

        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if ((flags & ChanPost.FLAG_IS_HEADER) == 0) {
            if (url != null && !url.isEmpty()) {
                if (DEBUG) Log.i(TAG, "setImage url=" + url);
                if (imageOnClickListener != null)
                    iv.setOnClickListener(imageOnClickListener);
                iv.setVisibility(View.VISIBLE);
                imageLoader.displayImage(url, iv, thumbDisplayImageOptions);
            }
            else {
                if (DEBUG) Log.i(TAG, "setImage null image");
                iv.setVisibility(View.GONE);
                iv.setImageBitmap(null);
            }
            return true;
        }

        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));

        if ((flags & ChanPost.FLAG_HAS_SPOILER) > 0) { // don't size based on hidden image, size based on filler image
            tn_w = 250;
            tn_h = 250;
        }

        if (tn_w == 0 || tn_h == 0)  // we don't have height and width, so just show unscaled image
            return displayImageAtDefaultSize(iv, params, url);

        // scale image
        Point imageSize;
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            imageSize = sizeHeaderImage(tn_w, tn_h);
            params.width = imageSize.x;
            params.height = imageSize.y;
        }
        else {
            imageSize = sizeItemImage(tn_w, tn_h);
            params.width = imageSize.x;
            params.height = imageSize.y;
        }
        ImageSize displayImageSize = new ImageSize(imageSize.x, imageSize.y);
        DisplayImageOptions options = createDisplayImageOptions(displayImageSize);

        // display image
        if (imageOnClickListener != null)
            iv.setOnClickListener(imageOnClickListener);
        iv.setVisibility(View.VISIBLE);
        ImageLoadingListener listener = ((flags & ChanPost.FLAG_IS_AD) > 0) ? adImageLoadingListener : null;
        if (DEBUG) Log.i(TAG, "setImage header url=" + url);
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

    public static Point sizeHeaderImage(final int tn_w, final int tn_h) {
        Point imageSize = new Point();
        double scaleFactor = (double) tn_w / (double) tn_h;
        if (scaleFactor < 1) { // tall image, restrict by height
            int desiredHeight = Math.min(displayMetrics.heightPixels / 2, tn_h * MAX_HEADER_SCALE); // prevent excessive scaling
            imageSize.x = (int) (scaleFactor * (double) desiredHeight);
            imageSize.y = desiredHeight;
        } else {
            int desiredWidth = Math.min(displayMetrics.widthPixels, tn_w * MAX_HEADER_SCALE); // prevent excessive scaling
            imageSize.x = desiredWidth; // restrict by width normally
            imageSize.y = (int) ((double) desiredWidth / scaleFactor);
        }
        if (DEBUG) Log.v(TAG, "Input size=" + tn_w + "x" + tn_h + " output size=" + imageSize.x + "x" + imageSize.y);
        return imageSize;
    }

    public static Point sizeItemImage(int tn_w, int tn_h) {
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

    static private boolean setImageExpandedWrapper(final ViewGroup v) {
        v.setVisibility(View.GONE);
        return true;
    }

    static private boolean setImageExpanded(final ImageView iv) {
        clearBigImageView(iv);
        iv.setVisibility(View.GONE);
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

    protected static boolean setButtonView(View view, Cursor cursor) {
        if (view.getId() == R.id.list_item_button) {
            String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            Spanned spanned = Html.fromHtml(text);
            ((TextView)view).setText(spanned);
        }
        return true;
    }

    protected static boolean setBannerAdView(View view, Cursor cursor, boolean isTablet) {
        switch (view.getId()) {
            case R.id.list_item_thread_banner_ad:
                return setBannerAd((ImageView) view, cursor, isTablet);
            case R.id.list_item_thread_banner_ad_click_effect:
                return setBannerAdLayoutParams(view, cursor, isTablet);
            default:
                return true;
        }
    }

    protected static boolean setBannerAd(final ImageView iv, Cursor cursor, boolean isTablet) {
        setBannerAdLayoutParams(iv, cursor, isTablet);
        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (DEBUG) Log.i(TAG, "Displaying ad image iv=" + iv + " url=" + url);
        imageLoader.displayImage(url, iv, displayImageOptions, bannerAdImageLoadingListener);
        return true;
    }

    protected static boolean setBannerAdLayoutParams(final View v, Cursor cursor, boolean isTablet) {
        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params == null)
            return false;
        double tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        double tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
        double viewWidth = displayMetrics.widthPixels
                - v.getResources().getDimensionPixelSize(R.dimen.BoardGridViewTablet_layout_width);
        double scale = viewWidth / tn_w;
        double scaledHeight = scale * tn_h;
        if (DEBUG) Log.i(TAG, "Ad inSize=" + tn_w + "x" + tn_h + " outSize=" + viewWidth + "x" + scaledHeight);
        params.width = (int)viewWidth;
        params.height = (int)scaledHeight;
        return true;
    }

    static private final Html.TagHandler spoilerTagHandler = new Html.TagHandler() {
        static private final String SPOILER_TAG = "s";
        public static final int PALETTE_BLACK = 0xFF2D2D2D;
        class SpoilerSpan extends ClickableSpan {
            private int start = 0;
            private int end = 0;
            private boolean blackout = true;
            public SpoilerSpan() {
                super();
            }
            public SpoilerSpan(int start, int end) {
                this();
                this.start = start;
                this.end = end;
            }
            @Override
            public void onClick(View widget) {
                if (!(widget instanceof TextView))
                    return;
                TextView tv = (TextView)widget;
                CharSequence cs = tv.getText();
                if (!(cs instanceof Spannable))
                    return;
                Spannable s = (Spannable)cs;
                Object[] spans = s.getSpans(start, end, this.getClass());
                if (spans == null || spans.length == 0)
                    return;
                if (DEBUG) Log.i(TAG, "Found " + spans.length + " spans");
                blackout = false;
                widget.invalidate();
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.bgColor = blackout ? PALETTE_BLACK : 0; // palette black 2d2d2d
            }
        }
        class SpanFactory {
            public Class getSpanClass() { return SpoilerSpan.class; }
            public Object getSpan(final int start, final int end) { return new SpoilerSpan(start, end); }
        }
        SpanFactory spanFactory = new SpanFactory();
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (SPOILER_TAG.equals(tag))
                handleSpoiler(opening, output);
        }
        private void handleSpoiler(boolean opening, Editable output) {
            if (opening)
                handleSpoilerOpen(output);
            else
                handleSpoilerClose(output);
        }
        private void handleSpoilerOpen(Editable output) {
            if (DEBUG) Log.i(TAG, "handleSpoilerOpen(" + output + ")");
            int len = output.length();
            output.setSpan(spanFactory.getSpan(len, len), len, len, Spannable.SPAN_MARK_MARK);
        }
        private void handleSpoilerClose(Editable output) {
            if (DEBUG) Log.i(TAG, "handleSpoilerClose(" + output + ")");
            int len = output.length();
            Object obj = getFirst(output, spanFactory.getSpanClass());
            int start = output.getSpanStart(obj);
            output.removeSpan(obj);
            if (start >= 0 && len >= 0 && start != len)  {
                output.setSpan(spanFactory.getSpan(start, len), start, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (DEBUG) Log.i(TAG, "setSpan(" + start + ", " + len + ")");
            }
        }
        private Object getFirst(Editable text, Class kind) {
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0)
                return null;
            if (DEBUG) Log.i(TAG, "Found " + objs.length + " matching spans");
            for (int i = 0; i < objs.length; i++) {
                Object span = objs[i];
                if (text.getSpanFlags(span) == Spannable.SPAN_MARK_MARK)
                    return span;
            }
            return null;
        }
        private Object getLast(Editable text, Class kind) {
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0)
                return null;
            for (int i = objs.length - 1; i >= 0; i--) {
                Object span = objs[i];
                if (text.getSpanFlags(span) == Spannable.SPAN_MARK_MARK)
                    return span;
            }
            return null;
        }
    };

    public static void safeClearImageView(ImageView v) {
        /*
        Drawable d = v.getDrawable();
        if (d != null && d instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable)d;
            Bitmap b = bd.getBitmap();
            if (b != null)
                b.recycle();
        }
        */
        v.setImageBitmap(null);
    }

    public static void clearBigImageView(final ImageView v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Drawable d = v.getDrawable();
                if (d != null && d instanceof BitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable)d;
                    Bitmap b = bd.getBitmap();
                    if (b != null)
                        b.recycle();
                }
            }
        }).start();
        v.setImageBitmap(null);
    }

    public static File fullSizeImageFile(Context context, Cursor cursor) {
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String ext = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(context, boardCode, postNo, ext);
        File localImage = new File(URI.create(uri.toString()));
        if (localImage != null && localImage.exists() && localImage.canRead() && localImage.length() > 0)
            return localImage;
        else
            return null;
    }

}
