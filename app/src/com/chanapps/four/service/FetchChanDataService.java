/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
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

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.*;
import com.chanapps.four.service.profile.MobileProfile;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchChanDataService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = FetchChanDataService.class.getSimpleName();
	private static final boolean DEBUG = false;
    public static final String SECONDARY_THREAD_NO = "secondaryThreadNo";

    private String boardCode;
    private boolean boardCatalog;
    private int pageNo;
    private long threadNo;
    private long secondaryThreadNo;
    private boolean boardHandling = true;
    private boolean priority;
    private boolean backgroundLoad;
    
    private ChanBoard board;
    private ChanThread thread;

    public static boolean scheduleBoardFetch(Context context, String boardCode, boolean priority, boolean backgroundLoad) {
        return scheduleBoardFetch(context, boardCode, priority, backgroundLoad, 0);
    }

    public static boolean scheduleBoardFetch(Context context, String boardCode, boolean priority, boolean backgroundLoad,
                                             long threadNo) {
        if (ChanBoard.isPopularBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "Redirecting refresh request for /" + boardCode + "/ to popular fetch service");
            return FetchPopularThreadsService.schedulePopularFetchService(context, priority, backgroundLoad);
        }
        else if (ChanBoard.isVirtualBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "non-popular virtual board /" + boardCode + "/ received, skipping");
            return false;
        }

    	if (!ChanBoard.boardNeedsRefresh(context, boardCode, priority)) {
            if (DEBUG) Log.i(TAG, "Skipping not needing refresh normal board fetch service for "
                    + boardCode + " priority=" + priority);
            return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch board service /" + boardCode + "/ priority=" + priority);
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        intent.putExtra(ChanBoard.PAGE, -1);
        intent.putExtra(ChanBoard.BOARD_CATALOG, 1);
        intent.putExtra(PRIORITY_MESSAGE_FETCH, priority ? 1 : 0);
        if (backgroundLoad) {
            intent.putExtra(BACKGROUND_LOAD, true);
        }
        if (threadNo > 0) {
            intent.putExtra(SECONDARY_THREAD_NO, threadNo);
        }
        context.startService(intent);
        return true;
    }

    public static boolean scheduleThreadFetch(Context context, String boardCode, long threadNo, boolean priority, boolean backgroundLoad) {
    	if (!ChanThread.threadNeedsRefresh(context, boardCode, threadNo, priority)) {
            if (DEBUG) Log.i(TAG, "skipping refresh, thread doesn't need it for /" + boardCode + "/" + threadNo);
        	return false;
        }
        ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
        if (thread != null && thread.isDead) {
            if (DEBUG) Log.i(TAG, "scheduleThreadFetch /" + boardCode + "/" + threadNo + " exiting due to dead thread");
            return false;
        }
        if (DEBUG) Log.i(TAG, "Start chan fetch service for " + boardCode + "/" + threadNo
                + " priority=" + priority + " background=" + backgroundLoad);
        if (boardCode == null || threadNo == 0) {
        	Log.e(TAG, "Wrong params passed, boardCode: " + boardCode + " threadNo: " + threadNo,
        			new Exception("Locate caller and fix issue!"));
        }
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        intent.putExtra(PRIORITY_MESSAGE_FETCH, priority ? 1 : 0);
        if (backgroundLoad) {
            intent.putExtra(BACKGROUND_LOAD, true);
        }
        context.startService(intent);

        //optionallyDownloadAllImages(context, boardCode, threadNo); // really slow on large image threads
        return true;
    }

    private static void optionallyDownloadAllImages(Context context, String boardCode, long threadNo) {
        ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
        if (thread == null)
            return;
        if (!thread.defData)
            return;
        NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
        if (profile.getConnectionType() != NetworkProfile.Type.WIFI)
            return;
        NetworkProfile.Health health = profile.getConnectionHealth();
        if (health != NetworkProfile.Health.PERFECT)
            return;
        if (DEBUG) Log.i(TAG, "scheduleThreadFetch /" + boardCode + "/" + threadNo + " good Wifi, downloading images");
        ThreadImageDownloadService.startDownloadToBoardFolder(context, boardCode, threadNo);
    }

    public static void clearServiceQueue(Context context) {
        if (DEBUG) Log.i(TAG, "Clearing chan fetch service queue");
        Intent intent = new Intent(context, FetchChanDataService.class);
        intent.putExtra(CLEAR_FETCH_QUEUE, 1);
        context.startService(intent);
    }

    public FetchChanDataService() {
   		super("chan_fetch");
   	}

    protected FetchChanDataService(String name) {
   		super(name);
   	}
    
	@Override
	protected void onHandleIntent(Intent intent) {
        backgroundLoad = intent.getBooleanExtra(BACKGROUND_LOAD, false);
		if (!isChanForegroundActivity() && !backgroundLoad) {
            if (DEBUG)
                Log.i(TAG, "Not foreground activity, exiting");
			return;
		}

        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(this.getBaseContext());
        NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
        if (profile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                || profile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION) {
            if (DEBUG) Log.i(TAG, "No network connection, exiting");
            profile.onDataFetchFailure(this, Failure.NETWORK);
            return;
        }

		boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
		boardCatalog = intent.getIntExtra(ChanBoard.BOARD_CATALOG, 0) == 1;
		pageNo = boardCatalog ? -1 : intent.getIntExtra(ChanBoard.PAGE, 0);
		threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        secondaryThreadNo = intent.getLongExtra(SECONDARY_THREAD_NO, 0);
		boardHandling = threadNo == 0;
		priority = intent.getIntExtra(PRIORITY_MESSAGE_FETCH, 0) > 0;

        if (boardHandling) {
			if (DEBUG) Log.i(TAG, "Handling board " + boardCode + (boardCatalog ? " catalog" : " page=" + pageNo) + " priority=" + priority);
			handleBoard();
		} else {
			if (DEBUG) Log.i(TAG, "Handling thread " + boardCode + "/" + threadNo + " priority=" + priority);
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
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)) {
            Log.e(TAG, "Watchlist cannot be fetched from external site, only added and removed within the program");
            return;
        }
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)) {
            Log.e(TAG, "Favorites cannot be fetched from external site, only added and removed within the program");
            return;
        }

        HttpURLConnection tc = null;
		try {
			board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
			if (board != null && board.defData) {
				board = ChanBoard.getBoardByCode(getBaseContext(), boardCode);
				board.lastFetched = 0;
			}
	        File boardFile = ChanFileStorage.getBoardFile(getBaseContext(), boardCode, pageNo);
			if (board != null && !board.defData
                    && boardFile != null && boardFile.exists()
                    && (new Date().getTime() - boardFile.lastModified() < 10000) ) {
				if (DEBUG) Log.i(TAG, "Board file exists within last modified time, quiting fetch");
				return;
			}
			
			URL chanApi = null;
			if (boardCatalog) {
				chanApi = new URL("http://api.4chan.org/" + boardCode + "/catalog.json");
				if (DEBUG) Log.i(TAG, "Fetching board " + boardCode + " catalog priority=" + priority);
			} else {
				chanApi = new URL("http://api.4chan.org/" + boardCode + "/" + pageNo + ".json");
				if (DEBUG) Log.i(TAG, "Fetching board " + boardCode + " page " + pageNo);
			}
			
    		long startTime = Calendar.getInstance().getTimeInMillis();
            tc = (HttpURLConnection) chanApi.openConnection();
            FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
            tc.setReadTimeout(fetchParams.readTimeout);
            tc.setConnectTimeout(fetchParams.connectTimeout);
            
            if (board != null && board.lastFetched > 0 && !priority) {
            	if (DEBUG) Log.i(TAG, "IfModifiedSince set as last fetch happened "
        				+ ((startTime - board.lastFetched) / 1000) + "s ago");
                tc.setIfModifiedSince(board.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + board.lastFetched);
                int fetchTime = (int)(new Date().getTime() - startTime);
                NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, 0);
                return;
            }

            if (pageNo > 0 && tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on next page, assuming last page at pageNo=" + pageNo);
                board.lastFetched = new Date().getTime();
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            }
            else if (contentType == null || !contentType.contains("json")) {
                // happens if 4chan is temporarily down or when access requires authentication to wifi router
                if (DEBUG) Log.i(TAG, "Wrong content type returned board=" + board + " contentType='" + contentType
                        + "' responseCode=" + tc.getResponseCode() + " content=" + tc.getContent().toString());
            }
            else {
                board.lastFetched = new Date().getTime();
                long fileSize = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo,
                        new InputStreamReader(tc.getInputStream()));
            	int fetchTime = (int)(board.lastFetched - startTime);
                
                if (DEBUG) Log.w(TAG, "Fetched and stored " + chanApi + " in " + fetchTime + "ms, size " + fileSize);
                if (DEBUG) Log.i(TAG, "Calling finishedFetchingData priority=" + priority);
                /*
                final ChanActivityId activityId = getChanActivityId();
                final Context context = getApplicationContext();
                final ChanIdentifiedService service = new ChanIdentifiedService() {
                    @Override
                    public ChanActivityId getChanActivityId() {
                        return activityId;
                    }
                    @Override
                    public Context getApplicationContext() {
                        return context;
                    }
                };
                */
                NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)fileSize);
            }
        } catch (IOException e) {
            //toastUI(R.string.board_service_couldnt_read);
            Log.e(TAG, "IO Error fetching Chan board json", e);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
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
            Log.e(TAG, "Board-level loading must be done via the BoardLoadService");
            return;
        }
        else if (boardCode.equals(ChanBoard.WATCHLIST_BOARD_CODE)) {
            Log.e(TAG, "Watchlist should not be fetched");
            return;
        }
        else if (boardCode.equals(ChanBoard.FAVORITES_BOARD_CODE)) {
            Log.e(TAG, "Favorites should not be fetched");
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
            if (thread.lastFetched > 0 && !priority) {
            	if (DEBUG) Log.i(TAG, "IfModifiedSince set as last fetch happened "
        				+ ((startTime - thread.lastFetched) / 1000) + "s ago");
                tc.setIfModifiedSince(thread.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + thread.lastFetched);
                NetworkProfileManager.instance().failedFetchingData(this, Failure.THREAD_UNMODIFIED);
                return;
            }

            thread.lastFetched = now;
            if (tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                if (DEBUG) Log.i(TAG, "Got 404 on thread, thread no longer exists, setting dead thread");

                // store dead status for thread
                thread.isDead = true;
                if (thread.posts != null && thread.posts[0] != null)
                    thread.posts[0].isDead = true;
                if (DEBUG) Log.i(TAG, "After handleBoard dead thread calling storeThreadData for /" + thread.board + "/" + thread.no);
                ChanFileStorage.storeThreadData(getBaseContext(), thread);

                // store dead status into board thread record
                ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
                if (board != null && board.threads != null) {
                    for (int i = 0; i < board.threads.length; i++) {
                        if (board.threads[i] != null && board.threads[i].no == threadNo) {
                            board.threads[i].isDead = true;
                            ChanFileStorage.storeBoardData(getBaseContext(), board);
                            break;
                        }
                    }
                }
                
                NetworkProfileManager.instance().failedFetchingData(this, Failure.DEAD_THREAD);
                return;
            } else if (contentType == null || !contentType.contains("json")) {
                NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
                return;
            }
            else {
                long fileSize = ChanFileStorage.storeThreadFile(getBaseContext(), boardCode, threadNo, new InputStreamReader(tc.getInputStream()));
                int fetchTime = (int)(new Date().getTime() - startTime);
                final ChanActivityId activityId = getChanActivityId();
                final Context context = getApplicationContext();
                final ChanIdentifiedService service = new ChanIdentifiedService() {
                    @Override
                    public ChanActivityId getChanActivityId() {
                        return activityId;
                    }
                    @Override
                    public Context getApplicationContext() {
                        return context;
                    }
                };
                NetworkProfileManager.instance().finishedFetchingData(service, fetchTime, (int)fileSize);
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
        ChanActivityId id;
		if (threadNo > 0) {
			id = new ChanActivityId(boardCode, threadNo, priority);
		} else {
			id = new ChanActivityId(boardCode, pageNo, priority);
		}
        if (secondaryThreadNo > 0)
            id.secondaryThreadNo = secondaryThreadNo;
        return id;
	}

}
