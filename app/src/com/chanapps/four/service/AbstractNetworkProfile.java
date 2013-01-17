/**
 * 
 */
package com.chanapps.four.service;

import java.util.Date;
import java.util.Stack;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.DataTransfer;
import com.chanapps.four.data.FetchParams;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public abstract class AbstractNetworkProfile implements NetworkProfile {
	private static final String TAG = "AbstractNetworkProfile";
	private static final boolean DEBUG = true;
	
	protected int usageCounter = 0;
	
	private static final int MAX_STORED_DATATRANSFERS = 5;
	private static final int MAX_DATATRANSFER_INACTIVITY = 600000;  // 10 min
	private Stack<DataTransfer> dataTransfers = new Stack<DataTransfer>();
	
	private Health currentHealth = null;
	private static final FetchParams DEFAULT_FETCH_PARAMS = new FetchParams(1800000L, 1800000L, 3000);
	
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
			if (DEBUG) Log.i(TAG, "Less than 2 transfers, setting default health " + defaultHealth);
			currentHealth = defaultHealth;
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
			if (DEBUG) Log.i(TAG, "More than 2 failures, switching to BAD from " + currentHealth);
			if (currentHealth != Health.BAD) {
				makeToast("Internet connection is not working!");
			}
			currentHealth = Health.BAD;
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
		if (DEBUG) Log.i(TAG, "Avg rate " + avgRate + " kB/s, setting health " + currentHealth);
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
	}

	@Override
	public void onBoardSelectorSelected(Context context) {
		if (DEBUG) Log.d(TAG, "onBoardSelectorSelected called");
		usageCounter++;
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		if (DEBUG) Log.d(TAG, "onBoardSelected called with board: " + board);
		usageCounter++;
	}

	@Override
	public void onBoardRefreshed(Context context, String board) {
		if (DEBUG) Log.d(TAG, "onBoardRefreshed called with board: " + board);
		usageCounter++;
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		if (DEBUG) Log.d(TAG, "onThreadSelected called with board: " + board + " threadId: " + threadId);
		usageCounter++;
	}

	@Override
	public void onThreadRefreshed(Context context, String board, long threadId) {
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
		if (DEBUG) Log.d(TAG, "finishedFetchingData called for " + service + " " + size + " bytes during " + time + "ms");
		
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
	public void onDataFetchFailure(ChanIdentifiedService service, Failure failure) {
		if (DEBUG) Log.d(TAG, "failedFetchingData called for " + service);
		storeFailedDataTransfer();
	}

	@Override
	public void onDataParseSuccess(ChanIdentifiedService service) {
		if (DEBUG) Log.d(TAG, "finishedParsingData called for " + service);
	}

	@Override
	public void onDataParseFailure(ChanIdentifiedService service, Failure failure) {
		if (DEBUG) Log.d(TAG, "failedParsingData called for " + service);
	}
	
	protected void makeToast(final String text) {
		final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				handler.postDelayed(new Runnable() {
		            public void run() {
		            	Log.w(TAG, "Calling toast with '" + text + "'");
		                Toast.makeText(activity.getBaseContext(), text, Toast.LENGTH_SHORT).show();
		            }
		        }, 300);
			} else {
				Log.w(TAG, "Null handler for " + activity);
			}
		}

	}
}
