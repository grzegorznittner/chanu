package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.viewer.ThreadListener;
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
public class ThreadPopupDialogFragment extends DialogFragment implements ThreadViewable
{
    public static final String TAG = ThreadPopupDialogFragment.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static final String LAST_POSITION = "lastPosition";
    public static final String POPUP_TYPE = "popupType";

    static public enum PopupType {
        SELF,
        BACKLINKS,
        REPLIES,
        SAME_ID
    }

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
    private ThreadListener threadListener;

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
        if (savedInstanceState != null && savedInstanceState.containsKey(ChanBoard.BOARD_CODE)) {
            boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
            threadNo = savedInstanceState.getLong(ChanThread.THREAD_NO);
            postNo = savedInstanceState.getLong(ChanPost.POST_NO);
            pos = savedInstanceState.getInt(LAST_POSITION);
            popupType = PopupType.valueOf(savedInstanceState.getString(POPUP_TYPE));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layout = inflater.inflate(R.layout.thread_popup_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.thread_popup_dialog_title);
        title.setText(popupTitle());
        init();
        setStyle(STYLE_NO_TITLE, 0);
        if (DEBUG) Log.i(TAG, "creating dialog");
        Dialog dialog = builder
                .setView(layout)
                //.setPositiveButton(R.string.thread_popup_reply, postReplyListener)
                //.setNeutralButton(R.string.thread_popup_goto, null)
                .setNegativeButton(R.string.dialog_close, dismissListener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private String popupTitle() {
        switch (popupType) {
            case BACKLINKS:
                return getString(R.string.thread_backlinks);
            case REPLIES:
                return getString(R.string.thread_replies);
            case SAME_ID:
                return getString(R.string.thread_same_id);
            default:
            case SELF:
                return getString(R.string.thread_post);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ChanBoard.BOARD_CODE, boardCode);
        outState.putLong(ChanThread.THREAD_NO, threadNo);
        outState.putLong(ChanPost.POST_NO, postNo);
        outState.putInt(LAST_POSITION, pos);
        outState.putString(POPUP_TYPE, popupType.toString());
    }

    protected DialogInterface.OnClickListener postReplyListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String replyText = ">>" + postNo + "\n";
            PostReplyActivity.startActivity(getActivity(), boardCode, threadNo, 0, ChanPost.planifyText(replyText));
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
        ThreadActivity activity = (ThreadActivity)getActivity();
        if (activity == null) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null activity, exiting");
            dismiss();
            return;
        }
        ThreadFragment fragment = activity.getCurrentFragment();
        if (fragment == null) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null fragment, exiting");
            dismiss();
            return;
        }
        if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " fragment=" + fragment);
        if (fragment.getAdapter() == null) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null adapter, exiting");
            dismiss();
            return;
        }
        if ((cursor = fragment.getAdapter().getCursor()) == null) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null cursor, exiting");
            dismiss();
            return;
        }
        if (cursor.getCount() == 0) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " empty cursor, exiting");
            dismiss();
            return;
        }
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
        adapter = new ThreadListCursorAdapter(getActivity(), viewBinder, false);
        absListView = (ListView) layout.findViewById(R.id.thread_popup_list_view);
        absListView.setAdapter(adapter);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivity().getApplicationContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        threadListener = new ThreadListener(this, ThemeSelector.instance(getActivity().getApplicationContext()).isDark());
    }

    @Override
    public AbsListView getAbsListView() {
        return absListView;
    }

    @Override
    public ResourceCursorAdapter getAdapter() {
        return adapter;
    }

    @Override
    public Handler getHandler() {
        return handler;
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
            case SAME_ID:
                addBlobRows(matrixCursor, ChanPost.POST_SAME_IDS_BLOB);
                break;
            case SELF:
                addSelfRow(matrixCursor);
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

    protected void addSelfRow(MatrixCursor matrixCursor) {
        if (cursor.moveToPosition(pos))
            matrixCursor.addRow(ChanPost.extractPostRow(cursor));
    }

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            return ThreadViewer.setViewValue(view, cursor, boardCode,
                    false,
                    0,
                    0,
                    threadListener.imageOnClickListener,
                    null, //threadListener.backlinkOnClickListener,
                    null,
                    null, //threadListener.repliesOnClickListener,
                    null, //threadListener.sameIdOnClickListener,
                    threadListener.exifOnClickListener,
                    //null,
                    null,
                    null,
                    null,
                    null);
        }
    };

    @Override
    public void showDialog(String boardCode, long threadNo, long postNo, int pos,
                           ThreadPopupDialogFragment.PopupType popupType) {
        throw new UnsupportedOperationException("showDialog not supported from ThreadPopupDialogFragment");
        //(new ThreadPopupDialogFragment(boardCode, threadNo, postNo, pos, popupType))
        //        .show(getFragmentManager(), ThreadPopupDialogFragment.TAG);
    }

}
