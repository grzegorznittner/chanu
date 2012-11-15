package com.chanapps.four.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ChanPostCursorLoader extends ChanThreadCursorLoader {
    private String boardName;
    private long threadNo;

    public ChanPostCursorLoader(Context context, SQLiteDatabase db, String boardName, long threadNo) {
        super(context, db);
        this.boardName = boardName;
        this.threadNo = threadNo;
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG(), "loadInBackground");
    	String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
				+ "'http://0.thumbs.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
					+ " || '/thumb/' || " + ChanDatabaseHelper.POST_TIM + " || 's.jpg' 'image_url', "
				+ ChanDatabaseHelper.POST_NOW + " || ' ' || " + ChanDatabaseHelper.POST_COM + " 'text', "
				+ ChanDatabaseHelper.POST_TN_W + " 'tn_w', " + ChanDatabaseHelper.POST_TN_H + " 'tn_h'"
				+ " FROM " + ChanDatabaseHelper.POST_TABLE
				+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
					+ "(" + ChanDatabaseHelper.POST_ID + "=" + threadNo + " OR " + ChanDatabaseHelper.POST_RESTO + "=" + threadNo + ")"
				+ " ORDER BY " + ChanDatabaseHelper.POST_TIM + " DESC";
    	if (db != null && db.isOpen()) {
    		Log.i(TAG(), "loadInBackground database is ok");
    		Cursor cursor = db.rawQuery(query, null);
    		if (cursor != null) {
    			// Ensure the cursor window is filled
    			int count = cursor.getCount();
    			Log.i(TAG(), "loadInBackground cursor is ok, count: " + count);
    			registerContentObserver(cursor, mObserver);
    		}
    		return cursor;
    	}
        return null;
    }

	protected String TAG() {
		return "ChanPostCursorLoader";
	}
}
