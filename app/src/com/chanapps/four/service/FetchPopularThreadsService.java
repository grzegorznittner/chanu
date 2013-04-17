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

import org.apache.commons.io.IOUtils;

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

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchPopularThreadsService extends BaseChanService implements ChanIdentifiedService {
	private static final String BOARDS_4CHAN_ORG = "boards.4chan.org/";
	private static final String DIV_CLASS_BOX_OUTER_RIGHT_BOX = "<div class=\"box-outer right-box\">";
	private static final String ID_POPULAR_THREADS = "id=\"popular-threads\"";
	private static final String TAG = FetchPopularThreadsService.class.getSimpleName();
	private static final boolean DEBUG = true;

    private boolean force;
    private boolean backgroundLoad;
    
    public static void scheduleBoardFetch(Context context) {
        schedulePopularFetchService(context);
    }
    
    public static boolean schedulePopularFetchWithPriority(Context context) {
    	return scheduleBoardFetchWithPriority(context);
    }

    public static boolean schedulePopularFetchService(Context context) {
    	if (!boardNeedsRefresh(context, false)) {
        	return false;
        }
    	if (DEBUG) Log.i(TAG, "Start popular threads fetch service");
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        context.startService(intent);
        return true;
    }

    public static boolean scheduleBoardFetchWithPriority(Context context) {
    	if (!boardNeedsRefresh(context, true)) {
        	return false;
        }
        if (DEBUG) Log.i(TAG, "Start priority popular threads fetch service");
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
        return true;
    }
    
    public static void clearServiceQueue(Context context) {
        if (DEBUG) Log.i(TAG, "Clearing chan fetch service queue");
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        intent.putExtra(ChanHelper.CLEAR_FETCH_QUEUE, 1);
        context.startService(intent);
    }

	private static boolean boardNeedsRefresh(Context context, boolean forceRefresh) {
		FetchParams params = NetworkProfileManager.instance().getFetchParams();
        ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.POPULAR_BOARD_CODE);
        long now = new Date().getTime();
        if (board != null && !board.defData && board.lastFetched > 0) {
        	long refresh = forceRefresh ? params.forceRefreshDelay : params.refreshDelay;
        	if (now - board.lastFetched < refresh) {
        		if (DEBUG) Log.i(TAG, "Skiping board " + ChanBoard.POPULAR_BOARD_CODE + " fetch as it was fetched "
        				+ ((now - board.lastFetched) / 1000) + "s ago, refresh delay is " + (refresh / 1000) + "s" );
        		return false;
        	}
        }
        return true;
	}

    public FetchPopularThreadsService() {
   		super("chan_popular_threads_fetch");
   	}

    protected FetchPopularThreadsService(String name) {
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

		force = intent.getBooleanExtra(ChanHelper.FORCE_REFRESH, false);
		if (DEBUG) Log.i(TAG, "Handling popular threads fetch");
		handlePopularThreadsFetch();
	}
	
	private boolean isChanForegroundActivity() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		 // get the info from the currently running task
		List <ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

		if (DEBUG) Log.d(TAG, "foreground activity: " + taskInfo.get(0).topActivity.getClass().getSimpleName());

		ComponentName componentInfo = taskInfo.get(0).topActivity;
		return componentInfo != null && componentInfo.getPackageName().startsWith("com.chanapps");
	}

	private void handlePopularThreadsFetch() {
        HttpURLConnection tc = null;
		try {
			ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.POPULAR_BOARD_CODE);
			
			URL chanApi = new URL("http://www.4chan.org/");
			
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

            if (tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 during popular threads fetch");
                board.lastFetched = new Date().getTime();
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            } else if (contentType == null || !contentType.contains("text/html")) {
                // happens if 4chan is temporarily down or when access requires authentication to wifi router
                if (DEBUG) Log.i(TAG, "Wrong content type returned board=" + board + " contentType='" + contentType + "' responseCode=" + tc.getResponseCode() + " content=" + tc.getContent().toString());
            } else {
            	// long fileSize = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo, new InputStreamReader(tc.getInputStream()));
            	String response = IOUtils.toString(tc.getInputStream());
            	int fetchTime = (int)(new Date().getTime() - startTime);
            	
            	parsePopularThreads(board, response);
                
                if (DEBUG) Log.w(TAG, "Fetched and stored " + chanApi + " in " + fetchTime + "ms, size " + response.length());
                NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)response.length());
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

	private void parsePopularThreads(ChanBoard board, String response) {
		int startIdx = response.indexOf(ID_POPULAR_THREADS);
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			String boardName = null;
			String thread = null;
			String name = null;
			String thumbUrl = null;
			String tim = null;
			for (int i = 1; i < strings.length; i++) {
				int boardStart = strings[i].indexOf(BOARDS_4CHAN_ORG);
				int boardEnd = strings[i].indexOf("/", boardStart + BOARDS_4CHAN_ORG.length() + 1);
				boardName = strings[i].substring(BOARDS_4CHAN_ORG.length(), boardEnd);
			}
		}
	}

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(ChanBoard.POPULAR_BOARD_CODE, -1, force);
	}

}
