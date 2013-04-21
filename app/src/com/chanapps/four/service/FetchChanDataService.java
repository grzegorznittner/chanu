/**
 * 
 */
package com.chanapps.four.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;
import com.chanapps.four.service.profile.NoConnectionProfile;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchChanDataService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = FetchChanDataService.class.getSimpleName();
	private static final boolean DEBUG = true;

    private String boardCode;
    private boolean boardCatalog;
    private int pageNo;
    private long threadNo;
    private boolean boardHandling = true;
    private boolean force;
    private boolean backgroundLoad;
    
    private ChanBoard board;
    private ChanThread thread;

    public static void scheduleBoardFetch(Context context, String boardCode) {
        scheduleBoardFetchService(context, boardCode, -1);
    }
    
    public static boolean scheduleBoardFetchWithPriority(Context context, String boardCode) {
    	return scheduleBoardFetchWithPriority(context, boardCode, -1);
    }

    public static boolean scheduleBoardFetchService(Context context, String boardCode, int pageNo) {
    	if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode)) {
    		return FetchPopularThreadsService.schedulePopularFetchService(context);
    	}
    	
    	if (!boardNeedsRefresh(context, boardCode, pageNo, false)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch service for " + boardCode + " page " + pageNo );
        if (boardCode == null) {
        	Log.e(TAG, "Wrong params passed, boardCode: " + boardCode,
        			new Exception("Locate caller and fix issue!"));
        }
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        if (pageNo == -1) {
        	intent.putExtra(ChanHelper.BOARD_CATALOG, 1);
            intent.putExtra(ChanHelper.PAGE, -1);
        } else {
        	intent.putExtra(ChanHelper.PAGE, pageNo);
        }
        context.startService(intent);
        return true;
    }

    public static boolean scheduleBoardFetchWithPriority(Context context, String boardCode, int pageNo) {
    	if (!boardNeedsRefresh(context, boardCode, pageNo, true)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan priorty fetch service for " + boardCode + " page " + pageNo );
        if (boardCode == null) {
        	Log.e(TAG, "Wrong params passed, boardCode: " + boardCode,
        			new Exception("Locate caller and fix issue!"));
        }
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        if (pageNo == -1) {
        	intent.putExtra(ChanHelper.BOARD_CATALOG, 1);
            intent.putExtra(ChanHelper.PAGE, -1);
        } else {
        	intent.putExtra(ChanHelper.PAGE, pageNo);
        }
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
        return true;
    }
    
    public static boolean scheduleThreadFetch(Context context, String boardCode, long threadNo) {
    	if (!threadNeedsRefresh(context, boardCode, threadNo, false)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch service for " + boardCode + "/" + threadNo );
        if (boardCode == null || threadNo == 0) {
        	Log.e(TAG, "Wrong params passed, boardCode: " + boardCode + " threadNo: " + threadNo,
        			new Exception("Locate caller and fix issue!"));
        }
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
        if (boardCode == null || threadNo == 0) {
        	Log.e(TAG, "Wrong params passed, boardCode: " + boardCode + " threadNo: " + threadNo,
        			new Exception("Locate caller and fix issue!"));
        }
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
        return true;
    }

    public static boolean scheduleThreadFetchAfterPost(Context context, String boardCode, long threadNo) {
        // after successful post, we should always fetch fresh
        if (DEBUG) Log.i(TAG, "Start chan after post fetch service for " + boardCode + "/" + threadNo );
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        intent.putExtra(ChanHelper.FORCE_REFRESH, true);
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
        if (board != null && !board.defData && pageNo == -1 && board.lastFetched > 0) {
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
        if (thread != null && !thread.defData && thread.lastFetched > 0 && !thread.isDead && thread.closed <= 0) {
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
        backgroundLoad = intent.getBooleanExtra(ChanHelper.BACKGROUND_LOAD, false);
		if (!isChanForegroundActivity() && !backgroundLoad) {
            if (DEBUG)
                Log.i(TAG, "Not foreground activity, exiting");
			return;
		}

        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(this.getBaseContext());
        NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
        if (profile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                || profile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION
                || profile.getConnectionHealth() == NetworkProfile.Health.BAD) {
            if (DEBUG) Log.i(TAG, "No network connection, exiting");
            return;
        }

		boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		boardCatalog = intent.getIntExtra(ChanHelper.BOARD_CATALOG, 0) == 1;
		pageNo = boardCatalog ? -1 : intent.getIntExtra(ChanHelper.PAGE, 0);
		threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		boardHandling = threadNo == 0;

		force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false)
            || (intent.getIntExtra(ChanHelper.PRIORITY_MESSAGE, 0) > 0);
		if (boardHandling) {
			if (DEBUG) Log.i(TAG, "Handling board " + boardCode + (boardCatalog ? " catalog" : " page=" + pageNo));
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

        HttpURLConnection tc = null;
		try {
			board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
			if (board != null && board.defData) {
				board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
				board.lastFetched = 0;
			}
			URL chanApi = null;
			if (boardCatalog) {
				chanApi = new URL("http://api.4chan.org/" + boardCode + "/catalog.json");
				if (DEBUG) Log.i(TAG, "Fetching board " + boardCode + " catalog.");
			} else {
				chanApi = new URL("http://api.4chan.org/" + boardCode + "/" + pageNo + ".json");
				if (DEBUG) Log.i(TAG, "Fetching board " + boardCode + " page " + pageNo);
			}
			
    		long startTime = Calendar.getInstance().getTimeInMillis();
            tc = (HttpURLConnection) chanApi.openConnection();
            FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
            tc.setReadTimeout(fetchParams.readTimeout);
            tc.setConnectTimeout(fetchParams.connectTimeout);
            
            if (board != null && board.lastFetched > 0 && !force) {
            	if (DEBUG) Log.i(TAG, "IfModifiedSince set as last fetch happened "
        				+ ((startTime - board.lastFetched) / 1000) + "s ago");
                tc.setIfModifiedSince(board.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + board.lastFetched);
            	return;
            }

            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on next page, assuming last page at pageNo=" + pageNo);
                board.lastFetched = new Date().getTime();
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            }
            else if (contentType == null || !contentType.contains("json")) {
                // happens if 4chan is temporarily down or when access requires authentication to wifi router
                if (DEBUG) Log.i(TAG, "Wrong content type returned board=" + board + " contentType='" + contentType + "' responseCode=" + tc.getResponseCode() + " content=" + tc.getContent().toString());
            }
            else {
            	long fileSize = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo, new InputStreamReader(tc.getInputStream()));
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
			closeConnection(tc);
		}
	}

	private void handleThread() {
        HttpURLConnection tc = null;
        if (threadNo == 0) {
            Log.e(TAG, "Thread loading must be done via the BoardLoadService");
            return;
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
                thread.lastFetched = 0;
            } else {
                if (thread.isDead && thread.posts.length == thread.replies) {
                    if (DEBUG) Log.i(TAG, "Dead thread retrieved from storage, therefore service is terminating");
                    return;
                }
            }

    		long startTime = Calendar.getInstance().getTimeInMillis();
            URL chanApi = new URL("http://api.4chan.org/" + boardCode + "/res/" + threadNo + ".json");
            tc = (HttpURLConnection) chanApi.openConnection();
            tc.setReadTimeout(NetworkProfileManager.instance().getFetchParams().readTimeout);
            if (thread.lastFetched > 0 && !force) {
            	if (DEBUG) Log.i(TAG, "IfModifiedSince set as last fetch happened "
        				+ ((startTime - thread.lastFetched) / 1000) + "s ago");
                tc.setIfModifiedSince(thread.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + thread.lastFetched);
            	return;
            }

            thread.lastFetched = now;
            if (tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on thread, thread no longer exists");
                thread.isDead = true;
                ChanFileStorage.storeThreadData(getBaseContext(), thread);
                // now refresh if we marked the current thread as dead
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                if (activity == null)
                    return;
                ChanActivityId id = activity.getChanActivityId();
                if (id == null)
                    return;
                if (boardCode.equals(id.boardCode) && threadNo == id.threadNo) {
                    activity.getChanHandler().sendEmptyMessageDelayed(0, BoardActivity.LOADER_RESTART_INTERVAL_SHORT_MS);
                    NetworkProfileManager.instance().makeToast(getString(R.string.mark_dead_thread));
                }
                return;
            } else if (contentType == null || !contentType.contains("json")) {
                NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
                return;
            }
            else {
                long fileSize = ChanFileStorage.storeThreadFile(getBaseContext(), boardCode, threadNo, new InputStreamReader(tc.getInputStream()));
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
			closeConnection(tc);
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
