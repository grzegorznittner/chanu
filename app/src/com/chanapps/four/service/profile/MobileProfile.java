package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanBoardStat;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThreadStat;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.service.CleanUpService;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

public class MobileProfile extends AbstractNetworkProfile {
	private static final String TAG = MobileProfile.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	private static final int MIN_THREADS_PER_BOARD = 20;
	
	private String networkType = "3G";
	
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD,       new FetchParams(660L, 240L, 20, 15));
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(600L, 200L, 20, 15));
		REFRESH_TIME.put(Health.SLOW,      new FetchParams(180L, 120L, 20, 10));
		REFRESH_TIME.put(Health.GOOD,      new FetchParams(120L,  80L, 12,  8));
		REFRESH_TIME.put(Health.PERFECT,   new FetchParams( 60L,  40L,  8,  4));
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
					makeToast(R.string.mobile_profile_loading_thread);
					FetchChanDataService.scheduleThreadFetch(context, activityId.boardCode, activityId.threadNo);
				} else if (activityId.activity == LastActivity.BOARD_ACTIVITY) {
					makeToast(R.string.mobile_profile_loading_board);
					FetchChanDataService.scheduleBoardFetch(context, activityId.boardCode);
				} else if (activityId.activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
					Handler handler = activity.getChanHandler();
					if (handler != null) {
						makeToast(R.string.mobile_profile_loading_image);
						handler.sendEmptyMessageDelayed(GalleryViewActivity.START_DOWNLOAD_MSG, 100);
					}
				} else if (activityId.activity == ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY) {
					if (health != Health.VERY_SLOW) {
                        prefetchDefaultBoards(context);
					} else {
                        makeHealthStatusToast(context, health);
					}
				}
			}
		} else {
			makeToast(R.string.mobile_profile_no_auto_refresh);
		}
	}

    private void makeHealthStatusToast(Context context, Health health) {
        makeToast(String.format(context.getString(R.string.mobile_profile_health_status), health.toString().toLowerCase().replaceAll("_", " ")));
    }

    private void prefetchDefaultBoards(Context context) {
        /*
            makeToast(R.string.mobile_profile_preloading_defaults);
            FetchChanDataService.scheduleBoardFetch(context, "a");
            FetchChanDataService.scheduleBoardFetch(context, "b");
            FetchChanDataService.scheduleBoardFetch(context, "v");
            FetchChanDataService.scheduleBoardFetch(context, "vg");
            FetchChanDataService.scheduleBoardFetch(context, "s");
        */
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
            prefetchDefaultBoards(context);
        } else {
            makeHealthStatusToast(context, health);
        }
	}

	@Override
	public void onBoardSelectorSelected(Context context) {
		super.onBoardSelectorSelected(context);
		
    	CleanUpService.startService(context);

		Health health = getConnectionHealth();
		if (health != Health.BAD && health != Health.VERY_SLOW) {
            prefetchDefaultBoards(context);
        } else {
            makeHealthStatusToast(context, health);
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
	public void onBoardRefreshed(Context context, Handler handler, String board) {
		super.onBoardRefreshed(context, handler, board);
		if (!FetchChanDataService.scheduleBoardFetchWithPriority(context, board))
            handler.sendEmptyMessage(LoaderHandler.SET_PROGRESS_FINISHED);
        if ("a".equals(board)) {
			UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
			int i = 1;
			for (ChanBoardStat stat : userStats.topBoards()) {
				if(DEBUG) Log.i(TAG, "Top boards: " + i++  + ". " + stat);
			}
			i = 1;
			for (ChanThreadStat stat : userStats.topThreads()) {
				if(DEBUG) Log.i(TAG, "Top threads: " + i++ + ". " + stat);
			}
		}
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		if(DEBUG) Log.d(TAG, "onThreadSelected");
		super.onThreadSelected(context, board, threadId);
		
		FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId);
	}
	
	@Override
	public void onThreadRefreshed(Context context, Handler handler, String board, long threadId) {
		super.onThreadRefreshed(context, handler, board, threadId);
		if (!FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId))
            handler.sendEmptyMessage(LoaderHandler.SET_PROGRESS_FINISHED);
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
            //makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_loaded_board), data.boardCode));
			ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
			if (board == null || board.defData) {
				// board data corrupted, we need to reload it
				if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted, it is scheduled for reload");
				FetchChanDataService.scheduleBoardFetch(service.getApplicationContext(), data.boardCode);
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
					if (DEBUG) Log.w(TAG, "Reloading board");
					handler.sendEmptyMessage(0);
				}
			}
		} else if (data.postNo == 0) {
			// thread fetching
			boolean threadActivity = currentActivityId != null && currentActivityId.boardCode != null
					&& currentActivityId.boardCode.equals(data.boardCode)
					&& currentActivityId.threadNo == data.threadNo;
            //makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_loaded_thread), data.boardCode, data.threadNo));
			ChanThread thread = ChanFileStorage.loadThreadData(service.getApplicationContext(), data.boardCode, data.threadNo);
			if (DEBUG) Log.i(TAG, "Loaded thread " + thread.board + "/" + thread.no + " posts " + thread.posts.length);
			if (thread.defData && threadActivity) {
				// thread file is corrupted, and user stays on thread page (or loads image), we need to refetch thread
				if (DEBUG) Log.w(TAG, "Thread " + data.boardCode + "/" + data.threadNo + " is corrupted, it is scheduled for reload");
				FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode, data.threadNo);
				return;
			}

            if(DEBUG) Log.i(TAG, "Check reload thread " + thread.board + "/" + thread.no
                    + " currentActivityId=" + currentActivityId
                    + " threadActivity=" + threadActivity
                    + " currentActivity.activity=" + (currentActivityId == null ? "null" : currentActivityId.activity)
                    + " currentActivity.postNo=" + (currentActivityId == null ? "null" : currentActivityId.postNo));
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
			makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_fetch_board_fail), data.boardCode));
		} else if (data.postNo == 0) {
			// thread fetch failed
            makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_fetch_thread_fail), data.boardCode, data.threadNo));
		} else {
			// image fetch failed
		}
	}

	@Override
	public void onDataParseFailure(ChanIdentifiedService service, Failure failure) {
		ChanActivityId data = service.getChanActivityId();
		if (data.threadNo == 0) {
			// board parse failed
            makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_parse_board_fail), data.boardCode));
		} else if (data.postNo == 0) {
			// thread parse failed
            makeToast(String.format(service.getApplicationContext().getString(R.string.mobile_profile_parse_thread_fail), data.boardCode, data.threadNo));
		}
	}

}
