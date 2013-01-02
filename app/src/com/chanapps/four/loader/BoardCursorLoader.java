package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;
import com.chanapps.four.service.BoardLoadService;

public class BoardCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = BoardCursorLoader.class.getSimpleName();

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    protected String boardName;
    private int pageNo;

    protected BoardCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public BoardCursorLoader(Context context, String boardName, int pageNo) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.pageNo = pageNo;
    }

    public BoardCursorLoader(Context context, String boardName) {
        this(context, boardName, 0);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG, "loadInBackground");
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
        if (board == null) { // this shouldn't happen, so reload
            Log.i(TAG, "Reloading board " + boardName + " - starting BoardLoadService");
            Intent threadIntent = new Intent(context, BoardLoadService.class);
            threadIntent.putExtra(ChanHelper.BOARD_CODE, boardName);
            //threadIntent.putExtra(ChanHelper.RETRIES, ++retries);
            context.startService(threadIntent);
        }
        else {
            MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
            for (ChanPost thread : board.threads) {
                if (thread.tn_w <= 0 || thread.tim == 0) {
                    Log.e(TAG, "Board thread without image, should never happen, board=" + boardName + " threadNo=" + thread.no);
                    matrixCursor.addRow(new Object[] {
                            thread.no, boardName, 0, "",
                            thread.getCountryFlagUrl(),
                            thread.getBoardThreadText(), thread.getHeaderText(), thread.getFullText(),
                            thread.tn_w, thread.tn_h, thread.w, thread.h, thread.tim, thread.isDead ? 1 : 0, 0, 0});

                } else {
                    matrixCursor.addRow(new Object[] {
                            thread.no, boardName, 0,
                            thread.getThumbnailUrl(), thread.getCountryFlagUrl(),
                            thread.getBoardThreadText(), thread.getHeaderText(), thread.getFullText(),
                            thread.tn_w, thread.tn_h, thread.w, thread.h, thread.tim, thread.isDead ? 1 : 0, 0, 0});
                }
            }
            if (board.lastPage) {
                matrixCursor.addRow(new Object[] {
                        2, boardName, 0,
                        "", "",
                        "", "", "",
                        -1, -1, -1, -1, 0, 1, 0, 1});
            }
            else {
                matrixCursor.addRow(new Object[] {
                        2, boardName, 0,
                        "", "",
                        "", "", "",
                        -1, -1, -1, -1, 0, 1, 1, 0});
                registerContentObserver(matrixCursor, mObserver);
            }
            return matrixCursor;
        }
        return null;
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
		Log.i(TAG, "deliverResult isReset(): " + isReset());
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
    	Log.i(TAG, "onStartLoading mCursor: " + mCursor);
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
    	Log.i(TAG, "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	Log.i(TAG, "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        Log.i(TAG, "onReset cursor: " + mCursor);
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
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
