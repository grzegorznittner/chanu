package com.chanapps.four.service;

import android.content.Context;
import android.os.Handler;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanHelper;

public class NoConnectionProfile extends AbstractNetworkProfile {

	@Override
	public Type getConnectionType() {
		return Type.NO_CONNECTION;
	}

	@Override
	public Health getConnectionHealth() {
		return Health.NO_CONNECTION;
	}
	
	@Override
	public void onProfileActivated(Context context) {
		super.onProfileActivated(context);
		
		FetchChanDataService.clearServiceQueue(context);
		
		makeToast("Data connection lost. Off-line mode");
	}

	@Override
	public void onApplicationStart(Context context) {
		super.onApplicationStart(context);
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
		
		makeToast("Off-line mode");
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		super.onThreadSelected(context, board, threadId);

		makeToast("Off-line mode");
	}

	@Override
	public void onDataParseSuccess(ChanIdentifiedService service) {
		ChanActivityId data = service.getChanActivityId();
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();
		
		if (data.threadNo == 0) {
			// board fetching
			boolean boardActivity = currentActivityId != null
					&& currentActivityId.boardCode != null
					&& currentActivityId.boardCode.equals(data.boardCode);
			if (boardActivity && currentActivityId.activity == ChanHelper.LastActivity.BOARD_ACTIVITY
					&& currentActivityId.threadNo == 0) {
				// user is on the board page, we need to be reloaded it
				Handler handler = activity.getChanHandler();
				if (handler != null) {
					handler.sendEmptyMessage(0);
				}
			}
		} else if (data.postNo == 0) {
			// thread fetching
			boolean threadActivity = currentActivityId != null && currentActivityId.boardCode != null
					&& currentActivityId.boardCode.equals(data.boardCode)
					&& currentActivityId.threadNo == data.threadNo;
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
}
