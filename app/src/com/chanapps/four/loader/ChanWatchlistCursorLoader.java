package com.chanapps.four.loader;

import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;

import javax.security.auth.login.LoginException;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class ChanWatchlistCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = ChanWatchlistCursorLoader.class.getSimpleName();

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
        MatrixCursor cursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
        for (String threadPath : savedWatchlist) {
            Log.d(TAG, "Parsing threadpath: " + threadPath);

            try {
                String[] threadComponents = ChanWatchlist.getThreadPathComponents(threadPath);
                long tim = Long.valueOf(threadComponents[0]);
                String boardCode = threadComponents[1];
                long threadNo = Long.valueOf(threadComponents[2]);
                String shortText;
                String text;
                String imageUrl;
                int imageWidth;
                int imageHeight;
                int isDead;
                ChanThread thread = null;
                ChanPost threadPost = null;
                try {
                    Log.i(TAG, "trying to load thread " + boardCode + "/" + threadNo + " from storage");
                    thread = ChanFileStorage.loadThreadData(getContext(), boardCode, threadNo);
                    if (thread != null) {
                        threadPost = thread.posts[0];
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Error loading thread data from storage", e);
                }
                if (threadPost != null) { // pull from cache, it will have the latest data
                    Log.i(TAG, "Found cached watchlist thread " + boardCode + "/" + threadNo + ", updating from cache");
                    threadPost.isDead = thread.isDead;
                    shortText = threadPost.getThreadText();
                    text = threadPost.getFullText();
                    imageUrl = threadPost.getThumbnailUrl();
                    imageWidth = threadPost.tn_w;
                    imageHeight = threadPost.tn_h;
                    isDead = thread.isDead ? 1 : 0;
                }
                else { // thread not in cache, pull last stored from watchlist prefs
                    Log.i(TAG, "Didn't find cached watchlist thread " + boardCode + "/" + threadNo + ", loading from prefs");
                    shortText = (threadComponents[3].length() > ChanPost.MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN
                            ? threadComponents[3].substring(0, ChanPost.MAX_BOARDTHREAD_IMAGETEXT_LEN) + "..."
                            : threadComponents[3]);
                    text = threadComponents[3];
                    imageUrl = threadComponents[4];
                    imageWidth = Integer.valueOf(threadComponents[5]);
                    imageHeight = Integer.valueOf(threadComponents[6]);
                    isDead = 0; // we don't know if it's dead or not, assume alive
                }
                MatrixCursor.RowBuilder row = cursor.newRow();
                row.add(threadNo);
                row.add(boardCode);
                row.add(0);
                row.add(imageUrl);
                row.add(shortText);
                row.add(text);
                row.add(imageWidth);
                row.add(imageHeight);
                row.add(imageWidth);
                row.add(imageHeight);
                row.add(tim);
                row.add(isDead);
                row.add(0);
                row.add(0);
                Log.i(TAG, "Thread dead status for " + boardCode + "/" + threadNo + " is " + isDead);
                Log.d(TAG, "Watchlist cursor has: " + threadNo + " " + boardCode + " " + imageUrl + " " + shortText);
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
