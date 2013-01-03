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
import java.util.*;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import javax.security.auth.login.LoginException;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class BoardLoadService extends BaseChanService {

    protected static final String TAG = BoardLoadService.class.getSimpleName();
	
	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;


    private String boardCode;
    private int pageNo;
    private ChanBoard board;

    public static void startService(Context context, String boardCode) {
        startService(context, boardCode, 0);
    }

    private static void startService(Context context, String boardCode, int pageNo) {
        Log.i(TAG, "Start board load service for " + boardCode + " page " + pageNo );
        Intent intent = new Intent(context, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
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
            else if (pageNo > 0 && board.lastPage) {
                Log.i(TAG, "Board request after last page, therefore service is terminating");
                return;
            }

            tc = (HttpURLConnection) chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength() + " code=" + tc.getResponseCode());
            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "Got 404 on next page, assuming last page at pageNo=" + pageNo);
                board.lastPage = true;
            }
            else {
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
                BoardLoadService.startService(getBaseContext(), boardCode, pageNo);
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
