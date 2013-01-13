/**
 * 
 */
package com.chanapps.four.service;

import java.util.Stack;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.DataTransfer;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public abstract class AbstractNetworkProfile implements NetworkProfile {
	private static final String TAG = "AbstractNetworkProfile";
	private static final boolean DEBUG = true;
	
	protected int usageCounter = 0;
	
	private static final int MAX_STORED_DATATRANSFERS = 20;
	protected Stack<DataTransfer> dataTransfers = new Stack<DataTransfer>();
	
	protected void storeDataTransfer(int time, int size) {
		dataTransfers.push(new DataTransfer(time, size));
		if (dataTransfers.size() > MAX_STORED_DATATRANSFERS) {
			dataTransfers.setSize(MAX_STORED_DATATRANSFERS);
		}
	}
	
	protected void storeFailedDataTransfer() {
		dataTransfers.push(new DataTransfer());
		if (dataTransfers.size() > MAX_STORED_DATATRANSFERS) {
			dataTransfers.setSize(MAX_STORED_DATATRANSFERS);
		}
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
	}

	@Override
	public void onProfileDeactivated(Context context) {
		if (DEBUG) Log.d(TAG, "onProfileDeactivated called");
	}

	@Override
	public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
		if (DEBUG) Log.d(TAG, "finishedFetchingData called for " + service + " " + size + " bytes during " + time + "ms");
	}

	@Override
	public void onDataFetchFailure(ChanIdentifiedService service, Failure failure) {
		if (DEBUG) Log.d(TAG, "failedFetchingData called for " + service);
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
				handler.post(new Runnable() {
		            public void run() {
		            	Log.w(TAG, "Calling toast with '" + text + "'");
		                Toast.makeText(activity.getBaseContext(), text, Toast.LENGTH_SHORT).show();
		            }
		        });
			} else {
				Log.w(TAG, "Null handler for " + activity);
			}
		}

	}
}
