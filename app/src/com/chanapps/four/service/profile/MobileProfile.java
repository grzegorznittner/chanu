package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import android.widget.Toast;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.loader.ChanWatchlistDataLoader;
import com.chanapps.four.service.CleanUpService;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.widget.BoardWidgetProvider;

public class MobileProfile extends AbstractNetworkProfile {
	private static final String TAG = MobileProfile.class.getSimpleName();
	private static final boolean DEBUG = true;
	
	private String networkType = "3G";
	
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD,       new FetchParams(660L,  10L, 20, 15));
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(600L,  10L, 20, 15));
		REFRESH_TIME.put(Health.SLOW,      new FetchParams(180L,  10L, 20, 10));
		REFRESH_TIME.put(Health.GOOD,      new FetchParams(120L,  10L, 12,  8));
		REFRESH_TIME.put(Health.PERFECT,   new FetchParams( 60L,  10L,  8,  4));
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
        // makeToast(R.string.mobile_profile_preloading_defaults);
        FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
        FetchChanDataService.scheduleBoardFetch(context, "a");
        /*
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
        CleanUpService.startService(context);
        Health health = getConnectionHealth();
		if (health != Health.BAD && health != Health.VERY_SLOW) {
            prefetchDefaultBoards(context);
        } else {
            makeHealthStatusToast(context, health);
        }
	}

	@Override
	public void onBoardSelectorSelected(Context context, String boardCode) {
		super.onBoardSelectorSelected(context, boardCode);
		Health health = getConnectionHealth();
        if (health == Health.BAD || health == Health.VERY_SLOW || health == Health.NO_CONNECTION)
            makeHealthStatusToast(context, health);
        else if (ChanBoard.WATCH_BOARD_CODE.equals(boardCode))
            ChanWatchlist.fetchWatchlistThreads(context);
        else if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode))
            FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
        else
            prefetchDefaultBoards(context);
	}


    @Override
    public void onBoardSelectorRefreshed(Context context, Handler handler, String boardCode) {
        super.onBoardSelectorRefreshed(context, handler, boardCode);
        if (DEBUG) Log.i(TAG, "Manual refresh board=" + boardCode);
        if (ChanBoard.WATCH_BOARD_CODE.equals(boardCode)) {
            ChanWatchlist.fetchWatchlistThreads(context);
        }
        else if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode)) {
            boolean canFetch = FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
            if (!canFetch)
                postStopMessage(handler, R.string.board_wait_to_refresh);
        }
    }

    protected void postStopMessage(Handler handler, final int stringId) {
        if (handler == null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                if (activity instanceof Activity) {
                    ((Activity)activity).setProgressBarIndeterminateVisibility(false);
                    if (stringId > 0)
                        Toast.makeText(activity.getBaseContext(), stringId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
		
		FetchChanDataService.scheduleBoardFetch(context, board);
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
	public void onBoardRefreshed(final Context context, Handler handler, String boardCode) {
		super.onBoardRefreshed(context, handler, boardCode);
        if (ChanFileStorage.hasNewBoardData(context, boardCode)) {
            onUpdateViewData(context, handler, boardCode);
        }
        else {
            boolean canFetch = FetchChanDataService.scheduleBoardFetchWithPriority(context, boardCode);
            if (!canFetch)
                postStopMessage(handler, R.string.board_wait_to_refresh);
        }
        if (DEBUG) {
			UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
			int i = 1;
			for (ChanBoardStat stat : userStats.topBoards()) {
				Log.i(TAG, "Top boards: " + i++  + ". " + stat);
			}
			i = 1;
			for (ChanThreadStat stat : userStats.topThreads()) {
				Log.i(TAG, "Top threads: " + i++ + ". " + stat);
			}
		}
	}

	@Override
	public void onUpdateViewData(Context baseContext, Handler handler, String boardCode) {
		super.onUpdateViewData(baseContext, handler, boardCode);
		
		final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();

        if (ChanFileStorage.hasNewBoardData(baseContext, boardCode))
		    ChanFileStorage.loadFreshBoardData(baseContext, boardCode);

		boolean boardActivity = currentActivityId != null
				&& currentActivityId.boardCode != null
				&& currentActivityId.boardCode.equals(boardCode);
		
		if (boardActivity && currentActivityId.activity == ChanHelper.LastActivity.BOARD_ACTIVITY
				&& currentActivityId.threadNo == 0 && handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.refresh();
                }
            });
    }

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		if(DEBUG) Log.d(TAG, "onThreadSelected");
		super.onThreadSelected(context, board, threadId);
		
		if (board != null || threadId > 0) {
			FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId);
		}
	}
	
	@Override
	public void onThreadRefreshed(Context context, Handler handler, String board, long threadId) {
		super.onThreadRefreshed(context, handler, board, threadId);
		boolean canFetch = FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId);
        if (!canFetch)
            postStopMessage(handler, R.string.board_wait_to_refresh);
    }

	@Override
	public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
		// default behaviour is to parse properly loaded item
		super.onDataFetchSuccess(service, time, size);
	}

    private void handleBoardSelectorParseSuccess(ChanIdentifiedService service) {
        ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();

        // check if board data corrupted, we need to reload it
        if (ChanBoard.POPULAR_BOARD_CODE.equals(data.boardCode)) {
            ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
            if ((board == null || board.defData)) {
                if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted, it is scheduled for reload");
                FetchPopularThreadsService.schedulePopularFetchService(service.getApplicationContext());
                return;
            }
        }

        // user is on the same tab, we need to reload it
        Handler handler = activity.getChanHandler();
        if (    //data.boardCode.equals(currentActivityId.boardCode) &&
                currentActivityId.activity == LastActivity.BOARD_SELECTOR_ACTIVITY &&
                handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.refresh();
                }
            });

        // tell it to refresh widgets for board if any are configured
        if (DEBUG) Log.i(TAG, "Calling widget provider update for boardCode=" + data.boardCode);
        BoardWidgetProvider.updateAll(activity.getBaseContext(), data.boardCode);
    }

    private void handleBoardParseSuccess(ChanIdentifiedService service) {
        ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();

        boolean isBoardActivity = currentActivityId != null
                && currentActivityId.boardCode != null
                && currentActivityId.boardCode.equals(data.boardCode);
        ChanBoard board;

        if (data.priority) {
            board = ChanFileStorage.loadFreshBoardData(service.getApplicationContext(), data.boardCode);
        } else {
            board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
        }

        if (board == null || board.defData) {
            // board data corrupted, we need to reload it
            if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted, it is scheduled for reload");
            FetchChanDataService.scheduleBoardFetch(service.getApplicationContext(), data.boardCode);
            return;
        }

        // user is on the board page, we need to be reloaded it
        Handler handler = activity.getChanHandler();
        if (isBoardActivity && currentActivityId.activity == ChanHelper.LastActivity.BOARD_ACTIVITY
                && currentActivityId.threadNo == 0 && handler != null
                && (currentActivityId.priority || data.priority))
            handler.post(new Runnable() {
            @Override
            public void run() {
                activity.refresh();
            }
        });

        // tell it to refresh widgets for board if any are configured
        if (DEBUG) Log.i(TAG, "Calling widget provider update for boardCode=" + data.boardCode);
        BoardWidgetProvider.updateAll(activity.getBaseContext(), data.boardCode);
    }

    private void handleThreadParseSuccess(ChanIdentifiedService service) {
        final ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        final ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();

        boolean isThreadActivity = currentActivityId != null && currentActivityId.boardCode != null
                && currentActivityId.boardCode.equals(data.boardCode)
                && currentActivityId.threadNo == data.threadNo;
        
        ChanThread thread = ChanFileStorage.loadThreadData(service.getApplicationContext(), data.boardCode, data.threadNo);
        if (DEBUG) Log.i(TAG, "Loaded thread " + thread.board + "/" + thread.no + " posts " + thread.posts.length);
        if ((thread == null || thread.defData) && isThreadActivity) {
            // thread file is corrupted, and user stays on thread page (or loads image), we need to refetch thread
            if (DEBUG) Log.w(TAG, "Thread " + data.boardCode + "/" + data.threadNo + " is corrupted, it is scheduled for reload");
            FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode, data.threadNo);
            return;
        }

        if(DEBUG) Log.i(TAG, "Check reload thread " + thread.board + "/" + thread.no
                + " currentActivityId=" + currentActivityId
                + " isThreadActivity=" + isThreadActivity
                + " activityPriority=" + currentActivityId.priority
                + " dataPriority=" + data.priority
                + " currentActivity.activity=" + (currentActivityId == null ? "null" : currentActivityId.activity)
                + " currentActivity.postNo=" + (currentActivityId == null ? "null" : currentActivityId.postNo));

        // user is on the board page, we need to be reloaded it
        Handler handler = activity.getChanHandler();
        if (isThreadActivity && currentActivityId.activity == LastActivity.THREAD_ACTIVITY
                && currentActivityId.postNo == 0 && handler != null)
                // && (currentActivityId.priority || data.priority)) // we don't care about priority, we always refresh
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.refresh();
                }
            });

    }

	@Override
	public void onDataParseSuccess(ChanIdentifiedService service) {
		super.onDataParseSuccess(service);
		ChanActivityId data = service.getChanActivityId();
        if (ChanBoard.isVirtualBoard(data.boardCode))
            handleBoardSelectorParseSuccess(service);
		else if (data.threadNo == 0)
            handleBoardParseSuccess(service);
        else if (data.postNo == 0)
            handleThreadParseSuccess(service);
        // otherwise image fetching, ignore
	}

	@Override
	public void onDataFetchFailure(final ChanIdentifiedService service, Failure failure) {
		super.onDataFetchFailure(service, failure);

		final ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        Handler handler = activity != null ? activity.getChanHandler() : null;
        if (handler == null)
            return;
        if ((data.threadNo == 0 || data.postNo == 0) && activity instanceof Activity)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Activity a = (Activity)activity;
                    a.setProgressBarIndeterminateVisibility(false);
                    String msg = data.threadNo == 0
                            ? String.format(service.getApplicationContext().getString(
                                R.string.mobile_profile_fetch_board_fail),
                                data.boardCode)
                            : String.format(service.getApplicationContext().getString(
                                R.string.mobile_profile_fetch_thread_fail),
                                data.boardCode, data.threadNo);
                    makeToast(msg);
                }
            });
    }

	@Override
	public void onDataParseFailure(final ChanIdentifiedService service, Failure failure) {
        final ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        Handler handler = activity != null ? activity.getChanHandler() : null;
        if (handler == null)
            return;
        if ((data.threadNo == 0 || data.postNo == 0) && activity instanceof Activity)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Activity a = (Activity)activity;
                    a.setProgressBarIndeterminateVisibility(false);
                    String msg = data.threadNo == 0
                            ? String.format(service.getApplicationContext().getString(
                            R.string.mobile_profile_parse_board_fail),
                            data.boardCode)
                            : String.format(service.getApplicationContext().getString(
                            R.string.mobile_profile_parse_thread_fail),
                            data.boardCode, data.threadNo);
                    makeToast(msg);
                }
            });
	}
}
