/**
 * 
 */
package com.chanapps.four.service;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public abstract class AbstractNetworkProfile implements NetworkProfile {
	protected int usageCounter = 0;

	@Override
	public void onApplicationStart() {
		usageCounter++;
	}

	@Override
	public void onBoardSelectorSelected() {
		usageCounter++;
	}

	@Override
	public void onBoardSelected(String board) {
		usageCounter++;
	}

	@Override
	public void onBoardRefreshed(String board) {
		usageCounter++;
	}

	@Override
	public void onThreadSelected(String board, long threadId) {
		usageCounter++;
	}

	@Override
	public void onThreadRefreshed(String board, long threadId) {
		usageCounter++;
	}

	@Override
	public void onFullImageLoading(String board, long threadId, long postId) {
		usageCounter++;
	}

	@Override
	public void onProfileDeactivated() {
		usageCounter = 0;
	}

}
