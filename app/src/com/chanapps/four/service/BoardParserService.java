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
import java.util.Collections;
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
import com.chanapps.four.service.profile.NetworkProfile.Failure;
import com.chanapps.four.widget.BoardWidgetProvider;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardParserService extends BaseChanService implements ChanIdentifiedService {

    protected static final String TAG = BoardParserService.class.getSimpleName();
    private static final boolean DEBUG = true;
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;

    private String boardCode;
    private int pageNo;
    private boolean force;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, int pageNo) {
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + false );
        Intent intent = new Intent(context, BoardParserService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        context.startService(intent);
    }

    public static void startServiceWithPriority(Context context, String boardCode, int pageNo) {
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + true );
        Intent intent = new Intent(context, BoardParserService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
    }

    public BoardParserService() {
   		super("board");
   	}

    protected BoardParserService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);
		if (DEBUG) Log.i(TAG, "Handling board=" + boardCode + " page=" + pageNo);

        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();

            File boardFile = ChanFileStorage.getBoardFile(context, boardCode, pageNo);
            parseBoard(new BufferedReader(new FileReader(boardFile)));

            if (DEBUG) Log.i(TAG, "Parsed board " + boardCode + " page " + pageNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();

            ChanFileStorage.storeBoardData(context, board);
            if (DEBUG) Log.i(TAG, "Stored board " + boardCode + " page " + pageNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");

            // thread files are stored in separate service call to make board parsing faster
            if (force) {
            	BoardThreadsParserService.startServiceWithPriority(getBaseContext(), boardCode, pageNo);
            } else {
            	BoardThreadsParserService.startService(getBaseContext(), boardCode, pageNo);
            }
            // tell it to refresh widgets for board if any are configured
            BoardWidgetProvider.updateAll(context, boardCode);

            NetworkProfileManager.instance().finishedParsingData(this);
        } catch (Exception e) {
            //toastUI(R.string.board_service_couldnt_read);
        	NetworkProfileManager.instance().failedParsingData(this, Failure.WRONG_DATA);
            Log.e(TAG, "IO Error reading Chan board json", e);
		}
	}

    private void parseBoard(BufferedReader in) throws IOException {
//    	List<ChanPost> stickyPosts = new ArrayList<ChanPost>();
    	List<ChanPost> threads = new ArrayList<ChanPost>();
    	board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
    	if (board.defData) {
    		// default board we should not use it
    		board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
    	}

        if (pageNo != 0) { // preserve existing threads on subsequent page loads
//            if (board.stickyPosts != null && board.stickyPosts.length > 0) {
//                Collections.addAll(stickyPosts, board.stickyPosts);
//                if (DEBUG) Log.i(TAG, "Added " + board.stickyPosts.length + " sticky posts from storage");
//            }
            if (board.threads != null && board.threads.length > 0) {
                if (board.threads.length < MAX_THREAD_RETENTION_PER_BOARD) {
                    Collections.addAll(threads, board.threads);
                    if (DEBUG) Log.i(TAG, "Added " + board.threads.length + " threads from storage");
                }
                else {
                    for (int i = 0; i < MAX_THREAD_RETENTION_PER_BOARD; i++) {
                        threads.add(board.threads[i]);
                    }
                    if (DEBUG) Log.i(TAG, "Hit thread retention limit, adding only " + MAX_THREAD_RETENTION_PER_BOARD + " threads from storage");
                }
            }
        }

        ObjectMapper mapper = ChanHelper.getJsonMapper();
        JsonNode rootNode = mapper.readValue(in, JsonNode.class);
        for (JsonNode threadValue : rootNode.path("threads")) { // iterate over threads
            JsonNode postValue = threadValue.path("posts").get(0); // first object is the thread post
            ChanPost post = mapper.readValue(postValue, ChanPost.class);
            post.board = boardCode;
            post.mergeIntoThreadList(threads);
        }

        board.threads = threads.toArray(new ChanPost[0]);
        if (DEBUG) Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(boardCode, pageNo, force);
	}
}
