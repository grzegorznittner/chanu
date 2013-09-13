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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.adapter.ThreadSingleItemListCursorAdapter;
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
public class ThreadSinglePopupDialogFragment extends DialogFragment implements ThreadViewable
{
    public static final String TAG = ThreadSinglePopupDialogFragment.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static final String LAST_POSITION = "lastPosition";

    private String boardCode;
    private long threadNo;
    private long postNo;
    private int pos;

    private Cursor cursor;

    private AbstractBoardCursorAdapter adapter;
    private AbsListView absListView;
    private ViewGroup layout;
    private Handler handler;
    private ThreadListener threadListener;

    public ThreadSinglePopupDialogFragment() {
        super();
    }

    public ThreadSinglePopupDialogFragment(String boardCode, long threadNo, long postNo, int pos) {
        super();
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
        this.pos = pos;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(ChanBoard.BOARD_CODE)) {
            boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
            threadNo = savedInstanceState.getLong(ChanThread.THREAD_NO);
            postNo = savedInstanceState.getLong(ChanPost.POST_NO);
            pos = savedInstanceState.getInt(LAST_POSITION);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layout = (ViewGroup)inflater.inflate(R.layout.thread_single_popup_dialog_fragment, null);
        init();
        setStyle(STYLE_NO_TITLE, 0);
        if (DEBUG) Log.i(TAG, "creating dialog");
        Dialog dialog = builder
                .setView(layout)
                //.setPositiveButton(R.string.thread_popup_reply, postReplyListener)
                //.setNeutralButton(R.string.thread_popup_goto, null)
                //.setNegativeButton(R.string.dialog_close, dismissListener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ChanBoard.BOARD_CODE, boardCode);
        outState.putLong(ChanThread.THREAD_NO, threadNo);
        outState.putLong(ChanPost.POST_NO, postNo);
        outState.putInt(LAST_POSITION, pos);
    }

    protected DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            ThreadSinglePopupDialogFragment.this.dismiss();
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
                if (handler != null)
                    handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter != null && detailCursor != null)
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
        adapter = new ThreadSingleItemListCursorAdapter(getActivity(), viewBinder, false);
        absListView = (ListView) layout.findViewById(R.id.thread_popup_list_view);
        absListView.setAdapter(adapter);
        absListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismiss();
            }
        });
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
        addSelfRow(matrixCursor);
        return matrixCursor;
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
                    null, //threadListener.imageOnClickListener,
                    null, //threadListener.backlinkOnClickListener,
                    null,
                    null, //threadListener.repliesOnClickListener,
                    null, //threadListener.sameIdOnClickListener,
                    null, //threadListener.exifOnClickListener,
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
