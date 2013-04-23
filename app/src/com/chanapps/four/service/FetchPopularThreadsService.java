/**
 * 
 */
package com.chanapps.four.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchPopularThreadsService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = FetchPopularThreadsService.class.getSimpleName();
	private static final boolean DEBUG = false;

    private boolean force;
    private boolean backgroundLoad;

    public static boolean scheduleBackgroundPopularFetchService(Context context) {
        return schedulePopularFetchService(context, false, true);
    }

    public static boolean schedulePopularFetchService(Context context) {
        return schedulePopularFetchService(context, false, false);
    }

    public static boolean schedulePopularFetchWithPriority(Context context) {
        return schedulePopularFetchService(context, true, false);
    }

    public static boolean schedulePopularFetchService(Context context, boolean priority, boolean backgroundLoad) {
        if (!boardNeedsRefresh(context, priority)) {
            if (DEBUG) Log.i(TAG, "Skipping priority popular threads fetch service refresh unneeded");
            return false;
        }
        if (DEBUG) Log.i(TAG, "Start popular threads fetch service priority=" + priority + " background=" + backgroundLoad);
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        if (priority)
            intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        if (backgroundLoad)
            intent.putExtra(ChanHelper.BACKGROUND_LOAD, true);
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
			URL chanApi = new URL("http://www.4chan.org/");
			
    		long startTime = Calendar.getInstance().getTimeInMillis();
            tc = (HttpURLConnection) chanApi.openConnection();
            FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
            tc.setReadTimeout(fetchParams.readTimeout);
            tc.setConnectTimeout(fetchParams.connectTimeout);

			ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.POPULAR_BOARD_CODE);
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
            	board.lastFetched = fetchTime;
            	ChanFileStorage.storeBoardData(getBaseContext(), board);
            	
            	ChanBoard latestBoard = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.LATEST_BOARD_CODE);
            	parseLatestPosts(latestBoard, response);
            	latestBoard.lastFetched = fetchTime;
            	ChanFileStorage.storeBoardData(getBaseContext(), latestBoard);
            	
            	ChanBoard imagesBoard = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.LATEST_IMAGES_BOARD_CODE);
            	parseLatestImages(imagesBoard, response);
            	imagesBoard.lastFetched = fetchTime;
            	ChanFileStorage.storeBoardData(getBaseContext(), imagesBoard);
                
                if (DEBUG) Log.w(TAG, "Fetched and stored " + chanApi + " in " + fetchTime + "ms, size " + response.length());
                NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)response.length());
                NetworkProfileManager.instance().finishedParsingData(this);
            }
        } catch (IOException e) {
            //toastUI(R.string.board_service_couldnt_read);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            Log.e(TAG, "IO Error fetching Chan web page", e);
		} catch (Exception e) {
            //toastUI(R.string.board_service_couldnt_load);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.WRONG_DATA);
			Log.e(TAG, "Error fetching Chan web page", e);
		} finally {
			closeConnection(tc);
		}
	}

	private static final String DIV_CLASS_BOX_OUTER_RIGHT_BOX = "class=\"box-outer right-box\"";
	private static final String ID_POPULAR_THREADS = "id=\"popular-threads\"";

	private void parseLatestImages(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf("id=\"recent-images\"");
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
					threads.add(parseThread(strings[i]));
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanPost[]{});
		}
		if (DEBUG) Log.i(TAG, "board " + board.name + " has " + board.threads.length + " threads\n\n");
	}

	private void parseLatestPosts(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf("id=\"recent-threads\"");
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
					threads.add(parseThread(strings[i]));
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanPost[]{});
		}
		if (DEBUG) Log.i(TAG, "board " + board.name + " has " + board.threads.length + " threads\n\n");
	}

	
	private void parsePopularThreads(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf(ID_POPULAR_THREADS);
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
					threads.add(parseThread(strings[i]));
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanPost[]{});
		}
		if (DEBUG) Log.i(TAG, "board " + board.name + " has " + board.threads.length + " threads\n\n");
	}

	private ChanThread parseThread(String threadStr) {
		ChanThread thread = new ChanThread();
		ParsableString str = new ParsableString();
		str.str = threadStr;
		str.pos = threadStr.indexOf("href");
		
		thread.board = str.extract(".4chan.org/", "/");
		thread.no = Long.parseLong(str.extract("res/", "#p"));
        String image = str.extract("&lt;a href=&quot;#&quot;&gt;", "&lt;/a&gt;");
		
		if (str.moveTo(")&lt;")) {
			String imageDesc = str.extractBefore("(");
			String parts[] = imageDesc != null ? imageDesc.split(",") : new String[]{};
			if (parts.length == 3) {
				try {
					String sizeStr[] = parts[0].split(" ");
					thread.fsize = Integer.parseInt(sizeStr[0]);
					String power = sizeStr.length < 2 || sizeStr[1] == null ? "" : sizeStr[1].toLowerCase();
					if (power.contains("mb")) {
						thread.fsize *= 1024 * 1024;
					} else if (power.contains("kb")) {
						thread.fsize *= 1024;
					}
				} catch (Exception e) {
                    if (DEBUG) Log.i(TAG, "Exception parsing popular thread filesize", e);
                    thread.fsize = 0;
				}
				try {
					String dimStr[] = parts[1].trim().split("x");
					thread.w = Integer.parseInt(dimStr[0]);
					thread.h = Integer.parseInt(dimStr[1]);
				} catch (Exception e) {
					if (DEBUG) Log.i(TAG, "Exception parsing popular thread dimensions", e);
				}
				try {
					String imgStr[] = parts[2].trim().split("\\.");
					thread.filename = imgStr[0];
					thread.ext = "." + imgStr[imgStr.length - 1];
				} catch (Exception e) {
                    if (DEBUG) Log.i(TAG, "Exception parsing popular thread filename", e);
                }
			}
		}
		String thumb = str.extract("thumbs.4chan.org/" + thread.board + "/thumb/", "&quot;");
		if (thumb != null) {
			int sIdx = thumb.indexOf("s");
			if (sIdx > 0) {
				thread.tim = Long.parseLong(thumb.substring(0, sIdx));
				thread.tn_w = 150;
				thread.tn_h = 150;
			}
		}
		if (str.moveTo("</a>")) {
			thread.sub = str.extractBefore(">");
		}
        if (str.moveTo("<blockquote>"))
            thread.com = str.extractBefore("</blockquote>");

        if (DEBUG) Log.i(TAG, "Board: " + thread.board + ", no: " + thread.no + ", tim: " + thread.tim + ", size: " + thread.fsize + ", " + thread.w + "x" + thread.h
				+ ",\n     img: " + thread.imageUrl() + ", thumb: " + thread.thumbnailUrl()
				+ ",\n     topic: " + thread.sub);
		return thread;
	}


	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(ChanBoard.POPULAR_BOARD_CODE, -1, force);
	}

	static class ParsableString {
		String str;
		int pos;
		
		public String extract(String start, String end) {
			int startIdx = str.indexOf(start, pos);
			if (startIdx < 0) {
				return null;
			}
			int endIdx = str.indexOf(end, startIdx + start.length());
			if (endIdx > -1) {
				pos = endIdx;
				return str.substring(startIdx + start.length(), endIdx);
			}
			return null;
		}
		
		public String extractBefore(String start) {
			int startIdx = str.lastIndexOf(start, pos);
			if (startIdx > -1) {
				return str.substring(startIdx + start.length(), pos);
			} else {
				return null;
			}
		}
		
		public boolean moveTo(String text) {
			int moveIdx = str.indexOf(text, pos);
			if (moveIdx > -1) {
				pos = moveIdx;
				return true;
			} else {
				return false;
			}
		}
	}
}
