/**
 * 
 */
package com.chanapps.four.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchChanDataService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = FetchChanDataService.class.getSimpleName();
	private static final boolean DEBUG = false;

	protected static final long STORE_INTERVAL_MS = 2000;
    protected static final int MAX_THREAD_RETENTION_PER_BOARD = 100;
    
    protected static final long MIN_BOARD_FORCE_INTERVAL = 10000; // 10 sec
    protected static final long MIN_BOARD_FETCH_INTERVAL = 300000; // 5 min
    
    protected static final long MIN_THREAD_FORCE_INTERVAL = 10000; // 10 sec
    protected static final long MIN_THREAD_FETCH_INTERVAL = 300000; // 5 min
    
    protected static final int DEFAULT_READ_TIMEOUT = 15000; // 15 sec
    
    private String boardCode;
    private int pageNo;
    private long threadNo;
    private boolean boardHandling = true;
    private boolean force;
    
    private ChanBoard board;
    private ChanThread thread;

    public static void scheduleBoardFetch(Context context, String boardCode) {
        scheduleBoardFetchService(context, boardCode, 0);
    }
    
    public static boolean scheduleBoardFetchWithPriority(Context context, String boardCode) {
    	return scheduleBoardFetchWithPriority(context, boardCode, 0);
    }

    public static boolean scheduleBoardFetchService(Context context, String boardCode, int pageNo) {
    	if (!boardNeedsRefresh(context, boardCode, pageNo, false)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch service for " + boardCode + " page " + pageNo );

        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);        
        context.startService(intent);
        return true;
    }

    public static boolean scheduleBoardFetchWithPriority(Context context, String boardCode, int pageNo) {
    	if (!boardNeedsRefresh(context, boardCode, pageNo, true)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan priorty fetch service for " + boardCode + " page " + pageNo );
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
        return true;
    }
    
    public static boolean scheduleThreadFetch(Context context, String boardCode, long threadNo) {
    	if (!threadNeedsRefresh(context, boardCode, threadNo, false)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch service for " + boardCode + "/" + threadNo );
        
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        context.startService(intent);
        return true;
    }

    public static boolean scheduleThreadFetchWithPriority(Context context, String boardCode, long threadNo) {
    	if (!threadNeedsRefresh(context, boardCode, threadNo, true)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan priority fetch service for " + boardCode + "/" + threadNo );
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
        return true;
    }
    
    public static void clearServiceQueue(Context context) {
        if (DEBUG) Log.i(TAG, "Clearing chan fetch service queue");
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.CLEAR_FETCH_QUEUE, 1);
        context.startService(intent);
    }

	private static boolean boardNeedsRefresh(Context context, String boardCode, int pageNo, boolean forceRefresh) {
		FetchParams params = NetworkProfileManager.instance().getFetchParams();
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        long now = new Date().getTime();
        if (board != null && !board.defData && pageNo == 0 && board.lastFetched > 0) {
        	long refresh = forceRefresh ? params.forceRefreshDelay : params.refreshDelay;
        	if (now - board.lastFetched < refresh) {
        		if (DEBUG) Log.i(TAG, "Skiping board " + boardCode + " fetch as it was fetched "
        				+ ((now - board.lastFetched) / 1000) + "s ago, refresh delay is " + (refresh / 1000) + "s" );
        		return false;
        	}
        }
        return true;
	}

	private static boolean threadNeedsRefresh(Context context, String boardCode, long threadNo, boolean forceRefresh) {
		FetchParams params = NetworkProfileManager.instance().getFetchParams();
        ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
        long now = new Date().getTime();
        if (thread != null && !thread.defData && thread.lastFetched > 0) {
        	long refresh = forceRefresh ? params.forceRefreshDelay : params.refreshDelay;
        	if (now - thread.lastFetched < refresh) {
        		if (DEBUG) Log.i(TAG, "Skiping thread " + boardCode + "/" + threadNo + " fetch as it was fetched "
        				+ ((now - thread.lastFetched) / 1000) + "s ago, refresh delay is " + (refresh / 1000) + "s" );
        		return false;
        	}
        }
        return true;
	}
    
    public FetchChanDataService() {
   		super("chan_fetch");
   	}

    protected FetchChanDataService(String name) {
   		super(name);
   	}
    
	@Override
	protected void onHandleIntent(Intent intent) {
		if (!isChanForegroundActivity()) {
			return;
		}
		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		pageNo = intent.getIntExtra(ChanHelper.PAGE, 0);
		threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		boardHandling = threadNo == 0;

		force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);
		if (boardHandling) {
			if (DEBUG) Log.i(TAG, "Handling board " + boardCode + " page=" + pageNo);
	        handleBoard();
		} else {
			if (DEBUG) Log.i(TAG, "Handling thread " + boardCode + "/" + threadNo);
			handleThread();
		}
	}
	
	private boolean isChanForegroundActivity() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		 // get the info from the currently running task
		List <ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

		if (DEBUG) Log.d(TAG, "foreground activity: " + taskInfo.get(0).topActivity.getClass().getSimpleName());

		ComponentName componentInfo = taskInfo.get(0).topActivity;
		return componentInfo != null && componentInfo.getPackageName().startsWith("com.chanapps");
	}

	private void handleBoard() {
        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

        BufferedReader in = null;
        HttpURLConnection tc = null;
		try {
			board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
			if (board.defData) {
				board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
			}
			URL chanApi = new URL("http://api.4chan.org/" + boardCode + "/" + pageNo + ".json");
			if (DEBUG) Log.i(TAG, "Fetching board " + boardCode + " page " + pageNo);
			
    		long startTime = Calendar.getInstance().getTimeInMillis();
            tc = (HttpURLConnection) chanApi.openConnection();
            tc.setReadTimeout(NetworkProfileManager.instance().getFetchParams().readTimeout);
            if (board.lastFetched > 0) {
                tc.setIfModifiedSince(board.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (board.lastFetched > 0 && tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + board.lastFetched);
            	ChanFileStorage.storeBoardData(getBaseContext(), board);
            	return;
            }
            if (contentType == null || !contentType.contains("json")) {
            	throw new IOException("Wrong content type returned '" + contentType + "'");
            }
            
            board.lastFetched = new Date().getTime();
            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on next page, assuming last page at pageNo=" + pageNo);
                board.lastPage = true;
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            } else {
                in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
            	long fileSize = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo, in);
            	int fetchTime = (int)(new Date().getTime() - startTime);
                
                if (DEBUG) Log.w(TAG, "Fetched and stored " + chanApi + " in " + fetchTime + "ms, size " + fileSize);
                NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)fileSize);
            }
        } catch (IOException e) {
            //toastUI(R.string.board_service_couldnt_read);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            Log.e(TAG, "IO Error fetching Chan board json", e);
		} catch (Exception e) {
            //toastUI(R.string.board_service_couldnt_load);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.WRONG_DATA);
			Log.e(TAG, "Error fetching Chan board json", e);
		} finally {
			closeBufferAndConnection(in, tc);
		}
	}

	private void handleThread() {
        BufferedReader in = null;
        HttpURLConnection tc = null;
        if (threadNo == 0) {
            Log.e(TAG, "Thread loading must be done via the BoardLoadService");
        }
        if (boardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            Log.e(TAG, "Watching board must use ChanWatchlist instead of service");
            return;
        }

		try {
            thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
            long now = (new Date()).getTime();
            if (thread == null) {
            	if (DEBUG) Log.i(TAG, "Load thread data returned null, therefore service is terminating");
            	return;
            } else if (thread.defData) {
                thread = new ChanThread();
                thread.board = boardCode;
                thread.no = threadNo;
                thread.isDead = false;
            } else {
                if (thread.isDead) {
                    if (DEBUG) Log.i(TAG, "Dead thread retrieved from storage, therefore service is terminating");
                    return;
                }
            }

    		long startTime = Calendar.getInstance().getTimeInMillis();
            URL chanApi = new URL("http://api.4chan.org/" + boardCode + "/res/" + threadNo + ".json");
            tc = (HttpURLConnection) chanApi.openConnection();
            tc.setReadTimeout(NetworkProfileManager.instance().getFetchParams().readTimeout);
            if (thread.lastFetched > 0) {
                tc.setIfModifiedSince(thread.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (thread.lastFetched > 0 && tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + thread.lastFetched);
            	try {
            		ChanFileStorage.storeThreadData(getBaseContext(), thread);
            	} catch (Exception e) {
            		Log.w(TAG, "Could not update thread file", e);
            	}
            	return;
            }

            thread.lastFetched = now;
            if (contentType == null || !contentType.contains("json") || tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on thread, thread no longer exists");
                thread.isDead = true;
                try {
            		ChanFileStorage.storeThreadData(getBaseContext(), thread);
            	} catch (Exception e) {
            		Log.w(TAG, "Could not update thread file", e);
            	}
            } else if (thread.lastFetched > 0 && tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + thread.lastFetched);
            	return;
            } else {
                in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
                long fileSize = ChanFileStorage.storeThreadFile(getBaseContext(), boardCode, threadNo, in);
            	int fetchTime = (int)(new Date().getTime() - startTime);
            	
            	NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)fileSize);
            }
        } catch (IOException e) {
            //toastUI(R.string.board_service_couldnt_read);
        	NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            Log.e(TAG, "IO Error reading Chan thread json. " + e.getMessage(), e);
		} catch (Exception e) {
            //toastUI(R.string.board_service_couldnt_load);
			NetworkProfileManager.instance().failedFetchingData(this, Failure.WRONG_DATA);
			Log.e(TAG, "Error parsing Chan thread json. " + e.getMessage(), e);
		} finally {
			closeBufferAndConnection(in, tc);
		}
	}

	protected void closeBufferAndConnection(BufferedReader in, HttpURLConnection tc) {
		try {
			IOUtils.closeQuietly(in);
		    if (tc != null) {
		        tc.disconnect();
		    }
		} catch (Exception e) {
			Log.e(TAG, "Error closing reader", e);
		}
	}
	
	@Override
	public ChanActivityId getChanActivityId() {
		if (threadNo > 0) {
			return new ChanActivityId(boardCode, threadNo, force);
		} else {
			return new ChanActivityId(boardCode, pageNo, force);
		}
	}

}
