package com.chanapps.four.service;

/**
 * Set of actions performed by application based on the network type.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public interface NetworkProfile {
	/**
	 * Called when application starts.
	 */
	void onApplicationStart();
	
	/**
	 * Called when user goes to board selector page (except when application starts)
	 */
	void onBoardSelectorSelected();

	/**
	 * Called when user opened board page.
	 * @param board Board name
	 */
	void onBoardSelected(String board);
	
	/**
	 * Called when user clicked refresh button on board page.
	 * @param board Board name
	 */
	void onBoardRefreshed(String board);
	
	/**
	 * Called when user opened thread page.
	 * @param board Board name
	 * @param threadId Thread id
	 */
	void onThreadSelected(String board, long threadId);
	
	/**
	 * Called when user clicked refresh button on thread page.
	 * @param board Board name
	 * @param threadId Thread id
	 */
	void onThreadRefreshed(String board, long threadId);
	
	/**
	 * Called when user wants to open full image view.
	 * @param board Board name
	 * @param threadId Thread id
	 * @param postId Post id
	 */
	void onFullImageLoading(String board, long threadId, long postId);

	/**
	 * Called when specified profile gets deactivated.
	 * Some internal data could be cleaned, eg. first usage flag
	 */
	void onProfileDeactivated();
}
