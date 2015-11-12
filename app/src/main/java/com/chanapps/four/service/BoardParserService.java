/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.JacksonNonBlockingObjectMapperFactory;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardParserService extends BaseChanService implements ChanIdentifiedService {

    protected static final String TAG = BoardParserService.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 200;
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    static {
        MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setDateFormat(new SimpleDateFormat("MMM d, yyyy h:mm:ss aaa")); // "Jan 15, 2013 10:16:20 AM"
    }

    private String boardCode;
    private boolean boardCatalog;
    private int pageNo;
    private boolean priority;
    private long secondaryThreadNo = 0;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, int pageNo, boolean priority, long secondaryThreadNo) {
    	if (ChanBoard.isVirtualBoard(boardCode)) {
    		return;
    	}
        if (DEBUG) Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " priority=" + priority );
        Intent intent = new Intent(context, BoardParserService.class);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        intent.putExtra(ChanBoard.BOARD_CATALOG, pageNo == -1 ? 1 : 0);
        intent.putExtra(ChanBoard.PAGE, pageNo);
        if (priority)
	        intent.putExtra(PRIORITY_MESSAGE_FETCH, 1);
        if (secondaryThreadNo > 0)
            intent.putExtra(FetchChanDataService.SECONDARY_THREAD_NO, secondaryThreadNo);
        context.startService(intent);
    }
    
    public BoardParserService() {
   		super("board");
   	}

    protected BoardParserService(String name) {
   		super(name);
   	}

    public static void configureJsonParser(JsonParser jp) throws IOException, JsonParseException {
        jp.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        jp.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        jp.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        jp.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        jp.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        jp.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    /*
    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    public static final Orientation getOrientation(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (metrics.widthPixels > metrics.heightPixels) {
            return Orientation.LANDSCAPE;
        }
        else {
            return Orientation.PORTRAIT;
        }
    }
    */
    public static ObjectMapper getJsonMapper() {
        return MAPPER;
    }

    @Override
	protected void onHandleIntent(Intent intent) {
		boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
		boardCatalog = intent.getIntExtra(ChanBoard.BOARD_CATALOG, 0) == 1;
		pageNo = intent.getIntExtra(ChanBoard.PAGE, 0);
        priority = intent.getIntExtra(PRIORITY_MESSAGE_FETCH, 0) > 0;
		secondaryThreadNo = intent.getLongExtra(FetchChanDataService.SECONDARY_THREAD_NO, 0);
        if (DEBUG) Log.i(TAG, "Handling board=" + boardCode + " priority=" + priority);

        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();

            File boardFile = ChanFileStorage.getBoardFile(context, boardCode, pageNo);
            if (boardCatalog) {
            	parseBoardCatalog(boardFile);
            } else {
            	parseBoard(boardFile);
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
            //setActionBarSubtitle();

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
        	NetworkProfileManager.instance().failedParsingData(this, Failure.WRONG_DATA);
            Log.e(TAG, "IO Error reading Chan board json", e);
		}
	}

    private void parseBoard(File in) throws IOException {
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

        ObjectMapper mapper = getJsonMapper();
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

        board.threads = threads.toArray(new ChanThread[threads.size()]);
        if (DEBUG) Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

    private void parseBoardCatalog(File in) throws IOException {
    	List<ChanThread> threads = new ArrayList<ChanThread>();
    	board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
    	boolean firstLoad = false;
    	if (board != null && board.defData) {
    		// default board we should not use it
    		board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
    		firstLoad = true;
    	}

    	try {
	        ObjectMapper mapper = getJsonMapper();
	        JsonParser jp = new MappingJsonFactory().createJsonParser(in);
	    	configureJsonParser(jp);
	    	JsonToken current = jp.nextToken(); // will return JsonToken.START_ARRAY
	    	while (jp.nextToken() != JsonToken.END_ARRAY) {
        		current = jp.nextToken(); // should be JsonToken.START_OBJECT
        		JsonNode pageNode = jp.readValueAsTree();
    	        for (JsonNode threadValue : pageNode.path("threads")) { // iterate over threads
    	            try {
                        ChanThread thread = mapper.readValue(threadValue, ChanThread.class);
                        if (DEBUG) Log.i(TAG, "thread sub=" + thread.sub + " thumb=" + thread.tn_w + "x" + thread.tn_h
                                + " full=" + thread.w + "x" + thread.h + " com=" + thread.com);
                        if (thread != null) {
                            thread.board = boardCode;
                            threads.add(thread);
                            if (DEBUG) Log.i(TAG, "thread lastReplies=" + thread.lastReplies
                                    + " len=" + (thread.lastReplies == null ? 0 : thread.lastReplies.length));
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

	private void updateBoardData(List<ChanThread> threads, boolean firstLoad) {
		if (board == null)
            return;

        final Context context = getBaseContext();
        final String boardCode = board.link;
        final Set<Long> threadNos = new HashSet<Long>();

        synchronized (board) {
            board.defData = false;
            if (firstLoad) {
                board.threads = threads.toArray(new ChanThread[0]);
                board.loadedThreads = threads.toArray(new ChanThread[0]);
            } else {
                board.loadedThreads = threads.toArray(new ChanThread[0]);
            }
            board.updateCountersAfterLoad(context);
            // create a map for lookup of watched threads
            for (ChanThread thread : board.loadedThreads) {
                threadNos.add(thread.no);
            }
        }

        updateWatchlist(context, boardCode, threadNos);
    }

    protected void updateWatchlist(final Context context, final String boardCode, final Set<Long> threadNos) {
        // mark watched threads no longer in board as dead
        boolean atLeastOne = false;
        ChanBoard watchlist = ChanFileStorage.loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        for (ChanThread thread : watchlist.threads) {
            if (boardCode.equals(thread.board) && !threadNos.contains(thread.no)) {
                thread.isDead = true;
                atLeastOne = true;
            }
        }

        // remove watched threads no longer in board (and thus dead)
        if (atLeastOne) {
            try {
                if (PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true))
                    ChanFileStorage.cleanDeadWatchedThreads(context);
            }
            catch (IOException e) {
                Log.e(TAG, "Exception cleaning dead threads for /" + boardCode + "/", e);
            }
            BoardActivity.refreshWatchlist(context);
        }
    }

    /*
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
							((BoardActivity)activity).handleUpdatedThreads();
						}
					}
				});
			}
		}
	}
    */
	@Override
	public ChanActivityId getChanActivityId() {
		ChanActivityId id = new ChanActivityId(boardCode, pageNo, priority);
        if (secondaryThreadNo > 0)
            id.secondaryThreadNo = secondaryThreadNo;
        return id;
	}

    @Override
    public String toString() {
        return "BoardParserService: " + getChanActivityId();
    }

}
