package com.chanapps.four.loader;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class BoardSelectorWatchlistCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = BoardSelectorWatchlistCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    public BoardSelectorWatchlistCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        this.context = context;
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        ChanWatchlistDataLoader dataLoader = new ChanWatchlistDataLoader(context);
        List<ChanPost> threads = dataLoader.getWatchedThreads();
        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor();
        if (threads != null && !threads.isEmpty()) {
            if (DEBUG) Log.i(TAG, "Loading " + threads.size() + " watchlist threads");
            for (ChanPost thread : threads) {
                Object[] row = ChanThread.makeRow(context, thread, "");
                matrixCursor.addRow(row);
                if (DEBUG) Log.i(TAG, "Added thread: " + thread.board + "/" + thread.no);
                if (DEBUG) Log.v(TAG, "Added thread row: " + Arrays.toString(row));
            }
            if (DEBUG) Log.i(TAG, "Loading watchlist threads complete");
        }
        registerContentObserver(matrixCursor, mObserver);
        return matrixCursor;
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
