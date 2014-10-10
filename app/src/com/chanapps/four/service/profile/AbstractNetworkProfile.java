/**
 * 
 */
package com.chanapps.four.service.profile;

import java.util.Date;
import java.util.Stack;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.*;
import com.chanapps.four.data.*;
import com.chanapps.four.service.BoardParserService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadParserService;
import com.chanapps.four.widget.WidgetProviderUtils;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public abstract class AbstractNetworkProfile implements NetworkProfile {
	private static final String TAG = "AbstractNetworkProfile";
	private static final boolean DEBUG = false;
	
	protected int usageCounter = 0;
	
	private static final int MAX_STORED_DATATRANSFERS = 5;
	private static final int MAX_DATATRANSFER_INACTIVITY = 600000;  // 10 min
	private Stack<DataTransfer> dataTransfers = new Stack<DataTransfer>();
	
	private Health currentHealth = null;
	/*
	 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
	 */
	private static final FetchParams DEFAULT_FETCH_PARAMS = new FetchParams(600L, 3L, 3, 3, 0, 10);
	
	@Override
	public FetchParams getFetchParams() {
		return DEFAULT_FETCH_PARAMS;
	}

	protected synchronized void checkDataTransfer() {
		if (dataTransfers.size() > 0) {
			if (new Date().getTime() - dataTransfers.get(0).time.getTime() > MAX_DATATRANSFER_INACTIVITY) {
				dataTransfers.clear();
			}
		}
	}
	
	protected synchronized void storeDataTransfer(int time, int size) {
		DataTransfer transfer = new DataTransfer(time, size);
		dataTransfers.push(transfer);
		if (DEBUG) Log.i(TAG, "Storing transfer " + transfer);
		if (dataTransfers.size() > MAX_STORED_DATATRANSFERS) {
			dataTransfers.setSize(MAX_STORED_DATATRANSFERS);
		}
	}
	
	protected synchronized void storeFailedDataTransfer() {
		DataTransfer transfer = new DataTransfer();
		dataTransfers.push(transfer);
		if (DEBUG) Log.i(TAG, "Storing transfer " + transfer);
		if (dataTransfers.size() > MAX_STORED_DATATRANSFERS) {
			dataTransfers.setSize(MAX_STORED_DATATRANSFERS);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Health getConnectionHealth() {
		double rateSum = 0.0;
		int rateNum = 0;
		int failures = 0;
		Stack<DataTransfer> clonedTransfers = (Stack<DataTransfer>)dataTransfers.clone();
		if (clonedTransfers.size() < 2) {
			Health defaultHealth = getDefaultConnectionHealth();
			if (currentHealth != defaultHealth) {
                if (DEBUG) Log.i(TAG, "Less than 2 transfers, changing default health to " + defaultHealth);
                currentHealth = defaultHealth;
            }
			return defaultHealth;
		}
		for (DataTransfer transfer : clonedTransfers) {
			if (transfer.failed) {
				rateNum++;
				rateSum /= 2.0;
				failures++;
			} else {
				rateSum += transfer.dataRate;
				rateNum++;
			}
		}
		if (failures > 2) {
			if (currentHealth != Health.BAD) {
                if (DEBUG) Log.i(TAG, "More than 2 failures, switching to BAD from " + currentHealth);
                makeToast(R.string.network_profile_health_bad);
                currentHealth = Health.BAD;
            }
			return currentHealth;
		}
		double avgRate = rateSum / rateNum;
		if (avgRate > 200) {
			currentHealth = Health.PERFECT;
		} else if (avgRate > 50) {
			currentHealth = Health.GOOD;
		} else if (avgRate > 10) {
			currentHealth = Health.SLOW;
		} else {
			currentHealth = Health.VERY_SLOW;
		}
		//if (DEBUG) Log.i(TAG, "Avg rate " + avgRate + " kB/s, setting health " + currentHealth);
		return currentHealth;
	}
	
	@Override
	public Health getDefaultConnectionHealth() {
		return Health.GOOD;
	}

	@Override
	public void onApplicationStart(Context context) {
		if (DEBUG) Log.d(TAG, "onApplicationStart called");
		usageCounter++;
        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
        if (DEBUG) Log.i(TAG, "onApplicationStart scheduling global alarm");
        WidgetProviderUtils.scheduleGlobalAlarm(context);
        /*
        Health health = getConnectionHealth();
        if (health != Health.NO_CONNECTION && health != Health.BAD && health != Health.VERY_SLOW && health != Health.SLOW) {
            WidgetProviderUtils.asyncUpdateWidgetsAndWatchlist(context); // this also schedules alarm
        } else {
            makeHealthStatusToast(context, health);
            WidgetProviderUtils.scheduleGlobalAlarm(context);
        }
        */
    }

	@Override
	public void onBoardSelectorSelected(Context context, String boardCode) {
		if (DEBUG) Log.d(TAG, "onBoardSelectorSelected called");
		usageCounter++;
	}

	@Override
	public void onBoardSelectorRefreshed(Context context, Handler handler, String boardCode) {
		if (DEBUG) Log.d(TAG, "onBoardSelectorRefreshed called");
		usageCounter++;
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		if (DEBUG) Log.d(TAG, "onBoardSelected called with board: " + board);
		usageCounter++;
	}

	@Override
	public void onBoardRefreshed(Context context, Handler handler, String board) {
		if (DEBUG) Log.d(TAG, "onBoardRefreshed called with board: " + board);
		usageCounter++;
	}

	@Override
	public void onUpdateViewData(Context baseContext, Handler chanHandler, String board) {
		if (DEBUG) Log.d(TAG, "onUpdateViewData called with board: " + board);
		usageCounter++;
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		if (DEBUG) Log.d(TAG, "onThreadSelected called with board: " + board + " threadId: " + threadId);
		usageCounter++;
	}

	@Override
	public void onThreadRefreshed(Context context, Handler handler, String board, long threadId) {
		if (DEBUG) Log.d(TAG, "onThreadRefreshed called with board: " + board + " threadId: " + threadId);
		usageCounter++;
	}

	@Override
	public void onFullImageLoading(Context context, String board, long threadId, long postId) {
		if (DEBUG) Log.d(TAG, "onFullImageLoading called with board: " + board + " threadId: " + threadId + " postId: " + postId);
		usageCounter++;
	}
	
	@Override
	public void onProfileActivated(Context context) {
		if (DEBUG) Log.d(TAG, "onProfileActivated called");
        checkDataTransfer();
	}

	@Override
	public void onProfileDeactivated(Context context) {
		if (DEBUG) Log.d(TAG, "onProfileDeactivated called");
	}

	@Override
	public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
		if (DEBUG) Log.i(TAG, "finishedFetchingData called for " + service + " " + size + " bytes during " + time + "ms");
		
		storeDataTransfer(time, size);
		
		ChanActivityId data = service.getChanActivityId();
        if (DEBUG) Log.i(TAG, "fetchData success for /" + data.boardCode + "/" + data.threadNo + "/" + data.postNo + " priority=" + data.priority);

        if (ChanBoard.isVirtualBoard(data.boardCode)) {
            // skip since fetch&parse steps happen together for virtual boards
        } else if (data.threadNo == 0) {
			// board fetching
            BoardParserService.startService(service.getApplicationContext(), data.boardCode, data.pageNo, data.priority, data.secondaryThreadNo);
            //CleanUpService.startService(service.getApplicationContext());
		} else if (data.postNo == 0) {
			// thread fetching
            ThreadParserService.startService(service.getApplicationContext(), data.boardCode, data.threadNo, data.priority);
		} else {
			// image fetching
		}
	}

	@Override
	public void onDataFetchFailure(ChanIdentifiedService service, Failure failure) {
		if (DEBUG) Log.d(TAG, "failedFetchingData called for " + service);
		storeFailedDataTransfer();
        final ChanActivityId data = service.getChanActivityId();
        if (data == null || (data.threadNo > 0 && data.postNo > 0)) { // ignore post/image fetch failures
            if (DEBUG) Log.i(TAG, "null data or image fetch failure, ignoring");
            return;
        }
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity == null) {
            if (DEBUG) Log.i(TAG, "null activity failure, ignoring");
            return;
        }
        Handler handler = activity.getChanHandler();
        if (handler == null) {
            if (DEBUG) Log.i(TAG, "null handler failure, ignoring");
            return;
        }
        switch (failure) {
            case DEAD_THREAD:
                if (DEBUG) Log.i(TAG, "refreshig after dead thread");
                if (activity instanceof ThreadActivity)
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                            ((ThreadActivity)activity)
                                    .refreshFragment(data.boardCode, data.threadNo, null);
                        }
                    });
                break;
            case THREAD_UNMODIFIED:
                if (DEBUG) Log.i(TAG, "stopping after unmodified thread");
                //postStopMessage(handler, R.string.mobile_profile_fetch_unmodified);
                postStopMessage(handler, null);
                break;
            case NETWORK:
            case MISSING_DATA:
            case WRONG_DATA:
            case CORRUPT_DATA:
            default:
                if (DEBUG) Log.i(TAG, "stopping after generic failure");
                //postStopMessage(handler, R.string.mobile_profile_fetch_failure);
                postStopMessage(handler, null);
                break;
        }
    }

	@Override
	public void onDataParseSuccess(ChanIdentifiedService service) {
		if (DEBUG) Log.d(TAG, "finishedParsingData called for " + service);
	}

	@Override
	public void onDataParseFailure(ChanIdentifiedService service, Failure failure) {
		if (DEBUG) Log.d(TAG, "failedParsingData called for " + service);
	    onDataFetchFailure(service, failure);
    }
	
	@Override
	public void onImageDownloadSuccess(Context context, int time, int size) {
		storeDataTransfer(time, size);
	}

	protected void makeToast(final String text) {
        NetworkProfileManager.instance().makeToast(text);
	}

	protected void makeToast(final int id) {
        NetworkProfileManager.instance().makeToast(id);
	}

    protected void startProgress(Handler handler) {
        if (handler == null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                NetworkProfileManager.instance().getActivity().setProgress(true);
            }
        });
    }

    protected void postStopMessage(Handler handler, final String string) {
        if (handler == null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                activity.setProgress(false);
                if (string != null && !string.isEmpty())
                    Toast.makeText(activity.getBaseContext(), string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void postStopMessage(Handler handler, final int stringId) {
        if (handler == null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                activity.setProgress(false);
                if (stringId > 0)
                    Toast.makeText(activity.getBaseContext(), stringId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void postStopMessageWithRefresh(Handler handler, final int stringId) {
        if (handler == null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                if (stringId > 0)
                    Toast.makeText(activity.getBaseContext(), stringId, Toast.LENGTH_SHORT).show();
                activity.refresh();
            }
        });
    }

    protected void makeHealthStatusToast(Context context, Health health) {
        Handler handler = NetworkProfileManager.instance().getActivity() != null
                ? NetworkProfileManager.instance().getActivity().getChanHandler()
                : null;
        if (handler != null)
            postStopMessage(handler,
                    String.format(context.getString(R.string.mobile_profile_health_status),
                            health.toString().toLowerCase().replaceAll("_", " ")));
    }

}
