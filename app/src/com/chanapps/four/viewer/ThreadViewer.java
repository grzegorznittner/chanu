package com.chanapps.four.viewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.*;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.ThreadImageExpander;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.FontSize;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
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

    //public static final double MAX_HEADER_SCALE = 1.5;
    public static final double MAX_HEADER_SCALE = 1.2;
    public static final String SUBJECT_FONT = "fonts/Roboto-BoldCondensed.ttf";

    private static final String TAG = ThreadViewer.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static DisplayMetrics displayMetrics = null;
    private static Typeface subjectTypeface = null;
    private static int cardPaddingPx = 0;
    private static ImageLoader imageLoader = null;
    private static DisplayImageOptions displayImageOptions = null;
    private static DisplayImageOptions thumbDisplayImageOptions = null;
    private static int stub;
    private static int boardTabletViewWidthPx = 0;

    public static void initStatics(Context context, boolean isDark) {
        imageLoader = ChanImageLoader.getInstance(context);
        stub = isDark
                ? R.drawable.stub_image_background_dark
                : R.drawable.stub_image_background;
        Resources res = context.getResources();
        cardPaddingPx = res.getDimensionPixelSize(R.dimen.BoardGridView_spacing);
        boardTabletViewWidthPx = res.getDimensionPixelSize(R.dimen.BoardGridViewTablet_layout_width);
        displayMetrics = res.getDisplayMetrics();
        subjectTypeface = Typeface.createFromAsset(res.getAssets(), SUBJECT_FONT);
        displayImageOptions = createDisplayImageOptions(null);
        thumbDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc()
                //.cacheInMemory()
                .imageScaleType(ImageScaleType.NONE)
                .resetViewBeforeLoading()
                .showStubImage(stub)
                .build();
    }

    private static DisplayImageOptions createDisplayImageOptions(ImageSize imageSize) {
        return new DisplayImageOptions.Builder()
                .cacheOnDisc()
                //.cacheInMemory()
                .imageSize(imageSize)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .showStubImage(stub)
                .resetViewBeforeLoading()
                .build();
    }

    public static boolean setViewValue(final View view, final Cursor cursor, String groupBoardCode,
                                       boolean showContextMenu,
                                       int columnWidth,
                                       int columnHeight,
                                       View.OnClickListener thumbOnClickListener,
                                       SpannableOnClickListener backlinkOnClickListener,
                                       View.OnClickListener commentsOnClickListener,
                                       View.OnClickListener imagesOnClickListener,
                                       View.OnClickListener repliesOnClickListener,
                                       View.OnClickListener sameIdOnClickListener,
                                       View.OnClickListener exifOnClickListener,
                                       //View.OnClickListener postReplyListener,
                                       View.OnClickListener overflowListener,
                                       View.OnClickListener expandedImageListener,
                                       View.OnClickListener itemBoardLinkListener,
                                       View.OnLongClickListener startActionModeListener,
                                       View.OnClickListener goToThreadUrlListener
                                       ) {
        if (imageLoader == null)
            throw new IllegalStateException("Must call initStatics() before calling setViewValue()");
        int flagIdx = cursor.getColumnIndex(ChanPost.POST_FLAGS);
        int flags = flagIdx >= 0 ? cursor.getInt(flagIdx) : -1;
        if (flags < 0) // we are on board list
            return BoardGridViewer.setViewValue(view, cursor, groupBoardCode, columnWidth, columnHeight, null, null, 0, null);
        //else if ((flags & ChanPost.FLAG_IS_URLLINK) > 0)
        //    return setUrlLinkView(view, cursor);
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            return setTitleView(view, cursor);
        //else if ((flags & ChanPost.FLAG_IS_BUTTON) > 0)
        //    return setButtonView(view, cursor);
        //else if ((flags & ChanPost.FLAG_IS_AD) > 0)
        //    return setBannerAdView(view, cursor, isTablet);
        else if (isListLink(flags))
            return setListLinkView(view, cursor, flags, itemBoardLinkListener);
        else
            return setListItemView(view, cursor, flags,
                    showContextMenu,
                    thumbOnClickListener,
                    backlinkOnClickListener,
                    commentsOnClickListener,
                    imagesOnClickListener,
                    repliesOnClickListener,
                    sameIdOnClickListener,
                    exifOnClickListener,
                    //postReplyListener,
                    overflowListener,
                    expandedImageListener,
                    startActionModeListener,
                    goToThreadUrlListener
            );
    }

    protected static boolean setListLinkView(final View view, final Cursor cursor, int flags, View.OnClickListener itemBoardLinkListener) {
        ThreadViewHolder viewHolder = (ThreadViewHolder)view.getTag(R.id.VIEW_HOLDER);
        setItem(viewHolder, cursor, flags, false, null, null, null, null, itemBoardLinkListener);
        setImageWrapper(viewHolder, flags);
        setImage(viewHolder, cursor, flags, null, null);
        setCountryFlag(viewHolder, cursor, flags);
        setHeaderValue(viewHolder, cursor, null);
        setSubject(viewHolder, cursor, flags, null);
        return true;
    }

    protected static boolean setListItemView(final View view, final Cursor cursor, int flags,
                                          boolean showContextMenu,
                                          View.OnClickListener thumbOnClickListener,
                                          SpannableOnClickListener backlinkOnClickListener,
                                          View.OnClickListener commentsOnClickListener,
                                          View.OnClickListener imagesOnClickListener,
                                          View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener,
                                          View.OnClickListener exifOnClickListener,
                                          //View.OnClickListener postReplyListener,
                                          View.OnClickListener overflowListener,
                                          View.OnClickListener expandedImageListener,
                                          final View.OnLongClickListener startActionModeListener,
                                          View.OnClickListener goToThreadUrlListener
                                          ) {
        //if (startActionModeListener != null)
        //    view.setOnLongClickListener(startActionModeListener);
        ThreadViewHolder viewHolder = (ThreadViewHolder)view.getTag(R.id.VIEW_HOLDER);
        setItem(viewHolder, cursor, flags, showContextMenu,
                //backlinkOnClickListener,
                commentsOnClickListener,
                imagesOnClickListener,
                repliesOnClickListener,
                //postReplyListener,
                overflowListener,
                null);
        setImageWrapper(viewHolder, flags);
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            setHeaderImage(viewHolder, cursor, flags, thumbOnClickListener, expandedImageListener);
        }
        else {
            setImage(viewHolder, cursor, flags, thumbOnClickListener, expandedImageListener);
        }
        setCountryFlag(viewHolder, cursor, flags);
        setHeaderValue(viewHolder, cursor, sameIdOnClickListener);
        setSubject(viewHolder, cursor, flags, backlinkOnClickListener);
        setSubjectIcons(viewHolder, flags);
        setText(viewHolder, cursor, flags, backlinkOnClickListener, exifOnClickListener);
        setImageExifValue(viewHolder);
        return true;
    }

    static protected boolean setItem(ThreadViewHolder viewHolder, Cursor cursor, int flags,
                                     boolean showContextMenu,
                                     //View.OnClickListener backlinkOnClickListener,
                                     View.OnClickListener commentsOnClickListener,
                                     View.OnClickListener imagesOnClickListener,
                                     View.OnClickListener repliesOnClickListener,
                                     //View.OnClickListener postReplyListener,
                                     View.OnClickListener overflowListener,
                                     View.OnClickListener itemBoardLinkListener)
    {
        ViewGroup item = viewHolder.list_item;
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        item.setTag((flags & ChanPost.FLAG_IS_AD) > 0 ? null : postId);
        item.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.FALSE);
        item.setTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED, Boolean.FALSE);

        if ((flags & ChanPost.FLAG_IS_HEADER) > 0)
            displayHeaderCountFields(viewHolder, cursor, showContextMenu,
                    commentsOnClickListener, imagesOnClickListener, repliesOnClickListener);
        else
            displayItemCountFields(viewHolder, cursor, showContextMenu, repliesOnClickListener);

        if (isListLink(flags)) {
            viewHolder.list_item.setOnClickListener(itemBoardLinkListener);
        }
        else {
            viewHolder.list_item.setOnClickListener(null);
            viewHolder.list_item.setClickable(false);
        }

        View listItemLeftSpacer = viewHolder.list_item_left_spacer;
        if (listItemLeftSpacer != null)
            listItemLeftSpacer.setVisibility((flags & ChanPost.FLAG_HAS_IMAGE) > 0 ? View.GONE : View.VISIBLE);
        View overflow = viewHolder.list_item_header_bar_overflow_wrapper;
        if (overflow != null) {
            if (showContextMenu) {
                overflow.setOnClickListener(overflowListener);
                overflow.setVisibility(View.VISIBLE);
                if (viewHolder.list_item_right_menu_spacer != null)
                    viewHolder.list_item_right_menu_spacer.setVisibility(View.GONE);
            }
            else {
                overflow.setOnClickListener(null);
                overflow.setVisibility(View.GONE);
                if (viewHolder.list_item_right_menu_spacer != null)
                    viewHolder.list_item_right_menu_spacer.setVisibility(View.VISIBLE);
            }
        }
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

    static public void setHeaderNumRepliesImages(ThreadViewHolder viewHolder, Cursor cursor,
                                                 View.OnClickListener commentsOnClickListener,
                                                 View.OnClickListener imagesOnClickListener) {
        int r = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_NUM_REPLIES));
        int i = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_NUM_IMAGES));
        TextView numReplies = viewHolder.list_item_num_replies_text;
        TextView numImages = viewHolder.list_item_num_images_text;
        TextView numRepliesLabel = viewHolder.list_item_num_replies_label;
        TextView numImagesLabel = viewHolder.list_item_num_images_label;
        View cmtWrapper = viewHolder.list_item_num_replies;
        View arrow = viewHolder.list_item_num_comments_spinner;
        View imgWrapper = viewHolder.list_item_num_images;
        View spinner = viewHolder.list_item_num_images_spinner;
        if (numReplies != null) {
            FontSize.sizeTextView(numReplies);
            numReplies.setText(String.valueOf(r));
        }
        if (numImages != null) {
            FontSize.sizeTextView(numImages);
            numImages.setText(String.valueOf(i));
        }
        if (numRepliesLabel != null) {
            FontSize.sizeTextView(numRepliesLabel);
            numRepliesLabel.setText(numRepliesLabel.getResources().getQuantityString(R.plurals.thread_num_replies_label, r));
        }
        if (numImagesLabel != null) {
            FontSize.sizeTextView(numImagesLabel);
            numImagesLabel.setText(numImagesLabel.getResources().getQuantityString(R.plurals.thread_num_images_label, i));
        }
        if (cmtWrapper != null) {
            if (r >= 0 && commentsOnClickListener != null && cursor.getCount() > 1) {
                cmtWrapper.setOnClickListener(commentsOnClickListener);
                cmtWrapper.setClickable(true);
                if (arrow != null)
                    arrow.setVisibility(View.VISIBLE);
            }
            else {
                cmtWrapper.setOnClickListener(null);
                cmtWrapper.setClickable(false);
                if (arrow != null)
                    arrow.setVisibility(View.GONE);
            }
        }
        if (imgWrapper != null) {
            if (i >= 0 && imagesOnClickListener != null) {
                imgWrapper.setOnClickListener(imagesOnClickListener);
                imgWrapper.setClickable(true);
                if (spinner != null)
                    spinner.setVisibility(View.VISIBLE);
            }
            else {
                imgWrapper.setOnClickListener(null);
                imgWrapper.setClickable(false);
                if (spinner != null)
                    spinner.setVisibility(View.GONE);
            }
        }
    }

    static protected void displayHeaderCountFields(ThreadViewHolder viewHolder, Cursor cursor, boolean showContextMenu,
                                                   View.OnClickListener commentsOnClickListener,
                                                   View.OnClickListener imagesOnClickListener,
                                                   View.OnClickListener repliesOnClickListener) {
        displayHeaderBarAgoNo(viewHolder, cursor);
        setHeaderNumRepliesImages(viewHolder, cursor, commentsOnClickListener, imagesOnClickListener);
        displayNumDirectReplies(viewHolder, cursor, showContextMenu, repliesOnClickListener);
    }

    static protected void displayHeaderBarAgoNo(ThreadViewHolder viewHolder, Cursor cursor) {
        String dateText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_DATE_TEXT));
        TextView ago = viewHolder.list_item_header_bar_ago;
        if (ago != null) {
            String sep = ago.getResources().getString(R.string.list_item_ago_date_separator);
            if (sep != null && !sep.isEmpty())
                sep += " ";
            FontSize.sizeTextView(ago);
            ago.setText(sep + dateText);
        }
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        TextView no = viewHolder.list_item_header_bar_no;
        if (no != null) {
            FontSize.sizeTextView(no);
            no.setText(String.valueOf(postNo));
        }
    }

    static protected void displayItemCountFields(ThreadViewHolder viewHolder, Cursor cursor, boolean showContextMenu,
                                                 //View.OnClickListener backlinkOnClickListener,
                                                 View.OnClickListener repliesOnClickListener) {
        displayHeaderBarAgoNo(viewHolder, cursor);
        //n += displayNumRefs(item, cursor, backlinkOnClickListener);
        displayNumDirectReplies(viewHolder, cursor, showContextMenu, repliesOnClickListener); //, true);
    }

    static public int displayNumDirectReplies(ThreadViewHolder viewHolder, Cursor cursor, boolean showContextMenu,
                                                  View.OnClickListener repliesOnClickListener) { //,
                                                  //boolean markVisibility) {
        View wrapper = viewHolder.list_item_num_direct_replies;
        if (wrapper == null)
            return 0;
        if (!showContextMenu) {
            wrapper.setVisibility(View.GONE);
            return 0;
        }
        TextView numDirectReplies = viewHolder.list_item_num_direct_replies_text;
        if (numDirectReplies == null)
            return 0;

        int directReplies = numDirectReplies(cursor);
        FontSize.sizeTextView(numDirectReplies);
        numDirectReplies.setText(String.valueOf(directReplies));
        if (directReplies > 0) {
            wrapper.setOnClickListener(repliesOnClickListener);
            wrapper.setVisibility(View.VISIBLE);
        }
        else {
            wrapper.setOnClickListener(null);
            wrapper.setVisibility(View.GONE);
        }
        return directReplies;
    }

    static protected int numDirectReplies(Cursor cursor) {
        byte[] b = cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_REPLIES_BLOB));
        if (b == null || b.length == 0)
            return 0;
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0)
            return 0;
        return links.size();
    }

    static private boolean setHeaderValue(ThreadViewHolder viewHolder, final Cursor cursor,
                                          //View.OnClickListener repliesOnClickListener,
                                          View.OnClickListener sameIdOnClickListener) {
        TextView tv = viewHolder.list_item_header;
        if (tv == null)
            return false;
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
        FontSize.sizeTextView(tv);
        tv.setText(spannable);
        tv.setVisibility(View.VISIBLE);
        return true;
    }

    static private boolean setSubject(ThreadViewHolder viewHolder, final Cursor cursor, int flags,
                                      SpannableOnClickListener backlinkOnClickListener) {
        TextView tv = viewHolder.list_item_subject;
        if (tv == null)
            return false;
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
        if (DEBUG) Log.v(TAG, "setSubject text=" + text);
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(text, null, spoilerTagHandler));
        if (spannable.length() > 0) {
            if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
                tv.setTypeface(subjectTypeface);
            }
            else {
                FontSize.sizeTextView(tv);
            }
            tv.setText(spannable);
            if (backlinkOnClickListener != null) {
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                addLinkedSpans(spannable, POST_PATTERN, backlinkOnClickListener);
            }
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setText("");
            tv.setVisibility(View.GONE);
        }
        return true;
    }

    static public boolean setSubjectIcons(ThreadViewHolder viewHolder, int flags) {
        if (viewHolder.list_item_dead_icon != null)
            viewHolder.list_item_dead_icon.setVisibility((flags & ChanPost.FLAG_IS_DEAD) > 0 ? View.VISIBLE : View.GONE);
        if (viewHolder.list_item_closed_icon != null)
            viewHolder.list_item_closed_icon.setVisibility((flags & ChanPost.FLAG_IS_CLOSED) > 0 ? View.VISIBLE : View.GONE);
        if (viewHolder.list_item_sticky_icon != null)
            viewHolder.list_item_sticky_icon.setVisibility((flags & ChanPost.FLAG_IS_STICKY) > 0 ? View.VISIBLE : View.GONE);
        if (DEBUG)
            Log.d(TAG, "setSubjectIcons()"
                    + " dead=" + ((flags & ChanPost.FLAG_IS_DEAD) > 0)
                    + " closed=" + ((flags & ChanPost.FLAG_IS_CLOSED) > 0)
                    + " sticky=" + ((flags & ChanPost.FLAG_IS_STICKY) > 0)
            );
        return true;
    }

    static private final String SHOW_EXIF_TEXT = "Show EXIF Data";
    static private final String SHOW_EXIF_HTML = "<b>" + SHOW_EXIF_TEXT + "</b>";

    static private boolean setText(ThreadViewHolder viewHolder, final Cursor cursor, int flags,
                                   final SpannableOnClickListener backlinkOnClickListener,
                                   final View.OnClickListener exifOnClickListener) {
        TextView tv = viewHolder.list_item_text;
        if (tv == null)
            return false;
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

        String html = markupHtml(text);
        //if (DEBUG) Log.i(TAG, "text before replace:" + text);
        //if (DEBUG) Log.i(TAG, "text after  replace:" + html);
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(html, null, spoilerTagHandler));
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

        //if (DEBUG) Log.v(TAG, "setText spannable=" + spannable + " len=" + spannable.length());
        FontSize.sizeTextView(tv);
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

    static private final String QUOTE_RE_FIRST = "^(>[^<>]*)(<br/?>)";
    static private final String QUOTE_RE_FIRST_REPLACE = "<font color=\"#7a9441\">$1</font>$2";   // #7a9441
    static private final String QUOTE_RE_MID = "(<br/?>)(>[^<>]*)";
    static private final String QUOTE_RE_MID_REPLACE = "$1<font color=\"#7a9441\">$2</font>";   // #7a9441
    static private final Pattern POST_PATTERN = Pattern.compile("(>>(\\d+))");
    //static private final Pattern REPLY_PATTERN = Pattern.compile("(1 Reply|\\d+ Replies)");
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

    static private void addLinkedSpans(Spannable spannable, Pattern pattern,
                                       final SpannableOnClickListener listener) {
        if (listener == null)
            return;
        Matcher m = pattern.matcher(spannable);
        while (m.find()) {
            String post = m.group(2);
            long postNo = -1;
            try {
                postNo = Long.parseLong(post);
            }
            catch (NumberFormatException e) {
                Log.e(TAG, "Exception parsing long: " + post);
            }
            final long finalPostNo = postNo;
            ClickableSpan popup = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    listener.onClick(widget, finalPostNo);
                }
            };
            spannable.setSpan(popup, m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    static private String markupHtml(String in) {
        String out = in
                .replaceFirst(QUOTE_RE_FIRST, QUOTE_RE_FIRST_REPLACE)
                .replaceAll(QUOTE_RE_MID, QUOTE_RE_MID_REPLACE)
                ;
        return out;
    }

    static private boolean setImageExifValue(ThreadViewHolder viewHolder) {
        TextView tv = viewHolder.list_item_exif_text;
        if (tv == null)
            return false;
        FontSize.sizeTextView(tv);
        tv.setText("");
        tv.setVisibility(View.GONE);
        return true;
    }


    static private boolean setImageWrapper(ThreadViewHolder viewHolder, final int flags) {
        View v = viewHolder.list_item_image_wrapper;
        if (v == null)
            return false;
        if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
        return true;
    }

    static private boolean setHeaderImage(ThreadViewHolder viewHolder, final Cursor cursor, int flags,
                                    View.OnClickListener thumbOnClickListener,
                                    View.OnClickListener expandedImageListener) {
        ImageView iv = viewHolder.list_item_image;
        if (iv == null)
            return false;
        if (DEBUG) Log.i(TAG, "setHeaderImage()");
        if (viewHolder.list_item_image_collapse != null)
            viewHolder.list_item_image_collapse.setVisibility(View.GONE);
        if (!shouldLoadThumbs(iv.getContext())) {
            if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0) {
                if (viewHolder.list_item_image_nothumbs_expand != null)
                    viewHolder.list_item_image_nothumbs_expand.setVisibility(View.VISIBLE);
                bindThumbnailExpandTarget(viewHolder.list_item_image_expansion_target, thumbOnClickListener);
            }
            return hideImage(viewHolder, iv, null);
        }
        if (viewHolder.list_item_image_wrapper != null)
            viewHolder.list_item_image_wrapper.setVisibility(View.GONE);
        if (viewHolder.list_item_image_nothumbs_expand != null)
            viewHolder.list_item_image_nothumbs_expand.setVisibility(View.GONE);
        if (hideNoImage(viewHolder, iv, null, flags))
            return true;
        if (displayCachedExpandedImage(viewHolder, cursor, expandedImageListener))
            return true;
        boolean isDead = (flags & ChanPost.FLAG_IS_DEAD) > 0;
        if (!isDead && prefetchExpandedImage(viewHolder, cursor, expandedImageListener))
            return true;
        return displayHeaderImage(viewHolder, cursor, flags, thumbOnClickListener);
    }

    static private boolean shouldLoadThumbs(Context context) {
        String autoloadType = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_AUTOLOAD_IMAGES,
                        context.getString(R.string.pref_autoload_images_default_value));
        return !(context.getString(R.string.pref_autoload_images_nothumbs_value).equals(autoloadType));
    }

    static private boolean setImage(ThreadViewHolder viewHolder, final Cursor cursor, int flags,
                                    View.OnClickListener thumbOnClickListener,
                                    View.OnClickListener expandedImageListener) {
        ImageView iv = viewHolder.list_item_image;
        ImageView spinner = viewHolder.list_item_image_spinner;
        if (iv == null)
            return false;
        if (viewHolder.list_item_image_collapse != null)
            viewHolder.list_item_image_collapse.setVisibility(View.GONE);
        if (!shouldLoadThumbs(iv.getContext())) {
            if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0) {
                if (viewHolder.list_item_image_nothumbs_expand != null)
                    viewHolder.list_item_image_nothumbs_expand.setVisibility(View.VISIBLE);
                bindThumbnailExpandTarget(viewHolder.list_item_image_expansion_target, thumbOnClickListener);
            }
            return hideImage(viewHolder, iv, spinner);
        }
        if (viewHolder.list_item_image_nothumbs_expand != null)
            viewHolder.list_item_image_nothumbs_expand.setVisibility(View.GONE);
        if (hideNoImage(viewHolder, iv, spinner, flags))
            return true;
        //if (isListLink(flags))
        //    return displayNonHeaderImage(iv, null, cursor, null);
        setSpinnerTarget(spinner, expandedImageListener);

        // display thumb and also expand if available
        displayNonHeaderImage(iv, viewHolder.list_item_image_expansion_target, cursor, thumbOnClickListener);
        if (displayCachedExpandedImage(viewHolder, cursor, expandedImageListener))
            return true;
        boolean isDead = (flags & ChanPost.FLAG_IS_DEAD) > 0;
        if (!isDead && prefetchExpandedImage(viewHolder, cursor, expandedImageListener))
            return true;
        return true;
    }

    static private void setSpinnerTarget(ImageView spinner, View.OnClickListener expandedImageListener) {
        if (spinner == null || expandedImageListener == null)
            return;
        spinner.setOnClickListener(expandedImageListener);
        spinner.setVisibility(View.VISIBLE);
    }

    static private boolean isListLink(int flags) {
        return (flags & (ChanPost.FLAG_IS_BOARDLINK | ChanPost.FLAG_IS_THREADLINK)) > 0; // skip for unexpandables
    }

    static private boolean displayHeaderImage(ThreadViewHolder viewHolder, Cursor cursor, int flags,
                                           View.OnClickListener thumbOnClickListener) {
        ImageView iv = viewHolder.list_item_image;
        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
        if ((flags & ChanPost.FLAG_HAS_SPOILER) > 0 || tn_w <= 0 || tn_h <= 0) { // don't size based on hidden image, size based on filler image
            tn_w = 250;
            tn_h = 250;
        }
        //if (tn_w == 0 || tn_h == 0)  // we don't have height and width, so just show unscaled image
        //    return displayHeaderImageAtDefaultSize(iv, url);

        // scale image
        Point imageSize;
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        //if ((flags & ChanPost.FLAG_IS_HEADER) > 0) {
            imageSize = sizeHeaderImage(tn_w, tn_h);
            if (params != null) {
                params.width = imageSize.x;
                params.height = imageSize.y;
            }
        //}
        //else {
        //    imageSize = sizeItemImage(tn_w, tn_h);
        //    if (params != null) {
        //        params.width = imageSize.x;
        //        params.height = imageSize.y;
        //    }
        //}
        ImageSize displayImageSize = new ImageSize(imageSize.x, imageSize.y);
        DisplayImageOptions options = createDisplayImageOptions(displayImageSize);

        // display image
        if (thumbOnClickListener != null)
            iv.setOnClickListener(thumbOnClickListener);
        viewHolder.list_item_image_expanded_wrapper.setVisibility(View.VISIBLE);
        iv.setVisibility(View.VISIBLE);
        //ImageLoadingListener listener = ((flags & ChanPost.FLAG_IS_AD) > 0) ? adImageLoadingListener : null;
        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (DEBUG) Log.i(TAG, "displayHeaderImage() url=" + url);
        //imageLoader.displayImage(url, iv, options, listener);
        imageLoader.displayImage(url, iv, options);

        return true;
    }

    static private boolean hideNoImage(ThreadViewHolder viewHolder, ImageView iv, ImageView spinner, int flags) {
        if ((flags & ChanPost.FLAG_HAS_IMAGE) == 0) {
            return hideImage(viewHolder, iv, spinner);
        }
        else {
            iv.setVisibility(View.VISIBLE);
            if (spinner != null)
                spinner.setVisibility(View.VISIBLE);
            return false;
        }
    }

    static private boolean hideImage(ThreadViewHolder viewHolder, ImageView iv, ImageView spinner) {
        if (DEBUG) Log.i(TAG, "hideImage()");
        iv.setImageBitmap(null);
        iv.setVisibility(View.GONE);
        if (spinner != null)
            spinner.setVisibility(View.GONE);
        return hideExpandedImage(viewHolder);
    }

    static private boolean prefetchExpandedImage(ThreadViewHolder viewHolder, final Cursor cursor,
                                                            final View.OnClickListener expandedImageListener) {
        if (viewHolder.list_item == null)
            return false;
        boolean autoload = shouldAutoload(viewHolder.list_item.getContext(), cursor);
        if (!autoload)
            return false;
        ThreadImageExpander expander =
                (new ThreadImageExpander(viewHolder, cursor, expandedImageListener, true, stub));
        expander.displayImage();
        return true;
    }

    static private boolean shouldAutoload(Context context, final Cursor cursor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String autoloadType = prefs.getString(SettingsActivity.PREF_AUTOLOAD_IMAGES,
                context.getString(R.string.pref_autoload_images_default_value));
        long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        if (context.getString(R.string.pref_autoload_images_nothumbs_value).equals(autoloadType))
            return false;
        else if (resto == 0)
            return shouldAutoloadBySizeAndNetwork(cursor);
        else if (context.getString(R.string.pref_autoload_images_never_value).equals(autoloadType))
            return false;
        else if (context.getString(R.string.pref_autoload_images_always_value).equals(autoloadType))
            return true;
        //else if (context.getString(R.string.pref_autoload_images_auto_value).equals(autoloadType))
        else
            return shouldAutoloadBySizeAndNetwork(cursor);
    }

    static private boolean shouldAutoloadBySizeAndNetwork(final Cursor cursor) {
        int fsize = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FSIZE));
        int maxAutoloadFSize = NetworkProfileManager.instance().getCurrentProfile().getFetchParams().maxAutoLoadFSize;
        //if (DEBUG) Log.i(TAG, "prefetchExpandedImage auto-expanding since fsize=" + fsize + " < " + maxAutoloadFSize);
        return (fsize <= maxAutoloadFSize);
    }

    static private boolean displayCachedExpandedImage(ThreadViewHolder viewHolder, final Cursor cursor,
                                                      final View.OnClickListener expandedImageListener) {
        File file = fullSizeImageFile(viewHolder.list_item.getContext(), cursor); // try for full size first
        if (file == null) {
            return !hideExpandedImage(viewHolder);
        }

        if (DEBUG) Log.i(TAG, "displayCachedExpandedImage() expanded file=" + file.getAbsolutePath());
        ThreadImageExpander expander =
                (new ThreadImageExpander(viewHolder, cursor, expandedImageListener, false, stub));
        expander.displayImage();
        return true;
    }

    static private boolean hideExpandedImage(ThreadViewHolder viewHolder) {
        View itemExpandedImage = viewHolder.list_item_image_expanded;
        View itemExpandedImageClickEffect = viewHolder.list_item_image_expanded_click_effect;
        View itemExpandedProgressBar = viewHolder.list_item_expanded_progress_bar;
        View itemExpandedImageWrapper = viewHolder.list_item_image_expanded_wrapper;
        if (itemExpandedImage != null)
            itemExpandedImage.setVisibility(View.GONE);
        if (itemExpandedImageClickEffect != null)
            itemExpandedImageClickEffect.setVisibility(View.GONE);
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.GONE);
        if (itemExpandedImageWrapper != null)
            itemExpandedImageWrapper.setVisibility(View.GONE);
        return true;
    }

    static private void bindThumbnailExpandTarget(final FrameLayout wrapper, View.OnClickListener thumbOnClickListener) {
        if (wrapper == null)
            return;
        if (thumbOnClickListener != null) {
            wrapper.setOnClickListener(thumbOnClickListener);
            wrapper.setForeground(wrapper.getResources().getDrawable(R.drawable.thread_list_selector_bg));
        }
        else {
            wrapper.setOnClickListener(null);
            wrapper.setForeground(wrapper.getResources().getDrawable(R.drawable.null_selector_bg));
        }
    }

    static private boolean displayNonHeaderImage(final ImageView iv, final FrameLayout wrapper, final Cursor cursor,
                                                 View.OnClickListener thumbOnClickListener) {
        String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (url != null && !url.isEmpty()) {
            if (DEBUG) Log.i(TAG, "setImage url=" + url);
            bindThumbnailExpandTarget(wrapper, thumbOnClickListener);
            iv.setVisibility(View.VISIBLE);
            imageLoader.displayImage(url, iv, thumbDisplayImageOptions);
        }
        else {
            if (DEBUG) Log.i(TAG, "setImage null image");
            iv.setVisibility(View.GONE);
            iv.setImageBitmap(null);
            if (wrapper != null) {
                wrapper.setOnClickListener(null);
                wrapper.setForeground(wrapper.getResources().getDrawable(R.drawable.null_selector_bg));
            }
        }
        return true;
    }
    /*
    static private boolean displayHeaderImageAtDefaultSize(final ImageView iv, String url) {
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        if (params != null) {
            params.width = itemThumbWidth;
            params.height = itemThumbMaxHeight;
        }
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setVisibility(View.VISIBLE);
        imageLoader.displayImage(url, iv, displayImageOptions);
        return true;
    }
    */

    public static Point sizeHeaderImage(final int tn_w, final int tn_h) {
        Point baseBox = new Point(tn_w, tn_h);
        Point scaledBox = new Point((int)(tn_w * MAX_HEADER_SCALE), (int)(tn_h * MAX_HEADER_SCALE));
        Point cardBox = new Point(cardMaxImageWidth(), cardMaxImageHeight());
        //baseBox <= scaleBox <= cardBox;

        //Point scaledBox = new Point(baseBox.x, baseBox.y);
        if (scaledBox.x > cardBox.x) { // downscale to fix x in card
            double scale = (double)cardBox.x / (double)scaledBox.x;
            scaledBox.x = (int)(scale * scaledBox.x);
            scaledBox.y = (int)(scale * scaledBox.y);
        }
        if (scaledBox.y > cardBox.y) { // downscale to fit y in card
            double scale = (double)cardBox.y / (double)scaledBox.y;
            scaledBox.x = (int)(scale * scaledBox.x);
            scaledBox.y = (int)(scale * scaledBox.y);
        }

        /*
        Point imageSize = new Point();
        double aspectRatio = (double) tn_w / (double) tn_h;
        if (aspectRatio < 1) { // tall image, restrict by height
            double desiredHeight = Math.min(cardMaxImageHeight(),
                    (tn_h * MAX_HEADER_SCALE)); // prevent excessive scaling
            imageSize.x = (int)(aspectRatio * desiredHeight);
            imageSize.y = (int)desiredHeight;
        } else {
            double desiredWidth = Math.min(cardMaxImageWidth(),
                    (tn_w * MAX_HEADER_SCALE)); // prevent excessive scaling
            imageSize.x = (int)desiredWidth; // restrict by width normally
            imageSize.y = (int)(desiredWidth / aspectRatio);
        }
        */
        
        if (DEBUG) Log.v(TAG, "Image size input=" + tn_w + "x" + tn_h
                + " box=" + cardBox.x + "x" + cardBox.y
                + " output=" + scaledBox.x + "x" + scaledBox.y);
        return scaledBox;
    }

    public static int cardMaxImageWidth() {
        int naiveMax;
        //if (displayMetrics.widthPixels < displayMetrics.heightPixels) // portrait
            naiveMax = displayMetrics.widthPixels - cardPaddingPx - cardPaddingPx;
        //else // landscape
        //    naiveMax = displayMetrics.widthPixels - cardPaddingPx - cardPaddingPx;
            //naiveMax = displayMetrics.widthPixels / 2 - cardPaddingPx - cardPaddingPx;
        naiveMax -= boardTabletViewWidthPx;
        return naiveMax;
    }

    public static int   cardMaxImageHeight() {
        int naiveMax;
        //if (displayMetrics.widthPixels < displayMetrics.heightPixels) // portrait
            naiveMax = displayMetrics.heightPixels - cardPaddingPx - cardPaddingPx;
        //else // landscape
        //    naiveMax = displayMetrics.heightPixels - cardPaddingPx - cardPaddingPx;
            //naiveMax = displayMetrics.heightPixels - cardPaddingPx - cardPaddingPx;
        //naiveMax -= boardTabletViewWidthPx;
        return naiveMax;
    }

    /*
    public static Point sizeItemImage(int tn_w, int tn_h) {
        Point imageSize = new Point();
        double aspectRatio = (double) tn_w / (double) tn_h;
        if (aspectRatio < 0.5) { // tall image, restrict by height
            imageSize.x = (int) (aspectRatio * (double) itemThumbMaxHeight);
            imageSize.y = itemThumbMaxHeight;
        } else {
            imageSize.x = itemThumbWidth; // restrict by width normally
            imageSize.y = (int) ((double) itemThumbWidth / aspectRatio);
        }
        //if (DEBUG) Log.i(TAG, "Item Input size=" + tn_w + "x" + tn_h + " output size=" + imageSize.x + "x" + imageSize.y);
        return imageSize;
    }
    */

    static private boolean setCountryFlag(ThreadViewHolder viewHolder, final Cursor cursor, int flags) {
        ImageView iv = viewHolder.list_item_country_flag;
        if (iv == null)
            return false;
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

    protected static boolean setTitleView(View view, Cursor cursor) {
        ThreadViewHolder viewHolder = (ThreadViewHolder)view.getTag(R.id.VIEW_HOLDER);
        TextView tv = viewHolder.list_item_title;
        if (tv == null)
            return false;
        String text = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
        Spanned spanned = Html.fromHtml(text);
        tv.setText(spanned);
        return true;
    }

    static private final Html.TagHandler spoilerTagHandler = new Html.TagHandler() {
        static private final String SPOILER_TAG = "s";
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
                if (blackout) {
                    int textColor = ds.getColor();
                    ds.bgColor = textColor;
                }
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
        Uri uri = ChanFileStorage.getHiddenLocalImageUri(context, boardCode, postNo, ext);
        File localImage = new File(URI.create(uri.toString()));
        if (localImage != null && localImage.exists() && localImage.canRead() && localImage.length() > 0)
            return localImage;
        else
            return null;
    }

    public static View.OnClickListener createCommentsOnClickListener(final AbsListView absListView, final Handler handler) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToBottom(absListView, handler);
            }
        };
    }

    public static View.OnClickListener createImagesOnClickListener(final Context context,
                                                                   final String boardCode,
                                                                   final long threadNo)
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GalleryViewActivity.startAlbumViewActivity(context, boardCode, threadNo);
            }
        };
    }

    public static void jumpToBottom(final AbsListView absListView, final Handler handler) {
        if (absListView == null)
            return;
        Adapter adapter = absListView.getAdapter();
        final int n = adapter == null ? -1 : (adapter.getCount() - 1);
        if (DEBUG) android.util.Log.i(TAG, "jumping to item n=" + n);
        if (handler != null && n >= 0)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    absListView.setSelection(n);
                }
            });
    }

}
