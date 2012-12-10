/**
 * 
 */
package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.component.BaseChanService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import javax.security.auth.login.LoginException;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanLoadService extends BaseChanService {
	private static final String TAG = ChanLoadService.class.getName();
	
	private static final long STORE_INTERVAL = 2000;  // it's in miliseconds

    public ChanLoadService() {
   		super("board");
   	}

    protected ChanLoadService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		int pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
        long threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		Log.i(TAG, "Handling board=" + boardCode + " threadNo=" + threadNo + " page=" + pageNo);

        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board not implemented yet");
            return;
        }

        BufferedReader in = null;
        HttpURLConnection tc = null;
		try {			
			URL chanApi = threadNo == 0
                ? new URL("http://api.4chan.org/" + boardCode + "/" + pageNo + ".json")
                : new URL("http://api.4chan.org/" + boardCode + "/res/" + threadNo + ".json");

            tc = (HttpURLConnection) chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength() + " code=" + tc.getResponseCode());
            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "Got 404 on next page, assuming last page");
                ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
                board.lastPage = true;
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            }
            else {
                in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
                if (threadNo == 0) {
                    parseBoard(boardCode, pageNo, in);
                }
                else {
                    parseThread(boardCode, threadNo, in);
                }
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

    protected void parseBoard(String boardCode, int pageNo, BufferedReader in) throws IOException {
    	long time = new Date().getTime();
    	ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
    	List<ChanPost> stickyPosts = new ArrayList<ChanPost>();
    	List<ChanPost> threads = new ArrayList<ChanPost>();
        if (pageNo > 0) { // merge instead of replace on subsequent page loads
            if (board.stickyPosts != null && board.stickyPosts.length > 0) {
                Collections.addAll(stickyPosts, board.stickyPosts);
            }
            if (board.threads != null && board.threads.length > 0) {
                Collections.addAll(threads, board.threads);
                Log.i(TAG, "Loaded " + threads.size() + " existing threads");
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
                	mergeThreads(post, stickyPosts);
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
                        else {
                            mergeThreads(post, threads);
                        }
                		first = false;
                	}
                	posts.add(post);
                }
                Log.v(TAG, post.toString());
            }
            if (thread != null) {
            	mergePosts(thread, posts);
            	ChanFileStorage.storeThreadData(getBaseContext(), thread);
            	if (new Date().getTime() - time > STORE_INTERVAL) {
            		board.threads = threads.toArray(new ChanPost[0]);
                    board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
                    ChanFileStorage.storeBoardData(getBaseContext(), board);
            	}
            }
            reader.endArray();
            reader.endObject();
        }

        if (threads.size() > 0) {
            board.threads = threads.toArray(new ChanPost[0]);
            board.stickyPosts = stickyPosts.toArray(new ChanPost[0]);
            board.lastPage = false;
            Log.i(TAG, "Now have " + threads.size() + " threads ");
        }
        else {
            Log.i(TAG, "No more threads, last page");
            board.lastPage = true;
        }
        ChanFileStorage.storeBoardData(getBaseContext(), board);
    }

    private void mergeThreads(ChanPost thread, List<ChanPost> threads) {
        boolean exists = false;
		for (ChanPost existingThread : threads) {
            if (thread.no == existingThread.no) {
                exists = true;
                copyUpdatedThreadFields(existingThread, thread);
                break;
            }
        }
        if (!exists) {
			threads.add(thread);
		}
	}

    private void mergePosts(ChanThread thread, List<ChanPost> posts) {
    	List<ChanPost> mergedPosts = new ArrayList<ChanPost>(Arrays.asList(thread.posts));
		for (ChanPost newPost : posts) {
			boolean exists = false;
			for (ChanPost p : thread.posts) {
				if (p.no == newPost.no) {
					exists = true;
                    copyUpdatedThreadFields(p, newPost);
				}
			}
			if (!exists) {
				mergedPosts.add(newPost);
			}
		}
		thread.posts = mergedPosts.toArray(new ChanPost[0]);
	}

    private void copyUpdatedThreadFields(ChanPost surviving, ChanPost from) {
        surviving.bumplimit = from.bumplimit;
        surviving.imagelimit = from.imagelimit;
        surviving.images = from.images;
        surviving.omitted_images = from.omitted_images;
        surviving.omitted_posts = from.omitted_posts;
        surviving.replies = from.replies;
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
            if (new Date().getTime() - time > STORE_INTERVAL) {
            	mergePosts(thread, posts);
                ChanFileStorage.storeThreadData(getBaseContext(), thread);
        	}
        }
        mergePosts(thread, posts);
        ChanFileStorage.storeThreadData(getBaseContext(), thread);
    }

}
