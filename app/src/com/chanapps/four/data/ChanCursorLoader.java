package com.chanapps.four.data;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;

/**
 * A loader that queries the ChanDatabaseHelper and returns a {@link Cursor}.
 * This class implements the {@link Loader} protocol in a standard way for
 * querying cursors, building on {@link AsyncTaskLoader} to perform the cursor
 * query on a background thread so that it does not block the application's UI.
 * 
 */
public class ChanCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = ChanCursorLoader.class.getSimpleName();

    protected final ForceLoadContentObserver mObserver;

    protected SQLiteDatabase db;
    protected Cursor mCursor;
    protected Context context;

    protected String boardName;
    protected long threadNo;

    protected ChanCursorLoader(Context context, SQLiteDatabase db) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        this.db = db;
    }

    public ChanCursorLoader(Context context, SQLiteDatabase db, String boardName, long threadNo) {
        this(context, db);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
    }

    public ChanCursorLoader(Context context, SQLiteDatabase db, String boardName) {
        this(context, db, boardName, 0);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG, "loadInBackground");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        boolean hideTextOnlyPosts = prefs.getBoolean(SettingsActivity.PREF_HIDE_TEXT_ONLY_POSTS, false);
        Log.i("ChanCursorLoader", "prefs: " + hideAllText + " " + hideTextOnlyPosts);
    	String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
				+ "'http://0.thumbs.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
					+ " || '/thumb/' || " + ChanDatabaseHelper.POST_TIM + " || 's.jpg' 'image_url', "
                + (hideAllText ? " '' 'text', " : ChanDatabaseHelper.POST_TEXT + " 'text', ")
				+ ChanDatabaseHelper.POST_TN_W + " 'tn_w', " + ChanDatabaseHelper.POST_TN_H + " 'tn_h', "
                + ChanDatabaseHelper.POST_W + " 'w', " + ChanDatabaseHelper.POST_H + " 'h'"
				+ " FROM " + ChanDatabaseHelper.POST_TABLE
				+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
                + (threadNo != 0
                    ? "(" + ChanDatabaseHelper.POST_ID + "=" + threadNo + " OR " + ChanDatabaseHelper.POST_RESTO + "=" + threadNo + ")"
				    :  ChanDatabaseHelper.POST_RESTO + "=0 ")
                + (hideAllText || hideTextOnlyPosts ? " AND " + ChanDatabaseHelper.POST_TIM + " IS NOT NULL " : "")
                + " ORDER BY " + ChanDatabaseHelper.POST_TIM + " ASC";

    	if (db != null && db.isOpen()) {
    		Log.i(TAG, "loadInBackground database is ok");
    		Cursor cursor = db != null && db.isOpen() ? db.rawQuery(query, null) : null;
    		if (cursor != null) {
    			// Ensure the cursor window is filled
    			int count = db != null && db.isOpen() && cursor != null ? cursor.getCount() : 0;
    			Log.i(TAG, "loadInBackground cursor is ok, count: " + count);
    			if (count > 0) {
                    registerContentObserver(cursor, mObserver);
                }
    		}
    		return cursor;
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
