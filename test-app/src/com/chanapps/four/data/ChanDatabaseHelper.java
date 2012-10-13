/**
 * 4Channer
 */
package com.chanapps.four.data;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanDatabaseHelper extends SQLiteOpenHelper {
	static final String TAG = "ChanDatabaseHelper";
	
	public static final String DB_NAME = "4Channer";
	public static final int DB_VERSION = 1;
	
	public static final String THREAD_TABLE = "thread";
	public static final String THREAD_ID = "_id";
	public static final String THREAD_BOARD_NAME = "board_name";
	public static final String THREAD_NAME = "no";
	public static final String THREAD_AUTHOR = "author";
	public static final String THREAD_TIME = "time";
	public static final String THREAD_SUB = "sub";
	public static final String THREAD_COM = "com";
	public static final String THREAD_TIM = "tim";
	public static final String THREAD_FILENAME = "filename";
	public static final String THREAD_EXT = "ext";
	public static final String THREAD_W = "w";
	public static final String THREAD_H = "h";
	public static final String THREAD_TN_W = "tn_w";
	public static final String THREAD_TN_H = "tn_h";
	public static final String THREAD_FSIZE = "fsize";
	public static final String THREAD_LAST_UPDATE = "last_update";
	
	public static final String POST_TABLE = "post";
	public static final String POST_ID = "_id";
	public static final String POST_THREAD_ID = "thread_id";
	public static final String POST_BOARD_NAME = "board_name";
	public static final String POST_AUTHOR = "author";
	public static final String POST_TIME = "time";
	public static final String POST_COM = "com";
	public static final String POST_TIM = "tim";
	public static final String POST_FILENAME = "filename";
	public static final String POST_EXT = "ext";
	public static final String POST_W = "w";
	public static final String POST_H = "h";
	public static final String POST_TN_W = "tn_w";
	public static final String POST_TN_H = "tn_h";
	public static final String POST_FSIZE = "fsize";
	public static final String POST_LAST_UPDATE = "last_update";
	
		
	public ChanDatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION, new ErrorHandler());
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
	    super.onOpen(db);
	    if (!db.isReadOnly()) {
	        // Enable foreign key constraints
	        db.execSQL("PRAGMA foreign_keys=ON;");
	    }
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + THREAD_TABLE + " ("
				+ THREAD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ THREAD_BOARD_NAME + " TEXT NOT NULL, "
				+ THREAD_NAME + " TEXT, "
				+ THREAD_AUTHOR + " TEXT, "
				+ THREAD_TIME + " INTEGER NOT NULL, "
				+ THREAD_SUB + " TEXT NOT NULL, "
				+ THREAD_COM + " TEXT NOT NULL, "
				+ THREAD_TIM + " INTEGER NOT NULL, "
				+ THREAD_FILENAME + " TEXT NOT NULL, "
				+ THREAD_EXT + " TEXT NOT NULL, "
				+ THREAD_W + " INTEGER NOT NULL, "
				+ THREAD_H + " INTEGER NOT NULL, "
				+ THREAD_TN_W + " INTEGER NOT NULL, "
				+ THREAD_TN_H + " INTEGER NOT NULL, "
				+ THREAD_FSIZE + " INTEGER NOT NULL, "
				+ THREAD_LAST_UPDATE + " INTEGER NOT NULL "
				+ ");";
		Log.e(TAG, "Executing: " + sql);
		db.execSQL(sql);
		
		sql = "CREATE INDEX " + THREAD_TABLE + "_tim " + "ON " + THREAD_TABLE + "(_id, tim);";
		Log.e(TAG, "Executing: " + sql);
		db.execSQL(sql);

		sql = "CREATE TABLE " + POST_TABLE + " ("
			+ POST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "FOREIGN KEY (" + POST_THREAD_ID + ") REFERENCES " + THREAD_TABLE + " (" + THREAD_ID + "), "
			+ POST_BOARD_NAME + " TEXT NOT NULL, "
			+ POST_AUTHOR + " TEXT, "
			+ POST_TIME + " INTEGER NOT NULL, "
			+ POST_COM + " TEXT NOT NULL, "
			+ POST_TIM + " INTEGER NOT NULL, "
			+ POST_FILENAME + " TEXT NOT NULL, "
			+ POST_EXT + " TEXT NOT NULL, "
			+ POST_W + " INTEGER NOT NULL, "
			+ POST_H + " INTEGER NOT NULL, "
			+ POST_TN_W + " INTEGER NOT NULL, "
			+ POST_TN_H + " INTEGER NOT NULL, "
			+ POST_FSIZE + " INTEGER NOT NULL "
			+ ");";
		Log.e(TAG, "Executing: " + sql);
		db.execSQL(sql);

		sql = "CREATE INDEX " + POST_TABLE + "_tim " + "ON " + POST_TABLE + "(_id, tim);";
		Log.e(TAG, "Executing: " + sql);
		db.execSQL(sql);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	
	static class ErrorHandler implements DatabaseErrorHandler {
		@Override
		public void onCorruption(SQLiteDatabase dbObj) {
			Log.e(TAG, "Database corruption!!!");
		}
	}
}
