package com.chanapps.four.service;

public class NoConnectionProfile extends AbstractNetworkProfile {

	@Override
	public void onApplicationStart() {
		super.onApplicationStart();
	}

	@Override
	public void onBoardSelectorSelected() {
		super.onBoardSelectorSelected();
	}

	@Override
	public void onBoardSelected(String board) {
		super.onBoardSelected(board);
	}

	@Override
	public void onBoardRefreshed(String board) {
		super.onBoardRefreshed(board);
	}

	@Override
	public void onThreadSelected(String board, long threadId) {
		super.onThreadSelected(board, threadId);
	}

	@Override
	public void onThreadRefreshed(String board, long threadId) {
		super.onThreadRefreshed(board, threadId);
	}

	@Override
	public void onFullImageLoading(String board, long threadId, long postId) {
		super.onFullImageLoading(board, threadId, postId);
	}

	@Override
	public void onProfileDeactivated() {
		super.onProfileDeactivated();
	}	

}
