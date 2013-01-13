package com.chanapps.four.service;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThread;

public class MobileProfile extends AbstractNetworkProfile {
	private static final String TAG = "";
	private static final boolean DEBUG = true;
	
	private static final int MIN_THREADS_PER_BOARD = 20;
	private static final int MAX_THREADS_PER_BOARD = 100;
	
	private static final long MIN_AUTO_REFRESH_INTERVAL = 600000;  // 10 min
	private static final long MIN_FORCED_REFRESH_INTERVAL = 120000;  // 2 min

	@Override
	public void onProfileActivated(Context context) {
		super.onProfileActivated(context);
		
		ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
		if (activityId != null) {
			if (activityId.threadNo != 0) {
				makeToast("Reloading thread data");
				FetchChanDataService.scheduleThreadFetch(context, activityId.boardCode, activityId.threadNo);
			} else if (activityId.boardCode != null) {
				makeToast("Reloading board data");
				FetchChanDataService.scheduleBoardFetch(context, activityId.boardCode);
			} else if (activityId.activity == ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY) {
				makeToast("Preloading boards a, b and s");
				FetchChanDataService.scheduleBoardFetch(context, "a");
				FetchChanDataService.scheduleBoardFetch(context, "b");
				FetchChanDataService.scheduleBoardFetch(context, "s");
			}
		}
	}

	@Override
	public void onProfileDeactivated(Context context) {
		super.onProfileDeactivated(context);
	}

	@Override
	public void onApplicationStart(Context context) {
		super.onApplicationStart(context);
		
		makeToast("Preloading boards a, b and s");
		FetchChanDataService.scheduleBoardFetch(context, "a");
		FetchChanDataService.scheduleBoardFetch(context, "b");
		FetchChanDataService.scheduleBoardFetch(context, "s");
	}

	@Override
	public void onBoardSelectorSelected(Context context) {
		super.onBoardSelectorSelected(context);

		makeToast("Preloading boards a, b and s");
		FetchChanDataService.scheduleBoardFetch(context, "a");
		FetchChanDataService.scheduleBoardFetch(context, "b");
		FetchChanDataService.scheduleBoardFetch(context, "s");
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
		
		FetchChanDataService.scheduleBoardFetch(context, board);
	}

	@Override
	public void onBoardRefreshed(Context context, String board) {
		super.onBoardRefreshed(context, board);
		
		FetchChanDataService.scheduleBoardFetchWithPriority(context, board);
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		super.onThreadSelected(context, board, threadId);
		
		FetchChanDataService.scheduleThreadFetch(context, board, threadId);
	}
	
	@Override
	public void onThreadRefreshed(Context context, String board, long threadId) {
		super.onThreadRefreshed(context, board, threadId);
		
		FetchChanDataService.scheduleThreadFetchWithPriority(context, board, threadId);
	}

	@Override
	public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
		super.onDataFetchSuccess(service, time, size);
		storeDataTransfer(time, size);
		
		ChanActivityId data = service.getChanActivityId();
		if (data.threadNo == 0) {
			// board fetching
	        if (data.priority) {
	        	BoardLoadService.startServiceWithPriority(service.getApplicationContext(), data.boardCode, data.pageNo);
	        } else {
	        	BoardLoadService.startService(service.getApplicationContext(), data.boardCode, data.pageNo);
	        }
		} else if (data.postNo == 0) {
			// thread fetching
            if (data.priority) {
            	ThreadLoadService.startServiceWithPriority(service.getApplicationContext(), data.boardCode, data.threadNo);
            } else {
            	ThreadLoadService.startService(service.getApplicationContext(), data.boardCode, data.threadNo);
            }
		} else {
			// image fetching
		}
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
			if (board == null) {
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
			if (thread == null && threadActivity) {
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
		storeFailedDataTransfer();

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
