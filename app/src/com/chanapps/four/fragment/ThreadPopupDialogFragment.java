package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import java.util.HashSet;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class ThreadPopupDialogFragment
        extends DialogFragment
        implements AbstractBoardCursorAdapter.ViewBinder,
        AdapterView.OnItemClickListener
{

    static public enum PopupType {
        BACKLINKS,
        REPLIES,
        SAME_ID
    }

    public static final String TAG = ThreadPopupDialogFragment.class.getSimpleName();

    private String boardCode;
    private long threadNo;
    private long postNo;
    private int pos;
    private PopupType popupType;

    private Cursor cursor;

    private AbstractBoardCursorAdapter adapter;
    private AbsListView absListView;
    private View layout;
    private Handler handler;

    public ThreadPopupDialogFragment() {
        super();
    }

    public ThreadPopupDialogFragment(String boardCode, long threadNo, long postNo, int pos, PopupType popupType) {
        super();
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
        this.pos = pos;
        this.popupType = popupType;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(ChanHelper.BOARD_CODE)) {
            boardCode = savedInstanceState.getString(ChanHelper.BOARD_CODE);
            threadNo = savedInstanceState.getLong(ChanHelper.THREAD_NO);
            postNo = savedInstanceState.getLong(ChanHelper.POST_NO);
            pos = savedInstanceState.getInt(ChanHelper.LAST_THREAD_POSITION);
            popupType = PopupType.valueOf(savedInstanceState.getString(ChanHelper.POPUP_TYPE));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layout = inflater.inflate(R.layout.thread_popup_dialog_fragment, null);
        init();
        return builder
                .setView(layout)
                .setTitle(popupTitle())
                .setPositiveButton(R.string.thread_popup_reply, postReplyListener)
                .setNegativeButton(R.string.dialog_close, dismissListener)
                .create();
    }

    private String popupTitle() {
        switch (popupType) {
            case BACKLINKS:
                return getString(R.string.thread_backlinks);
            case REPLIES:
                return getString(R.string.thread_replies);
            default:
            case SAME_ID:
                return getString(R.string.thread_same_id);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ChanHelper.BOARD_CODE, boardCode);
        outState.putLong(ChanHelper.THREAD_NO, threadNo);
        outState.putLong(ChanHelper.POST_NO, postNo);
        outState.putInt(ChanHelper.LAST_THREAD_POSITION, pos);
        outState.putString(ChanHelper.POPUP_TYPE, popupType.toString());
    }

    protected DialogInterface.OnClickListener postReplyListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String replyText = ">>" + postNo + "\n";
            Intent replyIntent = new Intent(getActivity().getApplicationContext(), PostReplyActivity.class);
            replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            replyIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            replyIntent.putExtra(ChanPost.POST_NO, 0);
            replyIntent.putExtra(ChanHelper.TEXT, ChanPost.planifyText(replyText));
            startActivity(replyIntent);
        }
    };

    protected DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            ThreadPopupDialogFragment.this.dismiss();
        }
    };

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
    }

    @Override
    public void onStart() {
        super.onStart();
        if (handler == null)
            handler = new Handler();
        loadAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler == null)
            handler = new Handler();
    }

    protected void loadAdapter() {
        ThreadActivity threadActivity = (ThreadActivity)getActivity();
        cursor = threadActivity.getCursor();
        if (cursor == null || cursor.getCount() <= 0)
            dismiss();
        else
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Cursor detailCursor = detailsCursor();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.swapCursor(detailCursor);
                        }
                    });
                }
            }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        handler = null;
    }

    protected void init() {
        adapter = new ThreadListCursorAdapter(getActivity().getApplicationContext(), this);
        absListView = (ListView) layout.findViewById(R.id.thread_list_view);
        absListView.setAdapter(adapter);
        absListView.setOnItemClickListener(this);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivity().getApplicationContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected Cursor detailsCursor() {
        MatrixCursor matrixCursor = ChanPost.buildMatrixCursor();
        switch (popupType) {
            case BACKLINKS:
                addBlobRows(matrixCursor, ChanPost.POST_BACKLINKS_BLOB);
                break;
            case REPLIES:
                addBlobRows(matrixCursor, ChanPost.POST_REPLIES_BLOB);
                break;
            default:
            case SAME_ID:
                addBlobRows(matrixCursor, ChanPost.POST_SAME_IDS_BLOB);
                break;
        }
        return matrixCursor;
    }

    protected int addBlobRows(MatrixCursor matrixCursor, String columnName) {
        cursor.moveToPosition(pos);
        byte[] b = cursor.getBlob(cursor.getColumnIndex(columnName));
        if (b == null || b.length == 0)
            return 0;
        HashSet<?> links = ChanPost.parseBlob(b);
        Log.e(TAG, "Exception blob size=" + links.size());
        if (links == null || links.size() <= 0)
            return 0;
        int count = links.size();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (links.contains(cursor.getLong(0)))
                matrixCursor.addRow(ChanPost.extractPostRow(cursor));
            cursor.moveToNext();
        }
        return count;
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        return ThreadViewer.setViewValue(view, cursor, boardCode, null, null, null, null, null, null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        if ((flags & ChanPost.FLAG_IS_URLLINK) > 0) {
            String url = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SUBJECT_TEXT));
            if (url != null && !url.isEmpty())
                ChanHelper.launchUrlInBrowser(getActivity(), url);
        }
    }
}
