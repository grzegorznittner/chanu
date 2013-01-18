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
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardThreadsParserService extends BaseChanService implements ChanIdentifiedService {
    protected static final String TAG = "BoardThreadsParserService";
    private static final boolean DEBUG = true;
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;

    private String boardCode;
    private int pageNo;
    private boolean force;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, int pageNo) {
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + false );
        Intent intent = new Intent(context, BoardThreadsParserService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        context.startService(intent);
    }

    public static void startServiceWithPriority(Context context, String boardCode, int pageNo) {
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + true );
        Intent intent = new Intent(context, BoardThreadsParserService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
    }

    public BoardThreadsParserService() {
   		super("boardThreads");
   	}

    protected BoardThreadsParserService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);
		if (DEBUG) Log.i(TAG, "Handling board=" + boardCode + " page=" + pageNo);

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();

        	board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
        	if (board.defData) {
        		// at this point valid board object should be available
        		return;
        	}
        	
            File boardFile = ChanFileStorage.getBoardFile(context, boardCode, pageNo);
            parseBoard(new BufferedReader(new FileReader(boardFile)));
            boardFile.delete();

            if (DEBUG) Log.i(TAG, "Parsed board " + boardCode + " page " + pageNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();
        } catch (Exception e) {
            Log.e(TAG, "Board parsing error", e);
		}
	}

    private void parseBoard(BufferedReader in) throws IOException {
//    	List<ChanPost> stickyPosts = new ArrayList<ChanPost>();
    	List<ChanPost> threads = new ArrayList<ChanPost>();

        ObjectMapper mapper = ChanHelper.getJsonMapper();
        JsonNode rootNode = mapper.readValue(in, JsonNode.class);
        for (JsonNode threadValue : rootNode.path("threads")) { // iterate over threads
            ChanThread thread = null;
            List<ChanPost> posts = new ArrayList<ChanPost>();
            boolean first = true;
//            boolean isSticky = false;
            for (JsonNode postValue : threadValue.path("posts")) { // first object is the thread post
                ChanPost post = mapper.readValue(postValue, ChanPost.class);
                post.board = boardCode;
//                if (post.sticky > 0 || isSticky) {
//                    post.mergeIntoThreadList(stickyPosts);
//                	isSticky = true;
//                } else {
                	if (first) {
                		thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, post.no);
                		// if thread was not stored create a new object
                		if (thread == null || thread.defData) {
                			thread = new ChanThread();
                			thread.board = boardCode;
                			thread.lastFetched = 0;
                			thread.no = post.no;
                            // note we don't set the lastUpdated here because we didn't pull the full thread yet
                		} else if (board.lastFetched < thread.lastFetched) {
                			// do not update thread if was fetched later than board
                			break;
                		}
                        post.mergeIntoThreadList(threads);
                		first = false;
                	}
                	posts.add(post);
//                }
                //if (DEBUG) Log.v(TAG, post.toString());
            }
            if (thread != null) {
                thread.mergePosts(posts);
            	ChanFileStorage.storeThreadData(getBaseContext(), thread);
            }
        }
        if (DEBUG) Log.i(TAG, "Stored " + threads.size() + " threads for board " + boardCode);
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(boardCode, pageNo, force);
	}
}
