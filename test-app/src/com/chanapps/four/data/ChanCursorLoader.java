package com.chanapps.four.data;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * A loader that queries the ChanDatabaseHelper and returns a {@link Cursor}.
 * This class implements the {@link Loader} protocol in a standard way for
 * querying cursors, building on {@link AsyncTaskLoader} to perform the cursor
 * query on a background thread so that it does not block the application's UI.
 * 
 */
public class ChanCursorLoader extends AsyncTaskLoader<Cursor> {
    final ForceLoadContentObserver mObserver;

    String boardName;
    int threadNo;

    SQLiteDatabase db;
    Cursor mCursor;

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
				+ "'http://0.thumbs.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
					+ " || '/thumb/' || " + ChanDatabaseHelper.POST_TIM + " || 's.jpg' 'image_url', "
				+ " " + ChanDatabaseHelper.POST_COM + " 'text', "
				+ ChanDatabaseHelper.POST_TN_W + " 'tn_w', " + ChanDatabaseHelper.POST_TN_H + " 'tn_h'"
				+ " FROM " + ChanDatabaseHelper.POST_TABLE
				+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
					+ ChanDatabaseHelper.POST_RESTO + "=0 ORDER BY " + ChanDatabaseHelper.POST_TIM;
    	if (db != null && db.isOpen()) {
    		Cursor cursor = db.rawQuery(query, null);
    		if (cursor != null) {
    			// Ensure the cursor window is filled
    			cursor.getCount();
    			registerContentObserver(cursor, mObserver);
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

    public ChanCursorLoader(Context context, SQLiteDatabase db, String boardName) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        this.db = db;
        this.boardName = boardName;
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
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        
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
