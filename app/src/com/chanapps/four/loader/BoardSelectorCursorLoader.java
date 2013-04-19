package com.chanapps.four.loader;

import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.BoardType;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanThread;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class BoardSelectorCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = BoardSelectorCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    public BoardSelectorCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        this.context = context;
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        if (DEBUG) Log.i(TAG, "loadInBackground");
        boolean showNSFWBoards = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor();
        for (BoardType boardType : BoardType.values()) {
            if (!boardType.isCategory())
                continue;
            if (!boardType.isSFW() && !showNSFWBoards)
                continue;
            Object[] row = ChanBoard.makeBoardTypeRow(context, boardType);
            matrixCursor.addRow(row);
            List<ChanBoard> boards = ChanBoard.getBoardsByType(context, boardType);
            if (boards != null && !boards.isEmpty()) {
                if (DEBUG) Log.i(TAG, "Loading " + boards.size() + " boards");
                for (ChanBoard board : boards) {
                    if (DEBUG) Log.i(TAG, "Loading board:" + board.link);
                    row = board.makeRow();
                    matrixCursor.addRow(row);
                    if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
                }
            }
            if (DEBUG) Log.i(TAG, "Loading boards complete");
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
