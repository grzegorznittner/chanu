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
        if (health == Health.NO_CONNECTION)
            return;

        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
        if (activityId == null)
            return;

        switch (activityId.activity) {
            case THREAD_ACTIVITY:
                ChanThread thread = ChanFileStorage.loadThreadData(context, activityId.boardCode, activityId.threadNo);
                if (thread == null || thread.posts == null || thread.posts.length < 2) {
                    makeToast(R.string.mobile_profile_loading_thread);
                    FetchChanDataService.scheduleThreadFetch(context, activityId.boardCode, activityId.threadNo);
                }
                break;
            case BOARD_ACTIVITY:
                ChanBoard board = ChanFileStorage.loadBoardData(context, activityId.boardCode);
                if (board == null || board.threads == null || board.threads.length == 0) {
                    makeToast(R.string.mobile_profile_loading_board);
                    FetchChanDataService.scheduleBoardFetch(context, activityId.boardCode);
                }
                break;
            case FULL_SCREEN_IMAGE_ACTIVITY:
                Handler handler = activity.getChanHandler();
                if (handler != null) {
                    makeToast(R.string.mobile_profile_loading_image);
                    handler.sendEmptyMessageDelayed(GalleryViewActivity.START_DOWNLOAD_MSG, 100);
                }
                break;
            case BOARD_SELECTOR_ACTIVITY:
                onBoardSelectorSelected(context, activityId.boardCode);
                break;
            default:
        }
    }

    private void makeHealthStatusToast(Context context, Health health) {
        postStopMessage(NetworkProfileManager.instance().getActivity().getChanHandler(),
                String.format(context.getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " ")));
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
        BoardWidgetProvider.asyncUpdateWidgetsAndWatchlist(context);
        if (health != Health.BAD && health != Health.VERY_SLOW) {
            prefetchDefaultBoards(context);
        } else {
            makeHealthStatusToast(context, health);
        }
	}

	@Override
	public void onBoardSelectorSelected(Context context, String boardCode) {
		super.onBoardSelectorSelected(context, boardCode);
        if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode)) {
            Health health = getConnectionHealth();
            if (health == Health.NO_CONNECTION) {
                makeHealthStatusToast(context, health);
                return;
            }
            ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
            if (board != null && board.threads != null && board.threads.length > 0) {
                if (DEBUG) Log.i(TAG, "skipping preload board " + boardCode + " as already have data");
                return;
            }

            FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
        }
	}


    @Override
    public void onBoardSelectorRefreshed(Context context, Handler handler, String boardCode) {
        super.onBoardSelectorRefreshed(context, handler, boardCode);
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        }
        if (DEBUG) Log.i(TAG, "Manual refresh board=" + boardCode);
        if (ChanBoard.WATCH_BOARD_CODE.equals(boardCode)) {
            ChanWatchlist.fetchWatchlistThreads(context);
            //ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
            //activity.refresh();
        }
        else if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode)) {
            boolean canFetch = FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
            if (!canFetch)
                postStopMessage(handler, R.string.board_wait_to_refresh);
        }
    }

    @Override
	public void onBoardSelected(Context context, String boardCode) {
		super.onBoardSelected(context, boardCode);
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board != null && board.threads != null && board.threads.length > 0) {
            if (DEBUG) Log.i(TAG, "skipping preload board as already have data");
            return;
        }
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            if (DEBUG) Log.i(TAG, "skipping preload board as there is no network connection");
            return;
        }
        boolean canFetch = FetchChanDataService.scheduleBoardFetch(context, boardCode);
        if (canFetch)
            if (DEBUG) Log.i(TAG, "auto-fetching selected board=" + boardCode);
        else
            if (DEBUG) Log.i(TAG, "skipping fresh selected board=" + boardCode);

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
        Health health = getConnectionHealth();
        if (ChanFileStorage.hasNewBoardData(context, boardCode)) {
            onUpdateViewData(context, handler, boardCode);
        }
        else if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
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
	public void onThreadSelected(Context context, String boardCode, long threadId) {
		if(DEBUG) Log.d(TAG, "onThreadSelected");
		super.onThreadSelected(context, boardCode, threadId);
        ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadId);
        if (thread != null && thread.posts != null && thread.posts.length > 1) {
            if (DEBUG) Log.i(TAG, "thread already loading, skipping load");
            return;
        }
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        }
        FetchChanDataService.scheduleThreadFetchWithPriority(context, boardCode, threadId);
	}
	
	@Override
	public void onThreadRefreshed(Context context, Handler handler, String board, long threadNo) {
		super.onThreadRefreshed(context, handler, board, threadNo);
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        }
        boolean canFetch = FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadNo);
        if (DEBUG) Log.i(TAG, "onThreadRefreshed canFetch=" + canFetch + " handler=" + handler);
        if (!canFetch) {
            ChanThread thread = ChanFileStorage.loadThreadData(context, board, threadNo);
            int msgId;
            if (thread != null && thread.isDead)
                msgId = R.string.thread_dead;
            else if (thread != null && thread.closed > 0)
                msgId = R.string.thread_closed;
            else
                msgId = R.string.board_wait_to_refresh;
            postStopMessage(handler, msgId);
        }
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
                if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted");
                NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
                //FetchPopularThreadsService.schedulePopularFetchService(service.getApplicationContext());
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
            if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted");
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            //FetchChanDataService.scheduleBoardFetch(service.getApplicationContext(), data.boardCode);
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

        boolean isThreadActivity = currentActivityId != null
                && currentActivityId.boardCode != null
                && currentActivityId.boardCode.equals(data.boardCode)
                && currentActivityId.threadNo == data.threadNo
                && currentActivityId.postNo == 0
                && currentActivityId.activity == LastActivity.THREAD_ACTIVITY;

        ChanThread thread = ChanFileStorage.loadThreadData(service.getApplicationContext(), data.boardCode, data.threadNo);
        if (DEBUG) Log.i(TAG, "Loaded thread " + thread.board + "/" + thread.no + " posts " + thread.posts.length);
        if ((thread == null || thread.defData) && isThreadActivity) {
            // thread file is corrupted, and user stays on thread page (or loads image), we need to refetch thread
            if (DEBUG) Log.w(TAG, "Thread " + data.boardCode + "/" + data.threadNo + " is corrupted");
            //FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode, data.threadNo);
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            return;
        }

        Handler handler = activity.getChanHandler();
        if(DEBUG) Log.i(TAG, "Check reload thread " + thread.board + "/" + thread.no
                + " isThreadActivity=" + isThreadActivity
                + " handler=" + handler);

        // user is on the thread page, we need to reloaded it
        if (isThreadActivity && handler != null)
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
    }

	@Override
	public void onDataParseFailure(final ChanIdentifiedService service, Failure failure) {
        super.onDataFetchFailure(service, failure);
	}
}
