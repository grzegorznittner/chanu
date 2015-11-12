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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ThreadParserService extends BaseChanService implements ChanIdentifiedService {

    protected static final String TAG = ThreadParserService.class.getName();
    private static final boolean DEBUG = false;

    protected static final long STORE_INTERVAL_MS = 2000;
    public static final String THREAD_FETCH_TIME = "threadFetchTime";

    private String boardCode;
    private long threadNo;
    private boolean priority;
    private ChanThread thread;
    private long threadFetchTime;

    public static void startService(Context context, String boardCode, long threadNo, boolean priority) {
        if (DEBUG) Log.i(TAG, "Start thread load service for " + boardCode + " thread " + threadNo
                + " priority=" + priority);
        Intent intent = new Intent(context, ThreadParserService.class);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        intent.putExtra(THREAD_FETCH_TIME, new Date().getTime());
        if (priority)
            intent.putExtra(PRIORITY_MESSAGE_FETCH, 1);
        context.startService(intent);
    }

    public ThreadParserService() {
   		super("thread");
   	}

    protected ThreadParserService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        priority = intent.getIntExtra(PRIORITY_MESSAGE_FETCH, 0) > 0;
        threadFetchTime = intent.getLongExtra(THREAD_FETCH_TIME, 0);

        if (DEBUG) Log.i(TAG, "Handling board=" + boardCode + " threadNo=" + threadNo + " priority=" + priority);

        if (threadNo == 0) {
            Log.e(TAG, "Board loading must be done via the FetchChanDataService");
        }
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)) {
            Log.e(TAG, "Watchlist is never parsed but stored directly");
            return;
        }
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)) {
            Log.e(TAG, "Favorites is never parsed but stored directly");
            return;
        }

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
			if (thread == null || thread.defData) {
				thread = new ChanThread();
                thread.board = boardCode;
                thread.no = threadNo;
                thread.isDead = false;
			} else if (thread.lastFetched > threadFetchTime) {
				if (DEBUG) Log.i(TAG, "Thread " + boardCode + "/" + threadNo + " won't be parsed. "
					+ "Last fetched " + new Date(thread.lastFetched) + ", scheduled " + new Date(threadFetchTime));
				NetworkProfileManager.instance().finishedParsingData(this);
				return;
			}
			thread.lastFetched = threadFetchTime;
			int previousPostNum = thread.posts.length;
			
			File threadFile = ChanFileStorage.getThreadFile(getBaseContext(), boardCode, threadNo);
			if (threadFile == null || !threadFile.exists()) {
                if (DEBUG) Log.i(TAG, "Thread file " + threadFile.getAbsolutePath() + " was deleted, probably already parsed.");
                NetworkProfileManager.instance().failedParsingData(this, Failure.MISSING_DATA);
				return;
			}
			parseThread(threadFile);

			if (DEBUG) Log.i(TAG, "Parsed thread " + boardCode + "/" + threadNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();
            threadFile.delete();

            if (previousPostNum > 0 && thread.posts.length == 0) {
                if (DEBUG) Log.w(TAG, "Thread " + boardCode + "/" + threadNo + " has 0 posts after parsing, won't be stored");
            } else {
                if (!boardCode.equals(thread.board)) {
                    if (DEBUG) Log.i(TAG, "Found inconsistent thread boardCode=" + thread.board + ", repairing to=" + boardCode);
                    thread.board = boardCode;
                }
                if (DEBUG) Log.i(TAG, "In onHandleIntent in ThreadParserService calling storeThreadData for /" + thread.board + "/" + thread.no);
                ChanFileStorage.storeThreadData(getBaseContext(), thread);
                if (DEBUG) Log.i(TAG, "Stored thread " + boardCode + "/" + threadNo + " with " + thread.posts.length + " posts"
                		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            }
            threadUpdateMessage = null;
            NetworkProfileManager.instance().finishedParsingData(this);
        } catch (Exception e) {
			Log.e(TAG, "Error parsing thread json. " + e.getMessage(), e);
        	NetworkProfileManager.instance().failedParsingData(this, Failure.WRONG_DATA);
		}
	}

    String threadUpdateMessage = null;

	protected void parseThread(File in) throws IOException {
    	if (DEBUG) Log.i(TAG, "starting parsing thread " + boardCode + "/" + threadNo);

    	List<ChanPost> posts = new ArrayList<ChanPost>();
    	ObjectMapper mapper = BoardParserService.getJsonMapper();
        JsonNode rootNode = mapper.readValue(in, JsonNode.class);

        for (JsonNode postValue : rootNode.path("posts")) { // first object is the thread post
            ChanPost post = mapper.readValue(postValue, ChanPost.class);
            if (post != null) {
                post.board = boardCode;
                posts.add(post);
            }
            //if (DEBUG) Log.v(TAG, "Added post " + post.no + " to thread " + boardCode + "/" + threadNo);
        }
        if (thread != null)
            thread.mergePosts(posts);

        if (DEBUG) Log.i(TAG, "finished parsing thread " + boardCode + "/" + threadNo);
    }
	
	@Override
	public ChanActivityId getChanActivityId() {
		ChanActivityId id = new ChanActivityId(boardCode, threadNo, priority);
        if (threadUpdateMessage != null)
            id.threadUpdateMessage = threadUpdateMessage;
	    return id;
    }

    @Override
    public String toString() {
        return "ThreadParserService : " + getChanActivityId();
    }

}
