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

    public static final String TAG = ThreadPopupDialogFragment.class.getSimpleName();

    private String boardCode;
    private long threadNo;
    private long postNo;
    private int pos;
    private Cursor cursor;

    private AbstractBoardCursorAdapter adapter;
    private AbsListView absListView;
    private View layout;
    private Handler handler;

    public ThreadPopupDialogFragment() {
        super();
    }

    public ThreadPopupDialogFragment(String boardCode, long threadNo, long postNo, int pos) {
        super();
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
        this.pos = pos;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(ChanHelper.BOARD_CODE)) {
            boardCode = savedInstanceState.getString(ChanHelper.BOARD_CODE);
            threadNo = savedInstanceState.getLong(ChanHelper.THREAD_NO);
            postNo = savedInstanceState.getLong(ChanHelper.POST_NO);
            pos = savedInstanceState.getInt(ChanHelper.LAST_THREAD_POSITION);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layout = inflater.inflate(R.layout.thread_popup_dialog_fragment, null);
        String title = String.format(getString(R.string.thread_popup_title), postNo);
        init();
        return builder
                .setView(layout)
                .setTitle(title)
                .setPositiveButton(R.string.thread_popup_reply, postReplyListener)
                .setNegativeButton(R.string.dialog_close, dismissListener)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ChanHelper.BOARD_CODE, boardCode);
        outState.putLong(ChanHelper.THREAD_NO, threadNo);
        outState.putLong(ChanHelper.POST_NO, postNo);
        outState.putInt(ChanHelper.LAST_THREAD_POSITION, pos);
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
        int count = addBlobRows(matrixCursor, ChanPost.POST_BACKLINKS_BLOB, R.plurals.thread_num_backlinks);
        addPostRow(matrixCursor, count > 0);
        addBlobRows(matrixCursor, ChanPost.POST_REPLIES_BLOB, R.plurals.thread_num_replies);
        addLinksRows(matrixCursor);
        return matrixCursor;
    }

    protected void addPostRow(MatrixCursor matrixCursor, boolean showTitle) {
        if (showTitle)
            matrixCursor.addRow(ChanPost.makeTitleRow(boardCode,
                    getResources().getString(R.string.thread_post_title).toUpperCase()));
        cursor.moveToPosition(pos);
        matrixCursor.addRow(ChanPost.extractPostRow(cursor));
    }

    protected int addBlobRows(MatrixCursor matrixCursor, String columnName, int pluralTitleStringId) {
        cursor.moveToPosition(pos);
        byte[] b = cursor.getBlob(cursor.getColumnIndex(columnName));
        if (b == null || b.length == 0)
            return 0;
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0)
            return 0;
        int count = links.size();
        String title = String.format(getResources().getQuantityString(pluralTitleStringId, count), count);
        matrixCursor.addRow(ChanPost.makeTitleRow(boardCode, title.toUpperCase()));
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (links.contains(cursor.getLong(0)))
                matrixCursor.addRow(ChanPost.extractPostRow(cursor));
            cursor.moveToNext();
        }
        return count;
    }

    protected void addLinksRows(MatrixCursor matrixCursor) {
        cursor.moveToPosition(pos);
        byte[] b = cursor.getBlob(cursor.getColumnIndex(ChanPost.POST_LINKED_URLS_BLOB));
        if (b == null || b.length == 0)
            return;
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0)
            return;
        int count = links.size();
        String title = String.format(getResources().getQuantityString(R.plurals.thread_num_links, count), count);
        matrixCursor.addRow(ChanPost.makeTitleRow(boardCode, title.toUpperCase()));
        for (Object link : links)
            matrixCursor.addRow(ChanPost.makeUrlLinkRow(boardCode, link.toString()));
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        return ThreadViewer.setViewValue(view, cursor, boardCode, null, null);
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
