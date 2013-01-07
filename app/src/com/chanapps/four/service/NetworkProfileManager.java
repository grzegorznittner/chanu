/**
 * 
 */
package com.chanapps.four.service;

import java.lang.ref.WeakReference;

import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;

/**
 * Class manages network profile switching.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class NetworkProfileManager {
	private static final String TAG = NetworkProfileManager.class.getSimpleName();
	
	/**
	 * Lenght of mobile network operations. Default to 20s.
	 */
	private static final long NETWORK_WINDOW_LENGHT = 20000;
	
	private static NetworkProfileManager instance;
	
	public static NetworkProfileManager instance() {
		if (instance == null) {
			instance = new NetworkProfileManager();
		}
		return instance;
	}
	
	private NetworkProfileManager() {
		
	}
	
	private ChanActivityId currentActivityId;
	private WeakReference<ChanIdentifiedActivity> currentActivity;
	private NetworkProfile activeProfile = new WifiProfile();
	private UserPreferences userPrefs = null;
	
	public ChanActivityId getActivityId () {
		return currentActivityId;
	}
	
	public ChanIdentifiedActivity getActivity() {
		return currentActivity.get();
	}

	public void activityChange(ChanIdentifiedActivity newActivity) {
		if (currentActivityId != null && newActivity != null && !currentActivityId.equals(newActivity.getChanActivityId())) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = new WeakReference<ChanIdentifiedActivity>(newActivity);
		
		switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
			if (currentActivityId == null) {
				activeProfile.onApplicationStart();
			} else {
				activeProfile.onBoardSelectorSelected();
			}
			break;
		case BOARD_ACTIVITY:
			activeProfile.onBoardSelected(currentActivityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			activeProfile.onThreadSelected(currentActivityId.boardCode, currentActivityId.threadNo);
			break;
		case FULL_SCREEN_IMAGE_ACTIVITY:
			activeProfile.onFullImageLoading(currentActivityId.boardCode, currentActivityId.threadNo, currentActivityId.postNo);
			break;
		case POST_REPLY_ACTIVITY:
			break;
		case SETTINGS_ACTIVITY:
			break;
		default:
			Log.e(TAG, "Not handled activity type: " + currentActivityId.activity, new Exception("Check stack trace!"));
		}
	}
	
	public void manualRefresh(ChanIdentifiedActivity newActivity) {
		if (newActivity == null) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = new WeakReference<ChanIdentifiedActivity>(newActivity);

		switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
			if (currentActivityId == null) {
				activeProfile.onApplicationStart();
			} else {
				activeProfile.onBoardSelectorSelected();
			}
			break;
		case BOARD_ACTIVITY:
			activeProfile.onBoardRefreshed(currentActivityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			activeProfile.onThreadRefreshed(currentActivityId.boardCode, currentActivityId.threadNo);
			break;
		case FULL_SCREEN_IMAGE_ACTIVITY:
			activeProfile.onFullImageLoading(currentActivityId.boardCode, currentActivityId.threadNo, currentActivityId.postNo);
			break;
		case POST_REPLY_ACTIVITY:
			break;
		case SETTINGS_ACTIVITY:
			break;
		default:
			Log.e(TAG, "Not handled activity type: " + currentActivityId.activity, new Exception("Check stack trace!"));
		}
	}
}
