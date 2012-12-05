package com.chanapps.four.data;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class ChanWatchlistCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = ChanWatchlistCursorLoader.class.getSimpleName();

    protected static final String[] columns = {
            ChanHelper.POST_ID,
            ChanHelper.POST_BOARD_NAME,
            ChanHelper.POST_IMAGE_URL,
            ChanHelper.POST_SHORT_TEXT,
            ChanHelper.POST_TEXT,
            ChanHelper.POST_TN_W,
            ChanHelper.POST_TN_H
    };

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    public ChanWatchlistCursorLoader(Context context) {
        super(context);
        this.context = context;
        mObserver = new ForceLoadContentObserver();
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        List<String> savedWatchlist = ChanWatchlist.getSortedWatchlistFromPrefs(context);
        if (savedWatchlist == null || savedWatchlist.isEmpty()) {
            return null;
        }
        Log.d(TAG, "Parsing watchlist: " + Arrays.toString(savedWatchlist.toArray()));
        MatrixCursor cursor = new MatrixCursor(columns);
        SharedPreferences prefs = context.getSharedPreferences(ChanHelper.PREF_NAME, 0);
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        for (String threadPath : savedWatchlist) {
            try {
                String[] threadComponents = ChanWatchlist.getThreadPathComponents(threadPath);
                String boardCode = threadComponents[0];
                long threadNo = Long.valueOf(threadComponents[1]);
                String shortText = hideAllText
                        ? ""
                        : (threadComponents[2].length() > 25
                            ? threadComponents[2].substring(0, 22) + "..."
                            : threadComponents[2]);
                String text = hideAllText ? "" : threadComponents[2];
                String imageUrl = threadComponents[3];
                int imageWidth = Integer.valueOf(threadComponents[4]);
                int imageHeight = Integer.valueOf(threadComponents[5]);
                MatrixCursor.RowBuilder row = cursor.newRow();
                row.add(threadNo);
                row.add(boardCode);
                row.add(imageUrl);
                row.add(shortText);
                row.add(text);
                row.add(imageWidth);
                row.add(imageHeight);
            }
            catch (Exception e) {
                Log.e(TAG, "Error parsing watch preferences ", e);
            }
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
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
