package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;

public class BoardSelectorCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = BoardSelectorCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;
    protected ChanBoard.Type boardType;

    public BoardSelectorCursorLoader(Context context, ChanBoard.Type boardType) {
        super(context);
        this.context = context;
        this.boardType = boardType;
        mObserver = new ForceLoadContentObserver();
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        if (BoardSelectorActivity.DEBUG) Log.d(BoardSelectorActivity.TAG, "Loading boardType=" + boardType);

        List<ChanBoard> boards = ChanBoard.getBoardsByType(context, boardType);
        if (boards == null || boards.isEmpty()) {
            Log.e(TAG, "Null board list, something went wrong for boardType=" + boardType);
            return null; // shouldn't happen
        }
        if (DEBUG) Log.i(TAG, "Creating board selector cursor for boardType=" + boardType);
        MatrixCursor cursor = new MatrixCursor(ChanHelper.SELECTOR_COLUMNS);
        for (ChanBoard board : boards) {
            MatrixCursor.RowBuilder row = cursor.newRow();
            row.add(board.link.hashCode());
            row.add(board.link);
            row.add(board.getImageResourceId());
            row.add(board.name);
        }

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
        }

        registerContentObserver(cursor, mObserver);
  		return cursor;
    }

    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
		if (DEBUG) Log.i(TAG, "deliverResult isReset(): " + isReset());
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
    	if (DEBUG) Log.i(TAG, "onStartLoading mCursor: " + mCursor);
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
    	if (DEBUG) Log.i(TAG, "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	if (DEBUG) Log.i(TAG, "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (DEBUG) Log.i(TAG, "onReset cursor: " + mCursor);
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
