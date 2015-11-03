package com.chanapps.four.service.profile;

import android.os.Handler;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.FetchParams;

import android.content.Context;

/**
 * Set of actions performed by application based on the network type.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public interface NetworkProfile {
	/**
	 * Connection type
	 */
	enum Type {WIFI, MOBILE, NO_CONNECTION};
	/**
	 * NETWORK - all connection and transmission errors (eg. timeouts)
	 * MISSING_DATA - requested resource is not available (eg. board page doesn't exist)
	 * WRONG_DATA - data parse error
	 */
	enum Failure {NETWORK, MISSING_DATA, WRONG_DATA, CORRUPT_DATA, THREAD_UNMODIFIED, DEAD_THREAD};
	/**
	 * NO_CONNECTION - phisical connection not established
	 * BAD - connection established but non of recent request worked
	 * VERY_SLOW - data are received but at rate 10kB/s or slower
	 * SLOW - data received at rate 50kB/s or slower
	 * GOOD - data received at rate 200kB/s or slower
	 * PERFECT - data received above rate 200kB/s
	 */
	enum Health {NO_CONNECTION, BAD, VERY_SLOW, SLOW, GOOD, PERFECT};

	/**
	 * Returns connection type.
	 */
	Type getConnectionType();
	
	/**
	 * Returns health status of the connection.
	 * NO_CONNECTION is only returned when there is no phisical connection established.
	 */
	Health getConnectionHealth();
	
	/**
	 * Returns fetch params based on the connection type and health
	 */
	FetchParams getFetchParams();
	
	/**
	 * Returns default connection health status. Used when health couldn't be calculated based on
	 * previous data transfers.
	 */
	Health getDefaultConnectionHealth();
	
	/**
	 * Called when application starts.
	 */
	void onApplicationStart(Context context);
	
	/**
	 * Called when user goes to board selector page (except when application starts)
	 */
	void onBoardSelectorSelected(Context context, String boardCode);

    /**
     * Called when user manually refreshes board selector page
     */
    void onBoardSelectorRefreshed(Context context, Handler handler, String boardCode);

    /**
	 * Called when user opened board page.
	 * @param board Board name
	 */
	void onBoardSelected(Context context, String board);
	
	/**
	 * Called when user clicked refresh button on board page.
	 * @param board Board name
	 */
	void onBoardRefreshed(Context context, Handler handler, String board);
	
	/**
	 * Called when user clicked on update view data button (displayed when new items available)
	 * @param board Board name
	 */
	void onUpdateViewData(Context baseContext, Handler chanHandler,	String board);
	
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
	void onThreadRefreshed(Context context, Handler handler, String board, long threadId);
	
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

	/**
	 * Called when image download is completed. It's intended to decide what will be done next.
	 */
	void onImageDownloadSuccess(Context context, int time, int size);	
}
