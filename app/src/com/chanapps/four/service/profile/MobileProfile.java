package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.FullScreenImageActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

public class MobileProfile extends AbstractNetworkProfile {
	private static final String TAG = "";
	private static final boolean DEBUG = true;
	
	private static final int MIN_THREADS_PER_BOARD = 20;
	
	private String networkType = "3G";
	
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD, new FetchParams(600000L, 240000L, 20000));  // 10 min
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(300000L, 200000L, 20000));  // 5 min
		REFRESH_TIME.put(Health.SLOW, new FetchParams(180000L, 120000L, 20000));  // 3 min
		REFRESH_TIME.put(Health.GOOD, new FetchParams(120000L, 80000L, 12000));  // 2 min
		REFRESH_TIME.put(Health.PERFECT, new FetchParams(60000L, 40000L, 8000));  // 1 min
	}
	
	@Override
	public Type getConnectionType() {
		return Type.MOBILE;
	}

	@Override
	public FetchParams getFetchParams() {
		return REFRESH_TIME.get(getConnectionHealth());
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}

	@Override
	public Health getDefaultConnectionHealth() {
		if ("2G".equalsIgnoreCase(networkType)) {
			return Health.VERY_SLOW;
		} else {
			return Health.SLOW;
		}
	}
	
	@Override
	public void onProfileActivated(Context context) {
		super.onProfileActivated(context);
		
		Health health = getConnectionHealth();
		if (health != Health.BAD) {
			ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
			ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
			if (activityId != null) {
				if (activityId.activity == LastActivity.THREAD_ACTIVITY) {
					makeToast("Reloading thread ...");
					FetchChanDataService.scheduleThreadFetch(context, activityId.boardCode, activityId.threadNo);
				} else if (activityId.activity == LastActivity.BOARD_ACTIVITY) {
					makeToast("Reloading board ...");
					FetchChanDataService.scheduleBoardFetch(context, activityId.boardCode);
				} else if (activityId.activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
					Handler handler = activity.getChanHandler();
					if (handler != null) {
						makeToast("Loading image ...");
						handler.sendEmptyMessageDelayed(FullScreenImageActivity.START_DOWNLOAD_MSG, 100);
					}
				} else if (activityId.activity == ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY) {
					if (health != Health.VERY_SLOW) {
						/*
						makeToast("Preloading boards a, b and s");
						FetchChanDataService.scheduleBoardFetch(context, "a");
						FetchChanDataService.scheduleBoardFetch(context, "b");
						FetchChanDataService.scheduleBoardFetch(context, "s");
						*/
					} else {
						makeToast("No preloading " + health);
					}
				}
			}
		} else {
			makeToast("No auto refresh");
		}
	}

	@Override
	public void onProfileDeactivated(Context context) {
		super.onProfileDeactivated(context);
	}

	@Override
	public void onApplicationStart(Context context) {
		super.onApplicationStart(context);
		
		Health health = getConnectionHealth();
		if (health != Health.BAD && health != Health.VERY_SLOW) {
			/*
			makeToast("Preloading boards a, b and s");
			FetchChanDataService.scheduleBoardFetch(context, "a");
			FetchChanDataService.scheduleBoardFetch(context, "b");
			FetchChanDataService.scheduleBoardFetch(context, "s");
			*/
		} else {
			makeToast("No preloading " + health);
		}
	}

	@Override
	public void onBoardSelectorSelected(Context context) {
		super.onBoardSelectorSelected(context);

		Health health = getConnectionHealth();
		if (health != Health.BAD && health != Health.VERY_SLOW) {
			/*
			makeToast("Preloading boards a, b and s");
			FetchChanDataService.scheduleBoardFetch(context, "a");
			FetchChanDataService.scheduleBoardFetch(context, "b");
			FetchChanDataService.scheduleBoardFetch(context, "s");
			*/
		} else {
			makeToast("No preloading " + health);
		}
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
		
		FetchChanDataService.scheduleBoardFetchWithPriority(context, board);
//		Health health = getConnectionHealth();
//		if (health == Health.GOOD || health == Health.PERFECT) {
//			ChanBoard boardObj = ChanFileStorage.loadBoardData(context, board);
//			int threadPrefechCounter = health == Health.GOOD ? 3 : 7;
//			if (boardObj != null) {
//				for(ChanPost post : boardObj.threads) {
//					if (threadPrefechCounter <= 0) {
//						break;
//					}
//					if (post.closed == 0 && post.sticky == 0 && post.replies > 5 && post.images > 1) {
//						threadPrefechCounter--;
//						FetchChanDataService.scheduleThreadFetch(context, board, post.no);
//					}
//				}
//			}
//		}
	}

	@Override
	public void onBoardRefreshed(Context context, String board) {
		super.onBoardRefreshed(context, board);
		
		if (FetchChanDataService.scheduleBoardFetchWithPriority(context, board)) {
			makeToast("Refreshing board ...");
		} else {
			makeToast("Board is fresh");
		}
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		Log.d(TAG, "onThreadSelected", new Exception("onThreadSelected called"));
		super.onThreadSelected(context, board, threadId);
		
		FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId);
	}
	
	@Override
	public void onThreadRefreshed(Context context, String board, long threadId) {
		super.onThreadRefreshed(context, board, threadId);
		
		if (FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId)) {
			makeToast("Refreshing thread ...");
		} else {
			makeToast("Thread is fresh");
		}
	}

	@Override
	public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
		// default behaviour is to parse properly loaded item
		super.onDataFetchSuccess(service, time, size);
	}

	@Override
	public void onDataParseSuccess(ChanIdentifiedService service) {
		super.onDataParseSuccess(service);
		
		ChanActivityId data = service.getChanActivityId();
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();
		
		if (data.threadNo == 0) {
			// board fetching
			boolean boardActivity = currentActivityId != null
					&& currentActivityId.boardCode != null
					&& currentActivityId.boardCode.equals(data.boardCode);
			makeToast("Loaded data for board " + data.boardCode);
			ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
			if (board.defData) {
				// board data corrupted, we need to reload it
				Log.w(TAG, "Board " + data.boardCode + " is corrupted, it is scheduled for reload");
				FetchChanDataService.scheduleBoardFetchService(service.getApplicationContext(), data.boardCode, 0);
				return;
			}

			if (boardActivity && board.threads.length < MIN_THREADS_PER_BOARD) {
				// user has not changed board (may go to the thread) and board has not enough threads available
		        if (DEBUG) Log.i(TAG, "Starting service to load next page for " + data.boardCode + " page " + data.pageNo
		        		+ ". Board has currently " + board.threads.length + " threads.");
		        if (data.priority) {
		    		// we should continue fetching priority fetches only when there is no other priority fetch scheduled
		    		FetchChanDataService.scheduleBoardFetchWithPriority(service.getApplicationContext(), data.boardCode, data.pageNo + 1);
		        } else {
		        	FetchChanDataService.scheduleBoardFetchService(service.getApplicationContext(), data.boardCode, data.pageNo + 1);
		        }
			}
			
			if (boardActivity && currentActivityId.activity == ChanHelper.LastActivity.BOARD_ACTIVITY
					&& currentActivityId.threadNo == 0) {
				// user is on the board page, we need to be reloaded it
				Handler handler = activity.getChanHandler();
				if (handler != null) {
					Log.w(TAG, "Reloading board");
					handler.sendEmptyMessage(0);
				}
			}
		} else if (data.postNo == 0) {
			// thread fetching
			boolean threadActivity = currentActivityId != null && currentActivityId.boardCode != null
					&& currentActivityId.boardCode.equals(data.boardCode)
					&& currentActivityId.threadNo == data.threadNo;
			makeToast("Loaded data for " + data.boardCode + "/" + data.threadNo);
			ChanThread thread = ChanFileStorage.loadThreadData(service.getApplicationContext(), data.boardCode, data.threadNo);
			Log.i(TAG, "Loaded thread " + thread.board + "/" + thread.no + " posts " + thread.posts.length);
			if (thread.defData && threadActivity) {
				// thread file is corrupted, and user stays on thread page (or loads image), we need to refetch thread
				Log.w(TAG, "Thread " + data.boardCode + "/" + data.threadNo + " is corrupted, it is scheduled for reload");
				FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode, data.threadNo);
				return;
			}
			
			if (currentActivityId != null && threadActivity && currentActivityId.activity == ChanHelper.LastActivity.THREAD_ACTIVITY
					&& currentActivityId.postNo == 0) {
				// user is on the thread page, we need to reload it
				Handler handler = activity.getChanHandler();
				if (handler != null) {
					handler.sendEmptyMessage(0);
				}
			}
		} else {
			// image fetching
		}
	}

	@Override
	public void onDataFetchFailure(ChanIdentifiedService service, Failure failure) {
		super.onDataFetchFailure(service, failure);

		ChanActivityId data = service.getChanActivityId();
		if (data.threadNo == 0) {
			// board fetch failed
			makeToast("Fetching " + data.boardCode + " failed");
		} else if (data.postNo == 0) {
			// thread fetch failed
			makeToast("Fetching " + data.boardCode + "/" + data.threadNo + " failed");
		} else {
			// image fetch failed
		}
	}

	@Override
	public void onDataParseFailure(ChanIdentifiedService service, Failure failure) {
		ChanActivityId data = service.getChanActivityId();
		if (data.threadNo == 0) {
			// board parse failed
			makeToast("Parsing " + data.boardCode + " failed");
		} else if (data.postNo == 0) {
			// thread parse failed
			makeToast("Parsing " + data.boardCode + "/" + data.threadNo + " failed");
		}
	}

}
