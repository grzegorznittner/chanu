/**
 * 
 */
package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanThreadService extends BaseChanService {
	private static final String TAG = ChanThreadService.class.getName();

	public ChanThreadService() {
		super("Thread");
	}
	
	protected ChanThreadService(String name) {
		super(name);
	}
	
	private int id = -1, boardName, now, time, name, sub, com, tim, filename, ext, w, h, tn_w, tn_h, fsize, resto, lastUpdate;
	
	protected Set<Integer> getListOfIds(ChanDatabaseHelper h, String boardName) {
		Set<Integer> ids = new HashSet<Integer>();
		Cursor c = null;
		try {
			String query = "SELECT " + ChanDatabaseHelper.POST_ID
					+ " FROM " + ChanDatabaseHelper.POST_TABLE
					+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "'";
			c = h.getWritableDatabase().rawQuery(query, null);
			int indexIdx = c.getColumnIndex(ChanDatabaseHelper.POST_ID);
			for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
			    ids.add(c.getInt(indexIdx));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while fetching list of thread ids for board " + boardName + ". " + e.getMessage(), e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return ids;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String boardName = intent.getStringExtra("board");
		int boardPage = intent.getIntExtra("page", 0);
		Log.i(TAG, "Handling board=" + boardName + " page=" + boardPage);

		BufferedReader in = null;
		ChanDatabaseHelper h = new ChanDatabaseHelper(getBaseContext());
		InsertHelper ih = new InsertHelper(h.getWritableDatabase(), ChanDatabaseHelper.POST_TABLE);
		
		try {
			Set<Integer> ids = getListOfIds(h, boardName);
			prepareColumnIndexes(ih);
			
			URL chanApi = new URL("http://api.4chan.org/" + boardName + "/" + boardPage + ".json");
	        URLConnection tc = chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength());
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
	        Gson gson = new GsonBuilder().create();
	        
			JsonReader reader = new JsonReader(in);
			reader.setLenient(true);
			reader.beginObject(); // has "threads" as single property
			reader.nextName(); // "threads"
			reader.beginArray();
			while (reader.hasNext()) { // iterate over threads
                reader.beginObject(); // has "posts" as single property
                reader.nextName(); // "posts"
                reader.beginArray();
                while (reader.hasNext()) { // first object is the thread post, spin over rest
                    ChanThread thread = gson.fromJson(reader, ChanThread.class);
                    thread.board = boardName;
                    boolean postExists = !ids.contains(thread.no);
                	Log.d(TAG, thread.toString() + ", existed = " + postExists);
                	addPost(ih, thread, postExists);
                }
                reader.endArray();
                reader.endObject();
            }
            reader.endArray();
            reader.endObject();
		} catch (Exception e) {
			Log.e(TAG, "Error parsing Chan board json. " + e.getMessage(), e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error closing reader", e);
			}
		}
		
		try {
//			String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
//					+ "'http://0.thumbs.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
//						+ " || '/thumb/' || " + ChanDatabaseHelper.POST_TIM + " || 's.jpg' 'thumbnail', "
//					+ " " + ChanDatabaseHelper.POST_COM + " 'text', " + ChanDatabaseHelper.POST_LAST_UPDATE
//					+ " FROM " + ChanDatabaseHelper.POST_TABLE
//					+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
//						+ ChanDatabaseHelper.POST_RESTO + "=0";
			String query = "SELECT count(*) 'num_threads'"
					+ " FROM " + ChanDatabaseHelper.POST_TABLE
					+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
						+ ChanDatabaseHelper.POST_RESTO + "=0";
			Cursor c = h.getWritableDatabase().rawQuery(query, null);
			int numIdx = c.getColumnIndex("num_threads");
			for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
			    Log.i(TAG, "Number of threads: " + c.getString(numIdx));
			}
			c.close();
			h.close();
		} catch (Exception e) {
			Log.e(TAG, "Error querying chan DB. " + e.getMessage(), e);
		}
	}
	
	protected void prepareColumnIndexes(InsertHelper ih) {
		if (id != -1) {
			// already initialized
			return;
		}
		id = ih.getColumnIndex(ChanDatabaseHelper.POST_ID);
		boardName = ih.getColumnIndex(ChanDatabaseHelper.POST_BOARD_NAME);
		now = ih.getColumnIndex(ChanDatabaseHelper.POST_NOW);
		time = ih.getColumnIndex(ChanDatabaseHelper.POST_TIME);
		name = ih.getColumnIndex(ChanDatabaseHelper.POST_NAME);
		sub = ih.getColumnIndex(ChanDatabaseHelper.POST_SUB);
		com = ih.getColumnIndex(ChanDatabaseHelper.POST_COM);
		tim = ih.getColumnIndex(ChanDatabaseHelper.POST_TIM);
		filename = ih.getColumnIndex(ChanDatabaseHelper.POST_FILENAME);
		ext = ih.getColumnIndex(ChanDatabaseHelper.POST_EXT);
		w = ih.getColumnIndex(ChanDatabaseHelper.POST_W);
		h = ih.getColumnIndex(ChanDatabaseHelper.POST_H);
		tn_w = ih.getColumnIndex(ChanDatabaseHelper.POST_TN_W);
		tn_h = ih.getColumnIndex(ChanDatabaseHelper.POST_TN_H);
		fsize = ih.getColumnIndex(ChanDatabaseHelper.POST_FSIZE);
		resto = ih.getColumnIndex(ChanDatabaseHelper.POST_RESTO);
		lastUpdate = ih.getColumnIndex(ChanDatabaseHelper.POST_LAST_UPDATE);
	}

	protected void addPost(InsertHelper ih, ChanThread thread, boolean postExists) throws SQLiteConstraintException {
		if (postExists) {
			ih.prepareForInsert();
		} else {
			ih.prepareForReplace();
		}
		
		ih.bind(id, thread.no);
		ih.bind(boardName, thread.board);
		ih.bind(resto, thread.resto);
		ih.bind(time, thread.time);
		if (thread.now != null) {
			ih.bind(now, thread.now);
		}
		if (thread.name != null) {
			ih.bind(name, thread.name);
		}
		if (thread.com != null) {
			ih.bind(com, thread.com);
		}
		if (thread.sub != null) {
			ih.bind(sub, thread.sub);
		}
		if (thread.tim != null) {
			ih.bind(tim, thread.tim);
		}
		if (thread.filename != null) {
			ih.bind(filename, thread.filename);
		}
		if (thread.ext != null) {
			ih.bind(ext, thread.ext);
		}
		if (thread.w != -1) {
			ih.bind(w, thread.w);
		}
		if (thread.h != -1) {
			ih.bind(h, thread.h);
		}
		if (thread.tn_w != -1) {
			ih.bind(tn_w, thread.tn_w);
		}
		if (thread.tn_h != -1) {
			ih.bind(tn_h, thread.tn_h);
		}
		if (thread.fsize != -1) {
			ih.bind(fsize, thread.fsize);
		}
		ih.bind(lastUpdate, new Date().getTime());
		
		ih.execute();
	}

}
