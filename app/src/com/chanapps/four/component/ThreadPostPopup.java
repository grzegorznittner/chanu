package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.adapter.AbstractThreadCursorAdapter;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.BlocklistAddDialogFragment;
import com.chanapps.four.fragment.DeletePostDialogFragment;
import com.chanapps.four.fragment.ReportPostDialogFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/2/13
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadPostPopup implements Dismissable {

    public static final String TAG = ThreadPostPopup.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected AbstractThreadCursorAdapter adapter;

    private static final int POPUP_BUTTON_HEIGHT_DP = 48;

    protected RefreshableActivity activity;
    protected LayoutInflater layoutInflater;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    protected View popupView;
    protected ScrollView popupScrollView;
    protected PopupWindow popupWindow;
    protected TextView deadThreadTextView;
    protected TextView closedThreadTextView;
    protected TextView spoilerTextView;
    protected TextView exifTextView;

    protected View spoilerButtonLine;
    protected Button spoilerButton;
    protected View exifButtonLine;
    protected Button exifButton;
    protected View translateButtonLine;
    protected Button translateButton;
    protected View blockButtonLine;
    protected Button blockButton;
    protected View deleteButtonLine;
    protected Button deleteButton;
    protected View replyButtonLine;
    protected Button replyButton;
    protected View reportButtonLine;
    protected Button reportButton;
    protected View highlightRepliesButtonLine;
    protected Button highlightRepliesButton;
    protected View highlightIdButtonLine;
    protected Button highlightIdButton;
    protected Button closeButton;

    public ThreadPostPopup(RefreshableActivity activity,
                LayoutInflater layoutInflater,
                ImageLoader imageLoader,
                DisplayImageOptions displayImageOptions,
                AbstractThreadCursorAdapter adapter)
    {

        this.activity = activity;
        this.layoutInflater = layoutInflater;
        this.imageLoader = imageLoader;
        this.displayImageOptions = displayImageOptions;
        this.adapter = adapter;

        popupView = layoutInflater.inflate(R.layout.thread_popup_layout, null);
        popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(activity.getBaseContext().getResources())); // magic so back button dismiss works
        popupWindow.setFocusable(true);

        popupScrollView = (ScrollView)popupView.findViewById(R.id.popup_full_text_scroll_view);
        deadThreadTextView = (TextView)popupView.findViewById(R.id.popup_dead_thread_text_view);
        closedThreadTextView = (TextView)popupView.findViewById(R.id.popup_closed_thread_text_view);

        spoilerTextView = (TextView)popupView.findViewById(R.id.popup_spoiler_text);
        spoilerButtonLine = (View)popupView.findViewById(R.id.popup_spoiler_button_line);
        spoilerButton = (Button)popupView.findViewById(R.id.popup_spoiler_button);
        spoilerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spoilerTextView.setVisibility(View.VISIBLE);
                spoilerButtonLine.setVisibility(View.GONE);
                spoilerButton.setVisibility(View.GONE);
            }
        });

        exifTextView = (TextView)popupView.findViewById(R.id.popup_exif_text);
        exifButtonLine = (View)popupView.findViewById(R.id.popup_exif_button_line);
        exifButton = (Button)popupView.findViewById(R.id.popup_exif_button);
        exifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exifTextView.setVisibility(View.VISIBLE);
                exifButtonLine.setVisibility(View.GONE);
                exifButton.setVisibility(View.GONE);
            }
        });

        translateButtonLine = (View)popupView.findViewById(R.id.popup_translate_button_line);
        translateButton = (Button)popupView.findViewById(R.id.popup_translate_button);
        blockButtonLine = (View)popupView.findViewById(R.id.popup_block_button_line);
        blockButton = (Button)popupView.findViewById(R.id.popup_block_button);
        deleteButtonLine = (View)popupView.findViewById(R.id.popup_delete_button_line);
        deleteButton = (Button)popupView.findViewById(R.id.popup_delete_button);
        replyButtonLine = (View)popupView.findViewById(R.id.popup_reply_button_line);
        replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
        reportButtonLine = (View)popupView.findViewById(R.id.popup_report_button_line);
        reportButton = (Button)popupView.findViewById(R.id.popup_report_button);
        highlightRepliesButtonLine = (View)popupView.findViewById(R.id.popup_highlight_replies_button_line);
        highlightRepliesButton = (Button)popupView.findViewById(R.id.popup_highlight_replies_button);
        highlightIdButtonLine = (View)popupView.findViewById(R.id.popup_highlight_id_button_line);
        highlightIdButton = (Button)popupView.findViewById(R.id.popup_highlight_id_button);
        closeButton = (Button)popupView.findViewById(R.id.popup_close_button);
    }

    public void showFromCursor(AdapterView<?> adapterView, int position) {
        showFromCursor(adapterView, null, position, 0);
    }

    public void showFromCursor(AdapterView<?> adapterView, final View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int isDeadInt = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_IS_DEAD));
        final int isClosedInt = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_CLOSED));
        final boolean isDead = isDeadInt == 0 ? false : true;
        final boolean isClosed = isClosedInt == 0 ? false : true;
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
        final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final String userId = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_USER_ID));
        final String tripcode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TRIPCODE));
        final String name = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_NAME));
        final String email = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EMAIL));
        final long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        final String spoilerText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SPOILER_TEXT));
        final String exifText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXIF_TEXT));
        final String messageText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final long clickedThreadNo = resto == 0 ? postId : resto;
        final long clickedPostNo = (resto == 0 || postId == resto) ? 0 : postId;
        if (DEBUG) Log.i(BoardActivity.TAG, "Calling popup with postId=" + postId + " isDead=" + isDead + " postNo=" + postId + " resto=" + resto);

        setSpoilerButton(spoilerText);
        setExifButton(exifText);
        setTranslateButton(messageText);
        setBlockButton(userId);
        setDeleteButton(isDead, isClosed, clickedBoardCode, clickedThreadNo, postId);
        setReportButton(isDead, isClosed, clickedBoardCode, clickedThreadNo, postId);
        setReplyButtons(isDead, isClosed, clickedBoardCode, clickedThreadNo, clickedPostNo, tim);
        displayHighlightRepliesButton(clickedBoardCode, clickedThreadNo, clickedPostNo);
        displayHighlightIdButton(clickedBoardCode, clickedThreadNo, clickedPostNo, userId, tripcode, name, email);
        setCloseButton();
        setScrollViewMargin();

        popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
    }

    protected void setSpoilerButton(String spoilerText) {
        spoilerTextView.setVisibility(View.GONE);
        if (spoilerText != null && !spoilerText.isEmpty()) {
            spoilerButtonLine.setVisibility(View.VISIBLE);
            spoilerButton.setVisibility(View.VISIBLE);
            spoilerTextView.setText(spoilerText);
        }
        else {
            spoilerButtonLine.setVisibility(View.GONE);
            spoilerButton.setVisibility(View.GONE);
            spoilerTextView.setText("");
        }
    }

    protected void setExifButton(String exifText) {
        exifTextView.setVisibility(View.GONE);
        if (exifText != null && !exifText.isEmpty()) {
            exifButtonLine.setVisibility(View.VISIBLE);
            exifButton.setVisibility(View.VISIBLE);
            exifTextView.setText(exifText);
        }
        else {
            exifButtonLine.setVisibility(View.GONE);
            exifButton.setVisibility(View.GONE);
            exifTextView.setText("");
        }
    }

    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final String STRIP_HTML_RE = "(?s)<[^>]*>(\\s*<[^>]*>)*";

    protected void setTranslateButton(String messageText) {
        if (!"".equals(messageText)) {
            final Locale locale = activity.getBaseContext().getResources().getConfiguration().locale;
            final String localeCode = locale.getLanguage();
            final String strippedText = messageText.replaceAll(STRIP_HTML_RE, " ");
            String escaped;
            try {
                escaped = URLEncoder.encode(strippedText, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding utf-8? You crazy!", e);
                escaped = strippedText;
            }
            final String escapedMessage = escaped;
            final String translateUrl = GOOGLE_TRANSLATE_ROOT + localeCode + "&text=" + escapedMessage;
            translateButtonLine.setVisibility(View.VISIBLE);
            translateButton.setVisibility(View.VISIBLE);
            translateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChanHelper.launchUrlInBrowser((ThreadActivity)activity, translateUrl); // yes cheating here
                    dismiss();
                }
            });
        }
        else {
            translateButtonLine.setVisibility(View.GONE);
            translateButton.setVisibility(View.GONE);
        }
    }
    protected void setBlockButton(final String userId) {
        if (userId != null && !userId.isEmpty()) {
            blockButtonLine.setVisibility(View.VISIBLE);
            blockButton.setVisibility(View.VISIBLE);
            blockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new BlocklistAddDialogFragment(ThreadPostPopup.this, activity, userId)).show(activity.getSupportFragmentManager(), BoardActivity.TAG);
                    dismiss();
                }
            });
        }
        else {
            blockButtonLine.setVisibility(View.GONE);
            blockButton.setVisibility(View.GONE);
        }
    }

    protected void setDeleteButton(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo,
                                   final long clickedPostNo)
    {
        if (!isDead
                && !isClosed
                && (clickedBoardCode != null && !clickedBoardCode.isEmpty())
                && clickedThreadNo != 0
                && clickedPostNo != 0)
        {
            deleteButtonLine.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new DeletePostDialogFragment(ThreadPostPopup.this, activity,
                            clickedBoardCode, clickedThreadNo, clickedPostNo))
                            .show(activity.getSupportFragmentManager(), BoardActivity.TAG);
                    dismiss();
                }
            });
        }
        else {
            deleteButtonLine.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }
    }

    protected void setReportButton(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo, final long postId) {
        if (!isDead
                && !isClosed
                && (clickedBoardCode != null && !clickedBoardCode.isEmpty())
                && clickedThreadNo != 0
                && postId != 0)
        {
            reportButtonLine.setVisibility(View.VISIBLE);
            reportButton.setVisibility(View.VISIBLE);
            reportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new ReportPostDialogFragment(ThreadPostPopup.this, activity,
                            clickedBoardCode, clickedThreadNo, postId))
                            .show(activity.getSupportFragmentManager(), BoardActivity.TAG);
                    dismiss();
                }
            });
        }
        else {
            reportButtonLine.setVisibility(View.GONE);
            reportButton.setVisibility(View.GONE);
        }
    }

    public void dismiss() {
        popupWindow.dismiss();
    }

    protected void setReplyButtons(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo, final long clickedPostNo,
                                   final long tim)
    {
        if (isDead) {
            replyButtonLine.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            deadThreadTextView.setVisibility(View.VISIBLE);
            closedThreadTextView.setVisibility(View.GONE);
        }
        else if (isClosed) {
            replyButtonLine.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            deadThreadTextView.setVisibility(View.GONE);
            closedThreadTextView.setVisibility(View.VISIBLE);
        }
        else {
            deadThreadTextView.setVisibility(View.GONE);
            closedThreadTextView.setVisibility(View.GONE);
            replyButtonLine.setVisibility(View.VISIBLE);
            replyButton.setVisibility(View.VISIBLE);
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(activity.getBaseContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.POST_NO, clickedPostNo);
                    replyIntent.putExtra(ChanHelper.TIM, tim);
                    replyIntent.putExtra(ChanHelper.TEXT, "");
                    ((Activity)activity).startActivity(replyIntent);
                    dismiss();
                }
            });
        }
    }

    protected void setCloseButton() {
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    protected void setScrollViewMargin() {
        ScrollView.LayoutParams params = (ScrollView.LayoutParams)popupScrollView.getLayoutParams();
        if (params == null)
            params = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int numVisibleButtons = 0;
        if (spoilerButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (exifButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (translateButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (blockButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (deleteButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (replyButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (highlightRepliesButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (highlightIdButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (closeButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        int bottomMarginDp = numVisibleButtons * POPUP_BUTTON_HEIGHT_DP;
        int bottomMarginPx = ChanGridSizer.dpToPx(activity.getBaseContext().getResources().getDisplayMetrics(), bottomMarginDp);
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMarginPx);
        popupScrollView.setLayoutParams(params);
    }

    protected void displayHighlightRepliesButton(final String boardCode, final long threadNo, final long postNo) { // board-level doesn't highlight, only thread-level does
        if (postNo > 0) {
            highlightRepliesButtonLine.setVisibility(View.VISIBLE);
            highlightRepliesButton.setVisibility(View.VISIBLE);
            highlightRepliesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightRepliesTask task = new HighlightRepliesTask(activity.getBaseContext(), adapter, boardCode, threadNo);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else {
            highlightRepliesButtonLine.setVisibility(View.GONE);
            highlightRepliesButton.setVisibility(View.GONE);
        }
    }

    protected void displayHighlightIdButton(final String boardCode, final long threadNo, final long postNo,
                                            final String userId, final String tripcode, final String name, final String email)
    { // board-level doesn't highlight, only thread-level does
        if (userId != null && !userId.isEmpty()) {
            highlightIdButtonLine.setVisibility(View.VISIBLE);
            highlightIdButton.setVisibility(View.VISIBLE);
            highlightIdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightIdTask task = new HighlightIdTask(activity.getBaseContext(), adapter, boardCode, threadNo, userId);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else if (tripcode != null && !tripcode.isEmpty()) {
            highlightIdButtonLine.setVisibility(View.VISIBLE);
            highlightIdButton.setVisibility(View.VISIBLE);
            highlightIdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightTripcodeTask task = new HighlightTripcodeTask(activity.getBaseContext(), adapter, boardCode, threadNo, tripcode);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("Anonymous")) {
            highlightIdButtonLine.setVisibility(View.VISIBLE);
            highlightIdButton.setVisibility(View.VISIBLE);
            highlightIdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightNameTask task = new HighlightNameTask(activity.getBaseContext(), adapter, boardCode, threadNo, name);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else if (email != null && !email.isEmpty() && !email.equalsIgnoreCase("sage")) {
            highlightIdButtonLine.setVisibility(View.VISIBLE);
            highlightIdButton.setVisibility(View.VISIBLE);
            highlightIdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightEmailTask task = new HighlightEmailTask(activity.getBaseContext(), adapter, boardCode, threadNo, email);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else {
            highlightIdButtonLine.setVisibility(View.GONE);
            highlightIdButton.setVisibility(View.GONE);
        }
    }

    private class HighlightRepliesTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private AbstractThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        public HighlightRepliesTask(Context context, AbstractThreadCursorAdapter adapter, String boardCode, long threadNo) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] prevPosts = null;
            long[] nextPosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    prevPosts = thread.getPrevPostsReferenced(postNo);
                    nextPosts = thread.getNextPostsReferredTo(postNo);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPostReplies(postNo, prevPosts, nextPosts);
            int prevCount = prevPosts == null ? 0 : prevPosts.length;
            int nextCount = nextPosts == null ? 0 : nextPosts.length;
            if (prevCount == 0 && nextCount == 0)
                result = context.getString(R.string.thread_no_replies_found);
            else if (prevCount == 0 && nextCount > 0)
                result = String.format(context.getString(R.string.thread_next_replies_found), nextCount);
            else if (prevCount > 0 && nextCount == 0)
                result = String.format(context.getString(R.string.thread_prev_replies_found), prevCount);
            else
                result = String.format(context.getString(R.string.thread_both_replies_found), prevCount, nextCount);
            if (DEBUG) Log.i(TAG, "Set highlight posts prev=" + Arrays.toString(prevPosts) + " next=" + Arrays.toString(nextPosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

    private class HighlightIdTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private AbstractThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        private String userId = null;
        public HighlightIdTask(Context context, AbstractThreadCursorAdapter adapter, String boardCode, long threadNo, String userId) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
            this.userId = userId;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] idPosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    idPosts = thread.getIdPosts(postNo, userId);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPostsWithId(postNo, idPosts);
            boolean useFriendlyIds = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext()).getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
            String formattedUserId = ChanPost.getUserId(userId, useFriendlyIds);
            if ((idPosts == null || idPosts.length == 0)) {
                result = String.format(context.getString(R.string.thread_no_id_found), formattedUserId);
            }
            else {
                result = String.format(context.getString(R.string.thread_id_found), idPosts.length, formattedUserId);
            }
            if (DEBUG) Log.i(TAG, "Set highlight posts for id=" + userId + " posts=" + Arrays.toString(idPosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

    private class HighlightTripcodeTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private AbstractThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        private String tripcode = null;
        public HighlightTripcodeTask(Context context, AbstractThreadCursorAdapter adapter, String boardCode, long threadNo, String tripcode) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
            this.tripcode = tripcode;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] tripcodePosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    tripcodePosts = thread.getTripcodePosts(postNo, tripcode);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPostsWithId(postNo, tripcodePosts);
            if ((tripcodePosts == null || tripcodePosts.length == 0)) {
                result = String.format(context.getString(R.string.thread_no_id_found), tripcode);
            }
            else {
                result = String.format(context.getString(R.string.thread_id_found), tripcodePosts.length, tripcode);
            }
            if (DEBUG) Log.i(TAG, "Set highlight posts for tripcode=" + tripcode + " posts=" + Arrays.toString(tripcodePosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

    private class HighlightNameTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private AbstractThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        private String name = null;
        public HighlightNameTask(Context context, AbstractThreadCursorAdapter adapter, String boardCode, long threadNo, String name) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
            this.name = name;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] namePosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    namePosts = thread.getNamePosts(postNo, name);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPostsWithId(postNo, namePosts);
            if ((namePosts == null || namePosts.length == 0)) {
                result = String.format(context.getString(R.string.thread_no_id_found), name);
            }
            else {
                result = String.format(context.getString(R.string.thread_id_found), namePosts.length, name);
            }
            if (DEBUG) Log.i(TAG, "Set highlight posts for name=" + name + " posts=" + Arrays.toString(namePosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

    private class HighlightEmailTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private AbstractThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        private String email = null;
        public HighlightEmailTask(Context context, AbstractThreadCursorAdapter adapter, String boardCode, long threadNo, String email) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
            this.email = email;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] emailPosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    emailPosts = thread.getEmailPosts(postNo, email);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPostsWithId(postNo, emailPosts);
            if ((emailPosts == null || emailPosts.length == 0)) {
                result = String.format(context.getString(R.string.thread_no_id_found), email);
            }
            else {
                result = String.format(context.getString(R.string.thread_id_found), emailPosts.length, email);
            }
            if (DEBUG) Log.i(TAG, "Set highlight posts for email=" + email + " posts=" + Arrays.toString(emailPosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

}
