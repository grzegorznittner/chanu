package com.chanapps.four.service;

import android.content.Context;

public class NoConnectionProfile extends AbstractNetworkProfile {

	@Override
	public Health getConnectionHealth() {
		return Health.NO_CONNECTION;
	}
	
	@Override
	public void onProfileActivated(Context context) {
		super.onProfileActivated(context);
		
		FetchChanDataService.clearServiceQueue(context);
		
		makeToast("Lost connectivity");
	}

	@Override
	public void onApplicationStart(Context context) {
		super.onApplicationStart(context);
	}

	@Override
	public void onBoardSelected(Context context, String board) {
		super.onBoardSelected(context, board);
		
		makeToast("No data connection");
	}

	@Override
	public void onThreadSelected(Context context, String board, long threadId) {
		super.onThreadSelected(context, board, threadId);

		makeToast("No data connection");
	}
}
