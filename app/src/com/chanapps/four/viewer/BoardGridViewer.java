package com.chanapps.four.viewer;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.*;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.LetterSpacingTextView;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import org.xml.sax.XMLReader;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/10/13
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridViewer {

    public static final int CATALOG_GRID = 0x01;
    public static final int ABBREV_BOARDS = 0x02;

    private static String TAG = BoardGridViewer.class.getSimpleName();
    private static boolean DEBUG = false;
    public static final String SUBJECT_FONT = "fonts/Roboto-BoldCondensed.ttf";

    private static ImageLoader imageLoader;
    private static DisplayImageOptions displayImageOptions;
    private static Typeface subjectTypeface;

    //protected static final int NUM_BOARD_CODE_COLORS = 5;

    public static void initStatics(Context context, boolean isDark) {
        Resources res = context.getResources();
        subjectTypeface = Typeface.createFromAsset(res.getAssets(), SUBJECT_FONT);
        imageLoader = ChanImageLoader.getInstance(context);
        int stub = isDark
                ? R.drawable.stub_image_background_dark
                : R.drawable.stub_image_background;
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.NONE)
                .cacheOnDisc()
                .cacheInMemory()
                .resetViewBeforeLoading()
                .showStubImage(stub)
                .build();
    }

    public static boolean setViewValue(View view, Cursor cursor, String groupBoardCode,
                                       int columnWidth, int columnHeight,
                                       View.OnClickListener overlayListener,
                                       View.OnClickListener overflowListener,
                                       int options,
                                       Typeface titleTypeface)
    {
        if (imageLoader == null)
            throw new IllegalStateException("Must call initStatics() before calling setViewValue()");
        int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        boolean isDark = ThemeSelector.instance(view.getContext()).isDark();
        BoardGridViewHolder viewHolder = (BoardGridViewHolder)view.getTag(R.id.VIEW_HOLDER);
        setItem(viewHolder, overlayListener, overflowListener, flags, options);
        setSubject(viewHolder, cursor, flags);
        setSubjectLarge(viewHolder, cursor, flags, subjectTypeface);
        setInfo(viewHolder, cursor, groupBoardCode, flags, options);
        setNumReplies(viewHolder, cursor, groupBoardCode, flags, options);
        setCountryFlag(viewHolder, cursor);
        setIcons(viewHolder, flags, isDark);
        setImage(viewHolder, cursor, groupBoardCode, flags, columnWidth, columnHeight, options, titleTypeface);
        return true;
    }

    protected static boolean setItem(BoardGridViewHolder viewHolder,
                                     View.OnClickListener overlayListener,
                                     View.OnClickListener overflowListener,
                                     int flags,
                                     int options) {
        boolean isHeader = (flags & ChanThread.THREAD_FLAG_HEADER) > 0;
        if (viewHolder.grid_item_line_1 != null)
            viewHolder.grid_item_line_1.setVisibility(isHeader ? View.GONE : View.VISIBLE);
        if (viewHolder.grid_item_line_2 != null)
            viewHolder.grid_item_line_2.setVisibility(isHeader ? View.GONE : View.VISIBLE);

        View overflow = viewHolder.grid_item_overflow_icon;
        if (overflow != null) {
            if ((options & ABBREV_BOARDS) > 0) {
                overflow.setVisibility(View.GONE);
            }
            else if (overflowListener == null) {
                overflow.setVisibility(View.GONE);
            }
            else {
                overflow.setOnClickListener(overflowListener);
                overflow.setVisibility(View.VISIBLE);
            }
        }

        ViewGroup overlay = viewHolder.grid_item;
        if (overlay != null) {
            if (overlayListener != null && !isHeader) {
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
            String name = board == null ? null : board.getName(context);
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(groupBoardCode))
                threadAbbrev += ""; // "/" + boardCode + "/";
            else if (name != null)
                threadAbbrev += name; //"/" + boardCode + "/ " + name;
            else
                threadAbbrev += ""; // "/" + boardCode + "/";
        }
        return threadAbbrev;
    }

    protected static boolean setSubject(BoardGridViewHolder viewHolder, Cursor cursor, int flags) {
        TextView tv = viewHolder.grid_item_thread_subject;
        if (tv == null)
            return false;
        if ((flags & ChanThread.THREAD_FLAG_HEADER) > 0) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }
        String s = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        String t = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        String u = (s != null && !s.isEmpty() ? "<b>" + s + "</b>" : "")
                + (s != null && t != null && !s.isEmpty() && !t.isEmpty() ? "<br/>" : "")
                + (t != null && !t.isEmpty() ? t : "");
        if (DEBUG) Log.i(TAG, "setSubject tv=" + tv + " u=" + u);
        if (u != null && !u.isEmpty()) {
            Spannable spannable = Spannable.Factory.getInstance().newSpannable(Html.fromHtml(u, null, spoilerTagHandler));
            tv.setText(spannable);
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

    protected static boolean setSubjectLarge(BoardGridViewHolder viewHolder, Cursor cursor, int flags, Typeface subjectTypeface) {
        TextView tv = viewHolder.grid_item_thread_subject_header;
        if (tv == null)
            return false;
        if ((flags & ChanThread.THREAD_FLAG_HEADER) == 0) {
            tv.setVisibility(View.GONE);
            tv.setText("");
            return true;
        }
        String u = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if (DEBUG) Log.i(TAG, "setSubjectLarge tv=" + tv + " u=" + u);
        if (u != null && !u.isEmpty()) {
            tv.setText(u);
            tv.setTypeface(subjectTypeface);
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

    protected static boolean setImage(BoardGridViewHolder viewHolder, Cursor cursor, String groupBoardCode,
                                      int flags, int columnWidth, int columnHeight, int options, Typeface titleTypeface) {
        ImageView iv = viewHolder.grid_item_thread_thumb;
        if (iv == null)
            return false;
        boolean isWideHeader = (flags & ChanThread.THREAD_FLAG_HEADER) > 0 && (options & CATALOG_GRID) == 0;
        if (isWideHeader) {
            iv.setImageBitmap(null);
            iv.setVisibility(View.GONE);
            return true;
        }

        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if ((options & ABBREV_BOARDS) > 0) {
            iv.setImageBitmap(null);
            iv.setVisibility(View.GONE);
            displayBoardCode(viewHolder, cursor, boardCode, groupBoardCode, titleTypeface, flags, options);
            return true;
        }
        View item = viewHolder.grid_item;
        iv.setVisibility(View.VISIBLE);
        sizeImage(iv, item, columnWidth, columnHeight, options);

        displayBoardCode(viewHolder, cursor, boardCode, groupBoardCode, titleTypeface, flags, options);
        String url = imageUrl(iv, boardCode, groupBoardCode, cursor, flags, options);
        if (url != null && !url.isEmpty())
            imageLoader.displayImage(url, iv, displayImageOptions, thumbLoadingListener);
        return true;
    }

    protected static final float BOARD_CODE_LETTER_SPACING = 0.65f;

    protected static void displayBoardCode(BoardGridViewHolder viewHolder,
                                           Cursor cursor,
                                           String boardCode, String groupBoardCode,
                                           Typeface titleTypeface,
                                           int flags,
                                           int options) {
        if ((options & ABBREV_BOARDS) > 0) {
            if (viewHolder.grid_item_board_code != null) {
                viewHolder.grid_item_board_code.setText("");
                viewHolder.grid_item_board_code.setVisibility(View.GONE);
            }
            if (viewHolder.grid_item_bottom_frame != null)
                viewHolder.grid_item_bottom_frame.setVisibility(View.GONE);
            displayNicelyFormattedBoardCode(titleTypeface, boardCode, viewHolder.grid_item_thread_subject_header_abbr);
        }
        else if (groupBoardCode != null
                && !ChanBoard.isVirtualBoard(groupBoardCode)
                && (flags & ChanThread.THREAD_FLAG_HEADER) > 0
                && (options & CATALOG_GRID) > 0
                && viewHolder.grid_item_thread_subject_header_abbr != null) { // grid header
            if (viewHolder.grid_item_board_code != null) {
                viewHolder.grid_item_board_code.setText("");
                viewHolder.grid_item_board_code.setVisibility(View.VISIBLE);
            }
            if (viewHolder.grid_item_bottom_frame != null)
                viewHolder.grid_item_bottom_frame.setVisibility(View.VISIBLE);
            displayNicelyFormattedBoardCode(titleTypeface, boardCode, viewHolder.grid_item_thread_subject_header_abbr);
        }
        else if (boardCode == null || boardCode.equals(groupBoardCode)) {
            if (viewHolder.grid_item_thread_subject_header_abbr != null)
                viewHolder.grid_item_thread_subject_header_abbr.setText("");
            if (viewHolder.grid_item_board_code != null) {
                viewHolder.grid_item_board_code.setText("");
                viewHolder.grid_item_board_code.setVisibility(View.VISIBLE);
            }
            if (viewHolder.grid_item_bottom_frame != null)
                viewHolder.grid_item_bottom_frame.setVisibility(View.VISIBLE);
        }
        else {
            if (viewHolder.grid_item_thread_subject_header_abbr != null)
                viewHolder.grid_item_thread_subject_header_abbr.setText("");
            if (viewHolder.grid_item_board_code != null)
                viewHolder.grid_item_board_code.setVisibility(View.VISIBLE);
            displayNicelyFormattedBoardCode(titleTypeface, boardCode, viewHolder.grid_item_board_code);
            if (viewHolder.grid_item_bottom_frame != null)
                viewHolder.grid_item_bottom_frame.setVisibility(View.VISIBLE);
        }
    }

    protected static void displayNicelyFormattedBoardCode(Typeface titleTypeface, String boardCode, TextView tv) {
        String boardCodeTitle = "/" + boardCode + "/";
        if (DEBUG) Log.i(TAG, "displayBoardCode() boardCodeTitle=" + boardCodeTitle);
        if (titleTypeface != null)
            tv.setTypeface(titleTypeface);
        if (tv instanceof LetterSpacingTextView)
            ((LetterSpacingTextView) tv).setLetterSpacing(BOARD_CODE_LETTER_SPACING);
        tv.setText(boardCodeTitle);
    }

    protected static String imageUrl(ImageView iv, String boardCode, String groupBoardCode,
                                     Cursor cursor, int flags, int options) {
        String url;
        long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
        if (DEBUG) Log.i(TAG, "setImage() /" + boardCode + "/" + threadNo);
        if (groupBoardCode != null
                && !ChanBoard.isVirtualBoard(groupBoardCode)
                && (flags & ChanThread.THREAD_FLAG_HEADER) > 0
                && (options & CATALOG_GRID) > 0) { // grid header
            //url = "drawable://" + R.drawable.transparent;
            int drawableId = ThemeSelector.instance(iv.getContext()).isDark()
                    ? R.drawable.bg_222
                    : R.drawable.bg_f4f4f4;
            url = "drawable://" + drawableId;
        }
        else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            url = null;
        }
        else if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
            url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL))
                    .split(ChanThread.AD_DELIMITER)[0];
        }
        //else if (threadNo <= 0) {
        //    if (DEBUG) Log.i(TAG, "setImage() /" + boardCode + "/" + threadNo + " displaying board code instead of image");
        //    url = null;
        //}
        else {
            url = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL));
            if (DEBUG) Log.i(TAG, "setImage() /" + boardCode + "/ url=" + url);
            int i = (Long.valueOf(threadNo % 3)).intValue();
            String defaultUrl = ChanBoard.getIndexedImageDrawableUrl(boardCode, i);
            iv.setTag(R.id.BOARD_GRID_VIEW_DEFAULT_DRAWABLE, defaultUrl);
            if (url == null || url.isEmpty())
                url = ChanBoard.getIndexedImageDrawableUrl(boardCode, i);
        }
        return url;
    }

    protected static void sizeImage(ImageView iv, View item, int columnWidth, int columnHeight, int options) {
        iv.setVisibility(View.VISIBLE);
        if ((options & CATALOG_GRID) > 0) {
            /*
            ViewParent parent = iv.getParent();
            if (parent != null && parent instanceof View) {
                View v = (View)parent;
                ViewGroup.LayoutParams params = v.getLayoutParams();
                if (columnWidth > 0 && params != null) {
                    params.width = columnWidth; // force square
                    params.height = columnWidth; // force square
                }
            }
            */
            ViewGroup.LayoutParams params = iv.getLayoutParams();
            if (columnWidth > 0 && params != null) {
                params.width = columnWidth; // force square
                params.height = columnWidth; // force square
            }
            /*
            ViewGroup.LayoutParams params2 = item.getLayoutParams();
            if (columnWidth > 0 && params2 != null) {
                params2.width = columnWidth; // force rectangle
                params2.height = (int)((double)columnWidth * 1.62d); // force rectangle
            }
            */
        }
        /*
        else {
            ViewGroup.LayoutParams params = iv.getLayoutParams();
            if (columnWidth > 0 && params != null) {
                params.width = columnWidth; // force square
                params.height = columnWidth; // force square
            }
        }
        */
    }

    /*
    protected static int colorIndex = -1;

    protected static void displayBoardCode(ImageView iv, TextView tv, String boardCode) {
        int idx = (colorIndex = (colorIndex + 1) % NUM_BOARD_CODE_COLORS);
        int color;
        switch (colorIndex) {
            default:
            case 0: color = R.color.PaletteBoardColor0; break;
            case 1: color = R.color.PaletteBoardColor1; break;
            case 2: color = R.color.PaletteBoardColor2; break;
            case 3: color = R.color.PaletteBoardColor3; break;
            case 4: color = R.color.PaletteBoardColor4; break;

            case 5: color = R.color.PaletteBoardColor5; break;
            case 6: color = R.color.PaletteBoardColor6; break;
            case 7: color = R.color.PaletteBoardColor7; break;
            case 8: color = R.color.PaletteBoardColor8; break;
            case 9: color = R.color.PaletteBoardColor9; break;
            case 10: color = R.color.PaletteBoardColor10; break;
        }
        if (DEBUG) Log.i(TAG, "setImage() displaying board code /" + boardCode + "/ color index=" + idx + " color=" + color);
        iv.setImageResource(color);
        if (tv != null)
            tv.setText("/" + boardCode + "/");
    }
    */
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

    protected static boolean setInfo(BoardGridViewHolder viewHolder, Cursor cursor, String groupBoardCode, int flags, int options) {
        TextView tv = viewHolder.grid_item_thread_info;
        TextView headerTv = viewHolder.grid_item_thread_info_header;
        if (tv == null)
            return false;
        if ((flags & ChanThread.THREAD_FLAG_HEADER) > 0) {
            tv.setText("");
            if (headerTv != null)
                headerTv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_HEADLINE))));
            return true;
        }
        else {
            if (headerTv != null)
                headerTv.setText("");
        }

        if (cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE)).equals(groupBoardCode)) {
            tv.setText("");
            return true;
        }
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
        }
        else {
            tv.setText("");
        }
        return true;
    }

    protected static boolean setNumReplies(BoardGridViewHolder viewHolder, Cursor cursor, String groupBoardCode,
                                           int flags, int options) {
        TextView numReplies = viewHolder.grid_item_num_replies_text;
        TextView numImages = viewHolder.grid_item_num_images_text;
        TextView numRepliesLabel = viewHolder.grid_item_num_replies_label;
        TextView numImagesLabel = viewHolder.grid_item_num_images_label;
        TextView numRepliesLabelAbbr = viewHolder.grid_item_num_replies_label_abbr;
        TextView numImagesLabelAbbr = viewHolder.grid_item_num_images_label_abbr;
        ImageView numRepliesImg = viewHolder.grid_item_num_replies_img;
        ImageView numImagesImg = viewHolder.grid_item_num_images_img;

        if ((flags & ChanThread.THREAD_FLAG_HEADER) > 0
                || (ChanBoard.isVirtualBoard(groupBoardCode) && !ChanBoard.WATCHLIST_BOARD_CODE.equals(groupBoardCode))
                || ((options & ABBREV_BOARDS) > 0)
            ) {
            if (numReplies != null)
                numReplies.setVisibility(View.GONE);
            if (numImages != null)
                numImages.setVisibility(View.GONE);
            if (numRepliesLabel != null)
                numRepliesLabel.setVisibility(View.GONE);
            if (numImagesLabel != null)
                numImagesLabel.setVisibility(View.GONE);
            if (numRepliesLabelAbbr != null)
                numRepliesLabelAbbr.setVisibility(View.GONE);
            if (numImagesLabelAbbr != null)
                numImagesLabelAbbr.setVisibility(View.GONE);
            if (numRepliesImg != null)
                numRepliesImg.setVisibility(View.GONE);
            if (numImagesImg != null)
                numImagesImg.setVisibility(View.GONE);
            return true;
        }

        int r = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_REPLIES));
        int i = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_NUM_IMAGES));
        if (numRepliesLabel != null) {
            numRepliesLabel.setText(numRepliesLabel.getResources().getQuantityString(R.plurals.thread_num_replies_label, r));
            numRepliesLabel.setVisibility(View.VISIBLE);
        }
        if (numImagesLabel != null) {
            numImagesLabel.setText(numImagesLabel.getResources().getQuantityString(R.plurals.thread_num_images_label, i));
            numImagesLabel.setVisibility(View.VISIBLE);
        }
        if (numRepliesLabelAbbr != null) {
            numRepliesLabelAbbr.setVisibility(View.VISIBLE);
        }
        if (numImagesLabelAbbr != null) {
            numImagesLabelAbbr.setVisibility(View.VISIBLE);
        }
        if (numReplies != null) {
            numReplies.setText(r >= 0 ? String.valueOf(r) : "");
            numReplies.setVisibility(View.VISIBLE);
        }
        if (numImages != null) {
            numImages.setText(i >= 0 ? String.valueOf(i) : "");
            numImages.setVisibility(View.VISIBLE);
        }
        if (numRepliesImg != null)
            numRepliesImg.setVisibility(View.VISIBLE);
        if (numImagesImg != null)
            numImagesImg.setVisibility(View.VISIBLE);
        /*
        if (r >= 0) {
            numReplies.setText(String.valueOf(r));
            viewHolder.grid_item_num_replies_text.setVisibility(View.VISIBLE);
            //viewHolder.grid_item_num_replies_img.setVisibility(View.VISIBLE);
        }
        else {
            numReplies.setText("");
            //viewHolder.grid_item_num_replies_text.setVisibility(View.GONE);
            //viewHolder.grid_item_num_replies_img.setVisibility(View.GONE);
        }

        if (i >= 0) {
            numImages.setText(String.valueOf(i));
            //viewHolder.grid_item_num_images_text.setVisibility(View.VISIBLE);
            //viewHolder.grid_item_num_images_img.setVisibility(View.VISIBLE);
        }
        else {
            numImages.setText("");
            //viewHolder.grid_item_num_images_text.setVisibility(View.GONE);
            //viewHolder.grid_item_num_images_img.setVisibility(View.GONE);
        }
        */
        return true;
    }

    protected static final int DRAWABLE_ALPHA_LIGHT = 0xaa;
    protected static final int DRAWABLE_ALPHA_DARK = 0xee;

    protected static boolean setIcons(BoardGridViewHolder viewHolder, int flags, boolean isDark) {
        int alpha = isDark ? DRAWABLE_ALPHA_DARK : DRAWABLE_ALPHA_LIGHT;
        if (viewHolder.grid_item_dead_icon != null) {
            viewHolder.grid_item_dead_icon.setVisibility((flags & ChanThread.THREAD_FLAG_DEAD) > 0 ? View.VISIBLE : View.GONE);
            viewHolder.grid_item_dead_icon.setAlpha(alpha);
        }
        if (viewHolder.grid_item_closed_icon != null) {
            viewHolder.grid_item_closed_icon.setVisibility((flags & ChanThread.THREAD_FLAG_CLOSED) > 0 ? View.VISIBLE : View.GONE);
            viewHolder.grid_item_closed_icon.setAlpha(alpha);
        }
        if (viewHolder.grid_item_sticky_icon != null) {
            viewHolder.grid_item_sticky_icon.setVisibility((flags & ChanThread.THREAD_FLAG_STICKY) > 0 ? View.VISIBLE : View.GONE);
            viewHolder.grid_item_sticky_icon.setAlpha(alpha);
        }
        if (DEBUG)
            Log.i(TAG, "setSubjectIcons()"
                    + " dead=" + ((flags & ChanThread.THREAD_FLAG_DEAD) > 0)
                    + " closed=" + ((flags & ChanThread.THREAD_FLAG_CLOSED) > 0)
                    + " sticky=" + ((flags & ChanThread.THREAD_FLAG_STICKY) > 0)
            );
        return true;
    }

    /* similar to ThreadViewer version, but not clickable */
    static private final Html.TagHandler spoilerTagHandler = new Html.TagHandler() {
        static private final String SPOILER_TAG = "s";
        class SpoilerSpan extends CharacterStyle {
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

}