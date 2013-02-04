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
    private static final boolean DEBUG = false;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    public ChanWatchlistCursorLoader(Context context) {
        super(context);
        this.context = context;
        mObserver = new ForceLoadContentObserver();
    }

    private boolean hideAllText;
    private boolean hidePostNumbers;
    private boolean useFriendlyIds;

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        List<String> savedWatchlist = ChanWatchlist.getSortedWatchlistFromPrefs(context);
//        if (savedWatchlist == null || savedWatchlist.isEmpty()) {
//            return null;
//        }
        if (DEBUG) Log.i(TAG, "Parsing watchlist: " + Arrays.toString(savedWatchlist.toArray()));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        MatrixCursor cursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
        for (String threadPath : savedWatchlist) {
            if (DEBUG) Log.i(TAG, "Parsing threadpath: " + threadPath);

            try {
                String[] threadComponents = ChanWatchlist.getThreadPathComponents(threadPath);
                String boardCode = threadComponents[1];
                long threadNo = Long.valueOf(threadComponents[2]);
                ChanThread thread = null;
                ChanPost threadPost = null;
                try {
                    if (DEBUG) Log.i(TAG, "trying to load thread " + boardCode + "/" + threadNo + " from storage");
                    thread = ChanFileStorage.loadThreadData(getContext(), boardCode, threadNo);
                    if (thread != null) {
                        threadPost = thread.posts[0];
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Error loading thread data from storage", e);
                }

                if (thread != null && thread.defData) {
                    if (DEBUG) Log.i(TAG, "Found defdata on thread, skipping");
                }
                else if (threadPost != null && threadPost.defData) {
                    if (DEBUG) Log.i(TAG, "Found defdata on thread post, skipping");
                }
                else if (threadPost != null) { // pull from cache, it will have the latest data
                    if (DEBUG) Log.i(TAG, "Found cached watchlist thread " + boardCode + "/" + threadNo + ", updating from cache");
                    threadPost.isDead = thread.isDead;
                    threadPost.hideAllText = hideAllText;
                    threadPost.hidePostNumbers = hidePostNumbers;
                    threadPost.useFriendlyIds = useFriendlyIds;
                    addRowFromThreadData(cursor, threadPost);
                }
                else { // thread not in cache, pull last stored from watchlist prefs
                    if (DEBUG) Log.i(TAG, "Didn't find cached watchlist thread " + boardCode + "/" + threadNo + ", loading from prefs");
                    addRowFromCachedPrefs(cursor, threadComponents);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error parsing watch preferences ", e);
            }
        }
        if (DEBUG) Log.i(TAG, "Adding final row marker");
        addFinalRow(cursor);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
        }
        registerContentObserver(cursor, mObserver);
  		return cursor;
    }

    private void addRowFromThreadData(MatrixCursor cursor, ChanPost threadPost) {
        String boardCode = threadPost.board;
        long threadNo = threadPost.no;
        long tim = threadPost.tim;
        String shortText = threadPost.getBoardThreadText();
        String headerText = threadPost.getHeaderText();
        String text = threadPost.getFullText();
        String imageUrl = threadPost.getThumbnailUrl();
        String countryUrl = threadPost.getCountryFlagUrl();
        int imageWidth = threadPost.tn_w;
        int imageHeight = threadPost.tn_h;
        int fullImageWidth = threadPost.w;
        int fullImageHeight = threadPost.h;
        int spoiler = threadPost.spoiler;
        String spoilerText = threadPost.getSpoilerText();
        String exifText = threadPost.getExifText();
        int isDead = threadPost.isDead ? 1 : 0;
        int closed = threadPost.closed;
        ChanWatchlist.updateThreadInfo(context, boardCode, threadNo, tim,
                text, imageUrl, imageWidth, imageHeight, threadPost.isDead);
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(threadNo);
        row.add(boardCode);
        row.add(0);
        row.add(imageUrl);
        row.add(countryUrl);
        row.add(shortText);
        row.add(headerText);
        row.add(text);
        row.add(imageWidth);
        row.add(imageHeight);
        row.add(fullImageWidth);
        row.add(fullImageHeight);
        row.add(tim);
        row.add(spoiler);
        row.add(spoilerText);
        row.add(exifText);
        row.add(threadPost.id);
        row.add(isDead);
        row.add(closed);
        row.add(0);
        row.add(0);
        row.add(0);
        if (DEBUG) Log.i(TAG, "Thread dead status for " + boardCode + "/" + threadNo + " isDead = " + isDead);
        if (DEBUG) Log.i(TAG, "Watchlist cursor has: " + threadNo + " " + boardCode + " " + imageUrl + " " + shortText);
    }

    private void addRowFromCachedPrefs(MatrixCursor cursor, String[] threadComponents) {
        String boardCode = threadComponents[1];
        long threadNo = Long.valueOf(threadComponents[2]);
        long tim = Long.valueOf(threadComponents[0]);
        String shortText = (threadComponents[3].length() > ChanPost.MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN
                ? threadComponents[3].substring(0, ChanPost.MAX_BOARDTHREAD_IMAGETEXT_LEN) + "..."
                : threadComponents[3]);
        String headerText = null;
        String text = threadComponents[3];
        String imageUrl = threadComponents[4];
        String countryUrl = null;
        int imageWidth = Integer.valueOf(threadComponents[5]);
        int imageHeight = Integer.valueOf(threadComponents[6]);
        int fullImageWidth = imageWidth;
        int fullImageHeight = imageHeight;
        int spoiler = 0;
        String spoilerText = "";
        String exifText = "";
        int isDead = 1; // we don't know if it's dead or not, assume dead
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(threadNo);
        row.add(boardCode);
        row.add(0);
        row.add(imageUrl);
        row.add(countryUrl);
        row.add(shortText);
        row.add(headerText);
        row.add(text);
        row.add(imageWidth);
        row.add(imageHeight);
        row.add(fullImageWidth);
        row.add(fullImageHeight);
        row.add(tim);
        row.add(spoiler);
        row.add(spoilerText);
        row.add(exifText);
        row.add("");
        row.add(isDead);
        row.add(0);
        row.add(0);
        row.add(0);
        row.add(0);
        if (DEBUG) Log.i(TAG, "Thread dead status for " + boardCode + "/" + threadNo + " is unknown, assumed dead");
        if (DEBUG) Log.i(TAG, "Watchlist cursor has: " + threadNo + " " + boardCode + " " + imageUrl + " " + shortText);
    }

    protected void addFinalRow(MatrixCursor matrixCursor) {
        matrixCursor.addRow(new Object[] {
                2, "", 0,
                "", "",
                "", "", "",
                -1, -1, -1, -1, 0, 0,
                "", "", "", 1, 0, 0, 1, 0});
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
