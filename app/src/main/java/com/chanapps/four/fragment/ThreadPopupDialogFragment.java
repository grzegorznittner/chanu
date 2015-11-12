package com.chanapps.four.fragment;

import android.app.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.ThreadCursorAdapter;
import com.chanapps.four.adapter.ThreadSingleItemCursorAdapter;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.loader.ThreadCursorLoader;
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

    protected String boardCode;
    protected long threadNo;
    protected long postNo;
    protected int pos;
    protected PopupType popupType;

    protected Cursor cursor;

    protected AbstractBoardCursorAdapter adapter;
    protected AbsListView absListView;
    protected View layout;
    protected Handler handler;
    protected ThreadListener threadListener;
    protected Fragment parent;
    protected String query;

    public ThreadPopupDialogFragment() {
        super();
        if (DEBUG) Log.i(TAG, "ThreadPopupDialogFragment()");
    }

    //public ThreadPopupDialogFragment(Fragment parent, String boardCode, long threadNo, long postNo, int pos, PopupType popupType, String query) {
    public ThreadPopupDialogFragment(Fragment parent, String boardCode, long threadNo, long postNo, PopupType popupType, String query) {
        super();
        this.parent = parent;
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
        this.pos = -1;
        this.popupType = popupType;
        this.query = query;
        if (DEBUG) Log.i(TAG, "ThreadPopupDialogFragment() /" + boardCode + "/" + threadNo + "#p" + postNo + " pos=" + pos + " query=" + query);
    }

    protected void inflateLayout() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if (popupType == PopupType.SELF)
            layout = inflater.inflate(R.layout.thread_single_popup_dialog_fragment, null);
        else
            layout = inflater.inflate(R.layout.thread_popup_dialog_fragment, null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(ChanBoard.BOARD_CODE)) {
            boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
            threadNo = savedInstanceState.getLong(ChanThread.THREAD_NO);
            postNo = savedInstanceState.getLong(ChanPost.POST_NO);
            //pos = savedInstanceState.getInt(LAST_POSITION);
            popupType = PopupType.valueOf(savedInstanceState.getString(POPUP_TYPE));
            query = savedInstanceState.getString(SearchManager.QUERY);
            if (DEBUG) Log.i(TAG, "onCreateDialog() /" + boardCode + "/" + threadNo + " restored from bundle");
        }
        else {
            if (DEBUG) Log.i(TAG, "onCreateDialog() /" + boardCode + "/" + threadNo + " null bundle");
        }
        if (popupType == null)
            popupType = PopupType.SELF;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        inflateLayout();
        init();
        setStyle(STYLE_NO_TITLE, 0);
        if (DEBUG) Log.i(TAG, "creating dialog");
        Dialog dialog = builder
                .setView(layout)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    protected String popupTitle() {
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
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo);
        outState.putString(ChanBoard.BOARD_CODE, boardCode);
        outState.putLong(ChanThread.THREAD_NO, threadNo);
        outState.putLong(ChanPost.POST_NO, postNo);
        //outState.putInt(LAST_POSITION, pos);
        outState.putString(POPUP_TYPE, popupType.toString());
        outState.putString(SearchManager.QUERY, query);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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
        if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " fragment=" + fragment + " query=" + query);

        ResourceCursorAdapter fragmentAdapter;
        if (query == null || query.isEmpty()) { // load directly from fragment for empty queries
            if ((fragmentAdapter = fragment.getAdapter()) == null) {
                if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null adapter, exiting");
                dismiss();
            }
            else {
                if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo
                        + " loading empty query cursor async count=" + fragmentAdapter.getCount());
                cursor = fragmentAdapter.getCursor();
                loadCursorAsync();
            }
        }
        else { // load from callback for non-empty queries
            loadCursorFromFragmentCallback(fragment);
        }
    }

    protected static final int CURSOR_LOADER_ID = 0x19; // arbitrary
    
    protected void loadCursorFromFragmentCallback(ThreadFragment fragment) {
        if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " doing cursor loader callback");
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, loaderCallbacks);
    }

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/" + threadNo + " id=" + id);
            return new ThreadCursorLoader(parent.getActivity(), boardCode, threadNo, "", false);
        }
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/" + threadNo + " id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()) + " loader=" + loader);
            int count = data == null ? 0 : data.getCount();
            Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " callback returned " + count + " rows");
            cursor = data;
            loadCursorAsync();
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/" + threadNo + " id=" + loader.getId());
            //adapter.swapCursor(null);
            adapter.changeCursor(null);
        }
    };

    protected void loadCursorAsync() {
        if (cursor == null) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " null cursor, exiting");
            dismiss();
            return;
        }
        if (cursor.getCount() == 0) {
            if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " empty cursor, exiting");
            dismiss();
            return;
        }
        if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " fragment cursor size=" + cursor.getCount());
        new Thread(new Runnable() {
            @Override
            public void run() {
                //if (pos == 0 && postNo != threadNo && cursor.moveToFirst()) { // on multi-jump the original position is invalid, so re-scan position
                pos = -1;
                if (cursor.moveToFirst()) { // on multi-jump the original position is invalid, so re-scan position
                    while (!cursor.isAfterLast()) {
                        long id = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                        if (id == postNo) {
                            pos = cursor.getPosition();
                            break;
                        }
                        cursor.moveToNext();
                    }
                }
                if (pos == -1) {
                    Log.e(TAG, "Couldn't find post position in cursor");
                    return;
                }
                final Cursor detailCursor = detailsCursor();
                if (DEBUG) Log.i(TAG, "loadAdapter /" + boardCode + "/" + threadNo + " detail cursor size=" + detailCursor.getCount());
                if (handler != null)
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

    protected void createAdapter() {
        if (popupType == PopupType.SELF) {
            adapter = new ThreadSingleItemCursorAdapter(getActivity(), viewBinder, true, new Runnable() {
                @Override
                public void run() {
                    dismiss();
                }
            });
        }
        else {
        adapter = new ThreadCursorAdapter(getActivity(), viewBinder, true, new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
        }
    }

    protected void init() {
        createAdapter();
        absListView = (ListView) layout.findViewById(R.id.thread_popup_list_view);
        absListView.setAdapter(adapter);
        absListView.setOnItemClickListener(itemListener);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivity().getApplicationContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
        threadListener = new ThreadListener(this, ThemeSelector.instance(getActivity().getApplicationContext()).isDark());
    }

    protected AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DEBUG) Log.i(TAG, "onItemClick() pos=" + position + " postNo=" + id);
            try {
                dismiss();
            }
            catch (IllegalStateException e) {
                Log.e(TAG, "Can't dismiss previous fragment", e);
            }
            Activity activity = getActivity();
            if (activity == null || !(activity instanceof ThreadActivity)) {
                if (DEBUG) Log.i(TAG, "onItemClick() no activity");
                return;
            }
            ThreadFragment fragment = ((ThreadActivity) activity).getCurrentFragment();
            if (fragment == null) {
                if (DEBUG) Log.i(TAG, "onItemClick() no thread fragment");
                return;
            }
            if (DEBUG) Log.i(TAG, "onItemClick() scrolling to postNo=" + id);
            fragment.scrollToPostAsync(id);
        }
    };

    @Override
    public AbsListView getAbsListView() {
        return absListView;
    }

    @Override
    public ResourceCursorAdapter getAdapter() {
        Activity activity = getActivity();
        if (activity == null || !(activity instanceof ThreadActivity)) {
            if (DEBUG) Log.i(TAG, "getAdapter() no activity");
            return adapter;
        }
        ThreadFragment fragment = ((ThreadActivity) activity).getCurrentFragment();
        if (fragment == null) {
            if (DEBUG) Log.i(TAG, "getAdapter() no thread fragment");
            return adapter;
        }
        ResourceCursorAdapter fragmentAdapter = fragment.getAdapter();
        if (fragmentAdapter == null) {
            if (DEBUG) Log.i(TAG, "getAdapter() no thread fragment adapter");
            return adapter;
        }
        if (query != null && !query.isEmpty()) {
            if (DEBUG) Log.i(TAG, "getAdapter() has query so returing adpter");
            return adapter;
        }
        if (DEBUG) Log.i(TAG, "getAdapter() returning fragment adapter");
        return fragmentAdapter;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    protected Cursor detailsCursor() {
        MatrixCursor matrixCursor = ChanPost.buildMatrixCursor(0);
        if (pos == -1) {
            Log.e(TAG, "Error: invalid pos position pos=" + -1);
            return matrixCursor;
        }
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
        if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " popupType=" + popupType + " columnName=" + columnName);
        if (!cursor.moveToPosition(pos)) {
            if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " could not move to position");
            return 0;
        }
        byte[] b = cursor.getBlob(cursor.getColumnIndex(columnName));
        if (b == null || b.length == 0) {
            if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " no blob found for columnName=" + columnName);
            return 0;
        }
        HashSet<?> links = ChanPost.parseBlob(b);
        if (links == null || links.size() <= 0) {
            if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " no links found in blob");
            return 0;
        }
        int count = links.size();
        if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " found links count=" + count);
        if (!cursor.moveToFirst()) {
            if (DEBUG) Log.i(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " could not move to first");
            return 0;
        }
        while (!cursor.isAfterLast()) {
            long id = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            if (DEBUG) Log.d(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " checking pos=" + cursor.getPosition() + " id=" + id);
            if (links.contains(id)) {
                if (DEBUG) Log.d(TAG, "addBlobRows() /" + boardCode + "/" + threadNo + " pos=" + pos + " found link at pos=" + cursor.getPosition());
                Object[] row = ChanPost.extractPostRow(cursor);
                if (row != null)
                    matrixCursor.addRow(row);
            }
            if (!cursor.moveToNext())
                break;
        }
        return count;
    }

    protected void addSelfRow(MatrixCursor matrixCursor) {
        if (!cursor.moveToPosition(pos)) {
            if (DEBUG) Log.i(TAG, "addSelfRow() /" + boardCode + "/" + threadNo + " could not move to pos=" + pos);
            return;
        }
        Object[] row = ChanPost.extractPostRow(cursor);
        if (row == null) {
            if (DEBUG) Log.i(TAG, "addSelfRow() /" + boardCode + "/" + threadNo + " null row from pos=" + pos);
            return;
        }
        if (DEBUG) Log.i(TAG, "addSelfRow() /" + boardCode + "/" + threadNo + " loaded row pos=" + pos);
        matrixCursor.addRow(row);
    }

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            return ThreadViewer.setViewValue(view, cursor, boardCode,
                    false,
                    0,
                    0,
                    null, //threadListener.thumbOnClickListener,
                    threadListener.backlinkOnClickListener,
                    null,
                    null,
                    threadListener.repliesOnClickListener,
                    null, //threadListener.sameIdOnClickListener,
                    null, //threadListener.exifOnClickListener,
                    null,
                    threadListener.expandedImageListener,
                    null,
                    null
            );
        }
    };

    @Override
    public void showDialog(String boardCode, long threadNo, long postNo, int pos,
                           ThreadPopupDialogFragment.PopupType popupType) {
        Activity activity = getActivity();
        if (activity == null || !(activity instanceof ThreadActivity)) {
            if (DEBUG) Log.i(TAG, "onItemClick() no activity");
            return;
        }
        ThreadFragment fragment = ((ThreadActivity) activity).getCurrentFragment();
        if (fragment == null) {
            if (DEBUG) Log.i(TAG, "onItemClick() no thread fragment");
            return;
        }
        if (DEBUG) Log.i(TAG, "onItemClick() scrolling to postNo=" + postNo);
        dismiss();
        //(new ThreadPopupDialogFragment(fragment, boardCode, threadNo, postNo, pos, popupType, query))
        (new ThreadPopupDialogFragment(fragment, boardCode, threadNo, postNo, popupType, query))
                .show(getFragmentManager(), ThreadPopupDialogFragment.TAG);
    }

}
