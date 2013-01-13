package com.chanapps.four.service;

import com.chanapps.four.activity.ChanIdentifiedService;

import android.content.Context;

/**
 * Set of actions performed by application based on the network type.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public interface NetworkProfile {
	enum Type {WIFI, MOBILE, NO_CONNECTION};
	enum Failure {NETWORK, MISSING_DATA, WRONG_DATA};
	
	/**
	 * Called when application starts.
	 */
	void onApplicationStart(Context context);
	
	/**
	 * Called when user goes to board selector page (except when application starts)
	 */
	void onBoardSelectorSelected(Context context);

	/**
	 * Called when user opened board page.
	 * @param board Board name
	 */
	void onBoardSelected(Context context, String board);
	
	/**
	 * Called when user clicked refresh button on board page.
	 * @param board Board name
	 */
	void onBoardRefreshed(Context context, String board);
	
	/**
	 * Called when user opened thread page.
	 * @param board Board name
	 * @param threadId Thread id
	 */
	void onThreadSelected(Context context, String board, long threadId);
	
	/**
	 * Called when user clicked refresh button on thread page.
	 * @param board Board name
	 * @param threadId Thread id
	 */
	void onThreadRefreshed(Context context, String board, long threadId);
	
	/**
	 * Called when user wants to open full image view.
	 * @param board Board name
	 * @param threadId Thread id
	 * @param postId Post id
	 */
	void onFullImageLoading(Context context, String board, long threadId, long postId);

	/**
	 * Called when specified profile gets deactivated.
	 */
	void onProfileActivated(Context context);
	
	/**
	 * Called when specified profile gets deactivated.
	 * Some internal data could be cleaned, eg. first usage flag
	 */
	void onProfileDeactivated(Context context);
	
	/**
	 * Called when data fetch is finished. It's intended to decide what will be done next.
	 */
	void onDataFetchSuccess(ChanIdentifiedService service, int time, int size);
	
	/**
	 * Called when data fetch has failed. It's intended to decide what will be done next.
	 */
	void onDataFetchFailure(ChanIdentifiedService service, Failure failure);

	/**
	 * Called when data parsing is finished. It's intended to decide what will be done next.
	 */
	void onDataParseSuccess(ChanIdentifiedService service);

	/**
	 * Called when data parsing has failed. It's intended to decide what will be done next.
	 */
	void onDataParseFailure(ChanIdentifiedService service, Failure failure);
}
