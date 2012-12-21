/**
 * 
 */
package com.chanapps.four.service;

import android.content.Intent;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ThreadLoadService extends BoardLoadService {

    protected static final String TAG = ThreadLoadService.class.getName();

    public ThreadLoadService() {
   		super("thread");
   	}

    protected ThreadLoadService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
        long threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		Log.i(TAG, "Handling board=" + boardCode + " threadNo=" + threadNo);
        if (threadNo == 0) {
            Log.e(TAG, "Board loading must be done via the BoardLoadService");
        }
        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        BufferedReader in = null;
        HttpURLConnection tc = null;
		try {			
			URL chanApi = new URL("http://api.4chan.org/" + boardCode + "/res/" + threadNo + ".json");

            tc = (HttpURLConnection) chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength() + " code=" + tc.getResponseCode());
            if (tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "Got 404 on thread, thread no longer exists");
                // FIXME: mark thread as dead so user can be notified
                //ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
                //thread.isDead = true;
                //ChanFileStorage.storeThreadData(getBaseContext(), thread);
            }
            else {
                in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
                parseThread(boardCode, threadNo, in);
            }
        } catch (IOException e) {
            toastUI(R.string.board_service_couldnt_read);
            Log.e(TAG, "IO Error reading Chan board json. " + e.getMessage(), e);
		} catch (Exception e) {
            toastUI(R.string.board_service_couldnt_load);
			Log.e(TAG, "Error parsing Chan board json. " + e.getMessage(), e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
                if (tc != null) {
                    tc.disconnect();
                }
			} catch (Exception e) {
				Log.e(TAG, "Error closing reader", e);
			}
		}
	}

	protected void parseThread(String boardCode, long threadNo, BufferedReader in) throws IOException {
    	long time = new Date().getTime();
    	ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
    	if (thread == null) {
    		thread = new ChanThread();
        	thread.board = boardCode;
            thread.no = threadNo;
    	}
        
    	List<ChanPost> posts = new ArrayList<ChanPost>();
        Gson gson = new GsonBuilder().create();

        JsonReader reader = new JsonReader(in);
        reader.setLenient(true);
        reader.beginObject(); // has "posts" as single property
        reader.nextName(); // "posts"
        reader.beginArray();

        while (reader.hasNext()) {
            ChanPost post = gson.fromJson(reader, ChanPost.class);
            post.board = boardCode;
            posts.add(post);
            if (new Date().getTime() - time > STORE_INTERVAL_MS) {
            	mergePosts(thread, posts);
                ChanFileStorage.storeThreadData(getBaseContext(), thread);
        	}
        }
        mergePosts(thread, posts);
        ChanFileStorage.storeThreadData(getBaseContext(), thread);
    }

}
