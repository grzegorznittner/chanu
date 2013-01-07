/**
 * 
 */
package com.chanapps.four.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ThreadLoadService extends BaseChanService {

    protected static final String TAG = ThreadLoadService.class.getName();

    protected static final long STORE_INTERVAL_MS = 2000;

    private String boardCode;
    private long threadNo;
    private boolean force;
    private ChanThread thread;

    public static void startService(Context context, String boardCode, long threadNo) {
        Log.i(TAG, "Start thread load service for " + boardCode + " thread " + threadNo );
        Intent intent = new Intent(context, ThreadLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        context.startService(intent);
    }

    public static void startServiceWithPriority(Context context, String boardCode, long threadNo) {
        Log.i(TAG, "Start thread load service for " + boardCode + " thread " + threadNo );
        Intent intent = new Intent(context, ThreadLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
    }

    public ThreadLoadService() {
   		super("thread");
   	}

    protected ThreadLoadService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
        force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);

        Log.i(TAG, "Handling board=" + boardCode + " threadNo=" + threadNo + " force=" + force);

        if (threadNo == 0) {
            Log.e(TAG, "Board loading must be done via the FetchChanDataService");
        }
        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
			
			File threadFile = ChanFileStorage.getThreadFile(getBaseContext(), boardCode, threadNo);
			parseThread(new BufferedReader(new FileReader(threadFile)));

            Log.w(TAG, "Parsed thread " + boardCode + "/" + threadNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();

            ChanFileStorage.storeThreadData(getBaseContext(), thread);
            Log.w(TAG, "Stored thread " + boardCode + "/" + threadNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
        } catch (IOException e) {
            toastUI(R.string.board_service_couldnt_read);
            Log.e(TAG, "IO Error reading Chan board json. " + e.getMessage(), e);
		} catch (Exception e) {
            toastUI(R.string.board_service_couldnt_load);
			Log.e(TAG, "Error parsing Chan board json. " + e.getMessage(), e);
		}
	}

	protected void parseThread(BufferedReader in) throws IOException {
    	Log.i(TAG, "starting parsing thread " + boardCode + "/" + threadNo);
        long time = new Date().getTime();

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
            Log.v(TAG, "Added post " + post.no + " to thread " + boardCode + "/" + threadNo);
            if (new Date().getTime() - time > STORE_INTERVAL_MS) {
            	thread.mergePosts(posts);
                ChanFileStorage.storeThreadData(getBaseContext(), thread);
        	}
        }
        thread.mergePosts(posts);

        Log.i(TAG, "finished parsing thread " + boardCode + "/" + threadNo);
    }
}
