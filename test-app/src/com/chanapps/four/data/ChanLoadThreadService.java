/**
 * 
 */
package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.chanapps.four.test.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanLoadThreadService extends ChanLoadBoardService {
	private static final String TAG = ChanLoadThreadService.class.getName();
	
	public ChanLoadThreadService() {
		super("Post");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String boardName = intent.getStringExtra(ChanHelper.BOARD_CODE);
		long threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);

		if (threadNo == 0) {
            toastUI(R.string.thread_service_no_thread);
			Log.w(TAG, "Thread number not passed!");
			return;
		}

        initDbHelpers();

		BufferedReader in = null;
		try {
			Set<Integer> ids = getListOfIds(boardName);
			prepareColumnIndexes();
			
			URL chanApi = new URL("http://api.4chan.org/" + boardName + "/res/" + threadNo + ".json");
            Log.i(TAG, "Calling API " + chanApi + " ...");
            URLConnection tc = chanApi.openConnection();
            Log.i(TAG, "Opened API " + chanApi + " response length=" + tc.getContentLength());
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
	        Gson gson = new GsonBuilder().create();
	        
			JsonReader reader = new JsonReader(in);
			reader.setLenient(true);
			reader.beginObject();
			reader.nextName(); // "posts"
			reader.beginArray();
			while (reader.hasNext()) {
				ChanThread thread = gson.fromJson(reader, ChanThread.class);
				thread.board = boardName;
				boolean postExists = !ids.contains(thread.no);
            	Log.i(TAG, thread.toString() + ", existed = " + postExists);
            	addPost(thread, postExists);
			}
        } catch (FileNotFoundException e) {
            toastUI(R.string.thread_service_not_found);
            Log.e(TAG, "Chan thread not found. " + e.getMessage(), e);
        } catch (IOException e) {
            toastUI(R.string.thread_service_couldnt_read);
            Log.e(TAG, "IO Error reading Chan thread json. " + e.getMessage(), e);
		} catch (Exception e) {
            toastUI(R.string.thread_service_couldnt_load);
			Log.e(TAG, "Error parsing Chan thread json. " + e.getMessage(), e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error closing reader", e);
			}
		}

        // getNumPosts(boardName, threadNo);

        cleanupDbHelpers();
	}

    protected void getNumPosts(String boardName, long threadNo) {
        if (chanDatabaseHelper == null) {
            return;
        }
        synchronized (chanDatabaseHelper) {
            if (chanDatabaseHelper == null) {
                return;
            }
            try {
                    String query = "SELECT count(*) 'num_posts'"
                            + " FROM " + ChanDatabaseHelper.POST_TABLE
                            + " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
                            + "(" + ChanDatabaseHelper.POST_ID + "=" + threadNo + " OR " + ChanDatabaseHelper.POST_RESTO + "=" + threadNo + ")";
                    Cursor c = chanDatabaseHelper.getWritableDatabase().rawQuery(query, null);
                    int numIdx = c.getColumnIndex("num_posts");
                    for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
                        Log.i(TAG, "Thread " + threadNo + " has " + c.getString(numIdx) + " posts");
                    }
                    c.close();
                } catch (Exception e) {
                      Toast.makeText(this, R.string.thread_service_couldnt_read_db, Toast.LENGTH_SHORT);
                    Log.e(TAG, "Error querying chan DB. " + e.getMessage(), e);
                }        synchronized (chanDatabaseHelper) {

            }

        }
    }

}
