package com.chanapps.four.service.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import android.widget.Toast;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

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
		
		makeToast(R.string.no_connection_profile);
	}

	@Override
	public void onApplicationStart(Context context) {
		super.onApplicationStart(context);
	}

    @Override
    public void onBoardSelectorRefreshed(final Context context, Handler handler, String boardCode) {
        super.onBoardSelectorRefreshed(context, handler, boardCode);
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                    if (activity instanceof Activity) {
                        ((Activity)activity).setProgressBarIndeterminateVisibility(false);
                        Toast.makeText(activity.getBaseContext(), R.string.board_offline_refresh, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    public void onBoardRefreshed(final Context context, Handler handler, String boardCode) {
        super.onBoardRefreshed(context, handler, boardCode);
        if (ChanFileStorage.hasNewBoardData(context, boardCode))
            onUpdateViewData(context, handler, boardCode);
        else if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                    if (activity instanceof Activity) {
                        ((Activity)activity).setProgressBarIndeterminateVisibility(false);
                        Toast.makeText(activity.getBaseContext(), R.string.board_offline_refresh, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    public void onThreadRefreshed(Context context, Handler handler, String boardCode, long threadNo) {
        super.onThreadRefreshed(context, handler, boardCode, threadNo);
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                    if (activity instanceof Activity) {
                        ((Activity)activity).setProgressBarIndeterminateVisibility(false);
                        Toast.makeText(activity.getBaseContext(), R.string.board_offline_refresh, Toast.LENGTH_SHORT).show();
                    }
                }
            });
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
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
    }

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		super.onThreadSelected(context, board, threadId);
    }

    @Override
    public void onDataFetchFailure(ChanIdentifiedService service, Failure failure) {
        super.onDataFetchFailure(service, failure);
    }

    @Override
    public void onDataParseFailure(ChanIdentifiedService service, Failure failure) {
        super.onDataParseFailure(service, failure);
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
