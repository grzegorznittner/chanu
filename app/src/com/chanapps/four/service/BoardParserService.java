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

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardParserService extends BaseChanService implements ChanIdentifiedService {

    protected static final String TAG = BoardParserService.class.getSimpleName();
    private static final boolean DEBUG = true;
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 200;

    private String boardCode;
    private boolean boardCatalog;
    private int pageNo;
    private boolean priority;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, int pageNo) {
        startService(context, boardCode, pageNo, false);
    }

    public static void startServiceWithPriority(Context context, String boardCode, int pageNo) {
        startService(context, boardCode, pageNo, true);
    }
    
    public static void startService(Context context, String boardCode, int pageNo, boolean priority) {
    	if (ChanBoard.isVirtualBoard(boardCode)) {
    		return;
    	}
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " priority=" + priority );
        Intent intent = new Intent(context, BoardParserService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.BOARD_CATALOG, pageNo == -1 ? 1 : 0);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        if (priority) {
	        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
	        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        }
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
		boardCatalog = intent.getIntExtra(ChanHelper.BOARD_CATALOG, 0) == 1;
		pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        priority = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false)
            || (intent.getIntExtra(ChanHelper.PRIORITY_MESSAGE, 0) > 0);
		if (DEBUG) Log.i(TAG, "Handling board=" + boardCode + " priority=" + priority);

        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();

            File boardFile = ChanFileStorage.getBoardFile(context, boardCode, pageNo);
            if (boardCatalog) {
            	parseBoardCatalog(new BufferedReader(new FileReader(boardFile)));
            } else {
            	parseBoard(new BufferedReader(new FileReader(boardFile)));
            }
            if (board != null)
                board.lastFetched = Calendar.getInstance().getTimeInMillis();

            if (DEBUG) Log.i(TAG, "Parsed board " + boardCode + (pageNo == -1 ? " catalog" : " page " + pageNo)
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();

            if (board != null)
                ChanFileStorage.storeBoardData(context, board);
            if (DEBUG) Log.i(TAG, "Stored board " + boardCode + (pageNo == -1 ? " catalog" : " page " + pageNo)
            		+ " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            setActionBarSubtitle();

            if (!boardCatalog) {
	            // thread files are stored in separate service call to make board parsing faster
	            if (priority) {
	            	BoardThreadsParserService.startServiceWithPriority(getBaseContext(), boardCode, pageNo);
	            } else {
	            	BoardThreadsParserService.startService(getBaseContext(), boardCode, pageNo);
	            }
            }
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
            try {
                ChanPost post = mapper.readValue(postValue, ChanPost.class);
                if (post != null) {
                    post.board = boardCode;
                    post.mergeIntoThreadList(threads);
                }
            }
            catch (JsonMappingException e) { // if we have just one error, try and recover
                Log.e(TAG, "Couldn't parseBoard deserialize postValue for board=" + boardCode, e);
            }
        }

        board.threads = threads.toArray(new ChanPost[threads.size()]);
        if (DEBUG) Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

    private void parseBoardCatalog(BufferedReader in) throws IOException {
    	List<ChanPost> threads = new ArrayList<ChanPost>();
    	board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
    	boolean firstLoad = false;
    	if (board != null && board.defData) {
    		// default board we should not use it
    		board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
    		firstLoad = true;
    	}

    	try {
	        ObjectMapper mapper = ChanHelper.getJsonMapper();
	        JsonParser jp = new MappingJsonFactory().createJsonParser(in);
	    	ChanHelper.configureJsonParser(jp);
	    	JsonToken current = jp.nextToken(); // will return JsonToken.START_ARRAY
	    	while (jp.nextToken() != JsonToken.END_ARRAY) {
        		current = jp.nextToken(); // should be JsonToken.START_OBJECT
        		JsonNode pageNode = jp.readValueAsTree();
    	        for (JsonNode threadValue : pageNode.path("threads")) { // iterate over threads
    	            try {
                        ChanPost post = mapper.readValue(threadValue, ChanPost.class);
                        if (post != null) {
                            post.board = boardCode;
                            threads.add(post);
                        }
                    }
                    catch (JsonMappingException e) { // if we have just one error, try and recover
                        Log.e(TAG, "Couldn't parseBoardCatalog deserialize threadValue for board=" + boardCode);
                    }
    	        }
	    	}
	    	jp.close();
		} catch (Exception e) {
			if (threads.size() == 0) {
				throw new JsonParseException("Board catalog parse error", null, e);
			}
		} finally {
			File boardFile = ChanFileStorage.getBoardFile(getBaseContext(), boardCode, pageNo);
			if (boardFile != null && boardFile.exists()) {
				FileUtils.deleteQuietly(boardFile);
			}
		}

        updateBoardData(threads, firstLoad);
        if (DEBUG) Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

	private void updateBoardData(List<ChanPost> threads, boolean firstLoad) {
		if (board != null) {
        	synchronized (board) {
        		board.defData = false;
        		if (firstLoad) {
        			board.threads = threads.toArray(new ChanPost[0]);
        		} else {
        			board.loadedThreads = threads.toArray(new ChanPost[0]);
        		}
        		board.updateCountersAfterLoad();
        	}
        }
	}

	private void setActionBarSubtitle() {
		final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId() != null) {
			final ChanActivityId activityId = activity.getChanActivityId();
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (activityId.activity == LastActivity.BOARD_ACTIVITY && board.link.equals(activityId.boardCode)) {
							((BoardActivity)activity).setActionBarTitle();
						}
					}
				});
			}
		}
	}

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(boardCode, pageNo, priority);
	}	
}
