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
import java.util.Date;
import java.util.List;

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
import com.chanapps.four.widget.UpdateWidgetService;
import com.chanapps.four.service.NetworkProfile.Failure;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardLoadService extends BaseChanService implements ChanIdentifiedService {

    protected static final String TAG = BoardLoadService.class.getSimpleName();
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;

    private String boardCode;
    private int pageNo;
    private boolean force;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, int pageNo) {
        Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + false );
        Intent intent = new Intent(context, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        context.startService(intent);
    }

    public static void startServiceWithPriority(Context context, String boardCode, int pageNo) {
        Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + true );
        Intent intent = new Intent(context, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
    }

    public BoardLoadService() {
   		super("board");
   	}

    protected BoardLoadService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);
		Log.i(TAG, "Handling board=" + boardCode + " page=" + pageNo);

        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();

            File boardFile = ChanFileStorage.getBoardFile(context, boardCode, pageNo);
            parseBoard(new BufferedReader(new FileReader(boardFile)));

            Log.w(TAG, "Parsed board " + boardCode + " page " + pageNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();

            ChanFileStorage.storeBoardData(context, board);
            Log.w(TAG, "Stored board " + boardCode + " page " + pageNo
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");

            // tell it to refresh widget
            if (boardCode.equals(UpdateWidgetService.getConfiguredBoardWidget(context))) {
                Intent updateIntent = new Intent(context, UpdateWidgetService.class);
                context.startService(updateIntent);
            }

            NetworkProfileManager.instance().finishedParsingData(this);
        } catch (Exception e) {
            //toastUI(R.string.board_service_couldnt_read);
        	NetworkProfileManager.instance().failedParsingData(this, Failure.WRONG_DATA);
            Log.e(TAG, "IO Error reading Chan board json", e);
		}
	}

    private void parseBoard(BufferedReader in) throws IOException {
    	long time = new Date().getTime();
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
//                Log.i(TAG, "Added " + board.stickyPosts.length + " sticky posts from storage");
//            }
            if (board.threads != null && board.threads.length > 0) {
                if (board.threads.length < MAX_THREAD_RETENTION_PER_BOARD) {
                    Collections.addAll(threads, board.threads);
                    Log.i(TAG, "Added " + board.threads.length + " threads from storage");
                }
                else {
                    for (int i = 0; i < MAX_THREAD_RETENTION_PER_BOARD; i++) {
                        threads.add(board.threads[i]);
                    }
                    Log.i(TAG, "Hit thread retention limit, adding only " + MAX_THREAD_RETENTION_PER_BOARD + " threads from storage");
                }
            }
        }

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
            
            ChanThread thread = null;
            List<ChanPost> posts = new ArrayList<ChanPost>();
            boolean first = true;
//            boolean isSticky = false;
            while (reader.hasNext()) { // first object is the thread post, spin over rest
                ChanPost post = gson.fromJson(reader, ChanPost.class);
                post.board = boardCode;
//                if (post.sticky > 0 || isSticky) {
//                    post.mergeIntoThreadList(stickyPosts);
//                	isSticky = true;
//                } else {
                	if (first) {
                		thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, post.no);
                		// if thread was not stored create a new object
                		if (thread == null) {
                			thread = new ChanThread();
                			thread.board = boardCode;
                			thread.no = post.no;
                            // note we don't set the lastUpdated here because we didn't pull the full thread yet
                		}
                        post.mergeIntoThreadList(threads);
                		first = false;
                	}
                	posts.add(post);
//                }
                Log.v(TAG, post.toString());
            }
            if (thread != null) {
                thread.mergePosts(posts);
            	ChanFileStorage.storeThreadData(getBaseContext(), thread);
            	if (new Date().getTime() - time > STORE_INTERVAL_MS) {
            		board.threads = threads.toArray(new ChanPost[0]);
//                    board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
                    ChanFileStorage.storeBoardData(getBaseContext(), board);
            	}
            }
            reader.endArray();
            reader.endObject();
        }

        board.threads = threads.toArray(new ChanPost[0]);
//        board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
        Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(boardCode, pageNo, force);
	}
}
