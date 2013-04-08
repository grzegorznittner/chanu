package com.chanapps.four.component;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.adapter.AbstractThreadCursorAdapter;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.BlocklistAddDialogFragment;
import com.chanapps.four.fragment.DeletePostDialogFragment;
import com.chanapps.four.fragment.ReportPostDialogFragment;
import com.chanapps.four.task.HighlightRepliesTask;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.UnsupportedEncodingException;
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

    protected RefreshableActivity activity;
    protected LayoutInflater layoutInflater;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected View firstVisibleLineView;
    protected String spoilerText;
    protected String exifText;
    
    protected View popupView;
    protected PopupWindow popupWindow;
    protected TextView popupHeaderText;
    protected View spoilerButtonLine;
    protected Button spoilerButton;     // todo
    protected View exifButtonLine;
    protected Button exifButton;
    protected View copyButtonLine;
    protected Button copyButton;
    protected View translateButtonLine;  // todo
    protected Button translateButton;
    protected View blockButtonLine;      // todo
    protected Button blockButton;
    protected View deleteButtonLine;     // todo
    protected Button deleteButton;
    protected View replyButtonLine;
    protected Button replyButton;
    protected View reportButtonLine;     // todo
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
        popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(activity.getBaseContext().getResources())); // magic so back button dismiss works
        popupWindow.setFocusable(true);

        popupHeaderText = (TextView)popupView.findViewById(R.id.popup_header_text);
        createSpoiler();
        createExif();
        copyButtonLine = popupView.findViewById(R.id.popup_copy_button_line);
        copyButton = (Button)popupView.findViewById(R.id.popup_copy_button);
        translateButtonLine = popupView.findViewById(R.id.popup_translate_button_line);
        translateButton = (Button)popupView.findViewById(R.id.popup_translate_button);
        blockButtonLine = popupView.findViewById(R.id.popup_block_button_line);
        blockButton = (Button)popupView.findViewById(R.id.popup_block_button);
        deleteButtonLine = popupView.findViewById(R.id.popup_delete_button_line);
        deleteButton = (Button)popupView.findViewById(R.id.popup_delete_button);
        replyButtonLine = popupView.findViewById(R.id.popup_reply_button_line);
        replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
        reportButtonLine = popupView.findViewById(R.id.popup_report_button_line);
        reportButton = (Button)popupView.findViewById(R.id.popup_report_button);
        highlightRepliesButtonLine = popupView.findViewById(R.id.popup_highlight_replies_button_line);
        highlightRepliesButton = (Button)popupView.findViewById(R.id.popup_highlight_replies_button);
        highlightIdButtonLine = popupView.findViewById(R.id.popup_highlight_id_button_line);
        highlightIdButton = (Button)popupView.findViewById(R.id.popup_highlight_id_button);
        closeButton = (Button)popupView.findViewById(R.id.popup_close_button);
    }

    private void createSpoiler() {
        spoilerButtonLine = popupView.findViewById(R.id.popup_spoiler_button_line);
        spoilerButton = (Button)popupView.findViewById(R.id.popup_spoiler_button);
        spoilerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SpoilerDialogFragment().show(activity.getSupportFragmentManager(), TAG);
            }
        });
    }

    private class SpoilerDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return createDisplayDialog((Activity)activity, getString(R.string.spoiler_title), spoilerText);
        }
    }

    private void createExif() {
        exifButtonLine = popupView.findViewById(R.id.popup_exif_button_line);
        exifButton = (Button)popupView.findViewById(R.id.popup_exif_button);
        exifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ExifDialogFragment().show(activity.getSupportFragmentManager(), TAG);
            }
        });
    }

    private class ExifDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return createDisplayDialog((Activity)activity, getString(R.string.exif_title), exifText);
        }
    }

    private Dialog createDisplayDialog(Activity activity, String title, String text) {
        dismiss();
        TextView tv = new TextView(activity);
        tv.setText(Html.fromHtml(text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        int padding = ChanGridSizer.dpToPx(activity.getResources().getDisplayMetrics(), 16);
        tv.setPadding(padding, padding, padding, padding);
        return new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(tv)
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //ignore
                    }
                })
                .create();
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
        final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_CODE));
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

        firstVisibleLineView = null;
        setPostHeader(postId);
        setSpoilerButton(spoilerText);
        setExifButton(exifText);
        setCopyButton(messageText);
        setTranslateButton(messageText);
        setBlockButton(userId);
        setDeleteButton(isDead, isClosed, clickedBoardCode, clickedThreadNo, postId);
        setReportButton(isDead, isClosed, clickedBoardCode, clickedThreadNo, postId);
        setReplyButtons(isDead, isClosed, clickedBoardCode, clickedThreadNo, clickedPostNo, tim);
        displayHighlightRepliesButton(clickedBoardCode, clickedThreadNo, clickedPostNo);
        displayHighlightIdButton(clickedBoardCode, clickedThreadNo, clickedPostNo, userId, tripcode, name, email);
        setCloseButton();
        if (firstVisibleLineView != null)
            firstVisibleLineView.setVisibility(View.INVISIBLE);

        popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
    }

    protected void setPostHeader(long postId) {
        String headerText = String.format(activity.getBaseContext().getString(R.string.popup_header_text_format), postId);
        popupHeaderText.setText(headerText);
    }

    protected void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected void setVisibility(boolean visible, View... views) {
        for (View view : views) {
            if (visible && firstVisibleLineView == null)
                firstVisibleLineView = view;
            setVisibility(view, visible);

        }
    }

    protected void setSpoilerButton(String spoilerText) {
        this.spoilerText = spoilerText;
        setVisibility(spoilerText != null && !spoilerText.isEmpty(), spoilerButtonLine, spoilerButton);
    }

    protected void setExifButton(String exifText) {
        this.exifText = exifText;
        setVisibility(exifText != null && !exifText.isEmpty(), exifButtonLine, exifButton);
    }

    protected void setCopyButton(final String messageText) {
        boolean visible = !"".equals(messageText);
        if (visible) {
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            activity.getBaseContext().getString(R.string.app_name),
                            messageText.replaceAll("</?br>", "").replaceAll("<[^>]*>.*",""));
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(activity.getBaseContext(), R.string.copy_text_complete, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
        setVisibility(visible, copyButtonLine, copyButton);
    }

    public static final String GOOGLE_TRANSLATE_ROOT = "http://translate.google.com/translate_t?langpair=auto|";
    public static final String STRIP_HTML_RE = "(?s)<[^>]*>(\\s*<[^>]*>)*";

    protected void setTranslateButton(String messageText) {
        boolean visible = !"".equals(messageText);
        if (visible) {
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
            translateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChanHelper.launchUrlInBrowser((ThreadActivity)activity, translateUrl); // yes cheating here
                    dismiss();
                }
            });
        }
        setVisibility(visible, translateButtonLine, translateButton);
    }

    protected void setBlockButton(final String userId) {
        boolean visible = userId != null && !userId.isEmpty();
        if (visible) {
            blockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new BlocklistAddDialogFragment(ThreadPostPopup.this, activity, userId)).show(activity.getSupportFragmentManager(), BoardActivity.TAG);
                    dismiss();
                }
            });
        }
        setVisibility(visible, blockButtonLine, blockButton);
    }

    protected void setDeleteButton(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo,
                                   final long clickedPostNo)
    {
        boolean visible = !isDead
                && !isClosed
                && (clickedBoardCode != null && !clickedBoardCode.isEmpty())
                && clickedThreadNo != 0
                && clickedPostNo != 0;
        if (visible) {
            final long[] postNos = { clickedPostNo };
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        setVisibility(visible, deleteButtonLine, deleteButton);
    }

    protected void setReportButton(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo, final long postId) {
        boolean visible = !isDead
                && !isClosed
                && (clickedBoardCode != null && !clickedBoardCode.isEmpty())
                && clickedThreadNo != 0
                && postId != 0;
        if (visible) {
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
        setVisibility(visible, reportButtonLine, reportButton);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }

    protected void setReplyButtons(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo, final long clickedPostNo,
                                   final long tim)
    {
        boolean visible = !isDead && !isClosed;
        if (visible) {
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
        setVisibility(visible, replyButtonLine, replyButton);
    }

    protected void setCloseButton() {
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    protected void displayHighlightRepliesButton(final String boardCode, final long threadNo, final long postNo) { // board-level doesn't highlight, only thread-level does
        boolean visible = postNo > 0;
        if (visible) {
            highlightRepliesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
        }
        setVisibility(visible, highlightRepliesButtonLine, highlightRepliesButton);
    }

    protected void displayHighlightIdButton(final String boardCode, final long threadNo, final long postNo,
                                            final String userId, final String tripcode, final String name, final String email)
    { // board-level doesn't highlight, only thread-level does
        boolean visible = false;
        if (userId != null && !userId.isEmpty()) {
            visible = true;
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
            visible = true;
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
            visible = true;
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
            visible = true;
            highlightIdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlightEmailTask task = new HighlightEmailTask(activity.getBaseContext(), adapter, boardCode, threadNo, email);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        setVisibility(visible, highlightIdButtonLine, highlightIdButton);
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
            //threadAdapter.setHighlightPostsWithId(postNo, idPosts);
            boolean useFriendlyIds = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext()).getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
            String formattedUserId = ChanPost.formattedUserId(userId, useFriendlyIds);
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
            //threadAdapter.setHighlightPostsWithId(postNo, tripcodePosts);
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
            //threadAdapter.setHighlightPostsWithId(postNo, namePosts);
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
            //threadAdapter.setHighlightPostsWithId(postNo, emailPosts);
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
