/**
 * 
 */
package com.chanapps.four.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
public class BoardLoadService extends BaseChanService {

    protected static final String TAG = BoardLoadService.class.getSimpleName();
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;
    protected static final long MIN_BOARD_FORCE_INTERVAL = 10000; // 10 sec
    protected static final long MIN_BOARD_FETCH_INTERVAL = 300000; // 5 min


    private String boardCode;
    private int pageNo;
    private boolean force;
    private ChanBoard board;

    public static void startService(Context context, String boardCode, boolean force) {
        startService(context, boardCode, 0, force);
    }

    private static void startService(Context context, String boardCode, int pageNo, boolean force) {
        Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + force );
        Intent intent = new Intent(context, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        if (force) {
            intent.putExtra(ChanHelper.FORCE_REFRESH, force);
        }
        context.startService(intent);
    }

    private static void startServiceWithPriority(Context context, String boardCode, int pageNo, boolean force) {
        Log.i(TAG, "Start board load service for board=" + boardCode + " page=" + pageNo + " force=" + force );
        Intent intent = new Intent(context, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        if (force) {
            intent.putExtra(ChanHelper.FORCE_REFRESH, force);
        }
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
        BufferedReader in = null;
        HttpURLConnection tc = null;
		try {
            URL chanApi = new URL("http://api.4chan.org/" + boardCode + "/" + pageNo + ".json");

            board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
            Log.w(TAG, "Loading " + chanApi + " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();
            
            if (pageNo == 0) {
                Log.i(TAG, "page 0 request, therefore resetting board.lastPage to false");
                board.lastPage = false;
            }

            if (pageNo > 0 && board.lastPage) {
                Log.i(TAG, "Board request after last page, therefore service is terminating");
                return;
            }

            long now = (new Date()).getTime();
            if (pageNo == 0) {
                Log.i(TAG, "page 0 request, therefore resetting board.lastPage to false");
                board.lastPage = false;
                long interval = now - board.lastFetched;
                if (force && interval < MIN_BOARD_FORCE_INTERVAL) {
                    Log.i(TAG, "board is forced but interval=" + interval + " is less than min=" + MIN_BOARD_FORCE_INTERVAL + " so exiting");
                    return;
                }
                if (!force && interval < MIN_BOARD_FETCH_INTERVAL) {
                    Log.i(TAG, "board interval=" + interval + " less than min=" + MIN_BOARD_FETCH_INTERVAL + " and not force thus exiting service");
                    return;
                }
            }

            tc = (HttpURLConnection) chanApi.openConnection();
            if (board.lastFetched > 0)
                tc.setIfModifiedSince(board.lastFetched);
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength() + " code=" + tc.getResponseCode());
            board.lastFetched = now;
            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "Got 404 on next page, assuming last page at pageNo=" + pageNo);
                board.lastPage = true;
            } else {
                in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
                File boardFile = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo, in);
                
                Log.w(TAG, "Fetched and stored " + chanApi + " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
                startTime = Calendar.getInstance().getTimeInMillis();
                
                parseBoard(new BufferedReader(new FileReader(boardFile)));
            }

            Log.w(TAG, "Parsed " + chanApi + " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
            startTime = Calendar.getInstance().getTimeInMillis();

            ChanFileStorage.storeBoardData(getBaseContext(), board);

            Log.w(TAG, "Stored " + chanApi + " in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");

            if (!board.lastPage) {
                pageNo++;
                Log.i(TAG, "Starting serivce to load next page for " + boardCode + " page " + pageNo);
                BoardLoadService.startService(getBaseContext(), boardCode, pageNo, force);
            }

        } catch (IOException e) {
            toastUI(R.string.board_service_couldnt_read);
            Log.e(TAG, "IO Error reading Chan board json", e);
		} catch (Exception e) {
            toastUI(R.string.board_service_couldnt_load);
			Log.e(TAG, "Error parsing Chan board json", e);
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

    private void parseBoard(BufferedReader in) throws IOException {
    	long time = new Date().getTime();
    	List<ChanPost> stickyPosts = new ArrayList<ChanPost>();
    	List<ChanPost> threads = new ArrayList<ChanPost>();

        if (pageNo != 0) { // preserve existing threads on subsequent page loads
            if (board.stickyPosts != null && board.stickyPosts.length > 0) {
                Collections.addAll(stickyPosts, board.stickyPosts);
                Log.i(TAG, "Added " + board.stickyPosts.length + " sticky posts from storage");
            }
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
            boolean isSticky = false;
            while (reader.hasNext()) { // first object is the thread post, spin over rest
                ChanPost post = gson.fromJson(reader, ChanPost.class);
                post.board = boardCode;
                if (post.sticky > 0 || isSticky) {
                    post.mergeIntoThreadList(stickyPosts);
                	isSticky = true;
                } else {
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
                }
                Log.v(TAG, post.toString());
            }
            if (thread != null) {
                thread.mergePosts(posts);
            	ChanFileStorage.storeThreadData(getBaseContext(), thread);
            	if (new Date().getTime() - time > STORE_INTERVAL_MS) {
            		board.threads = threads.toArray(new ChanPost[0]);
                    board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
                    ChanFileStorage.storeBoardData(getBaseContext(), board);
            	}
            }
            reader.endArray();
            reader.endObject();
        }

        board.threads = threads.toArray(new ChanPost[0]);
        board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
        Log.i(TAG, "Now have " + threads.size() + " threads ");
    }

}
