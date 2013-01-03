/**
 * 
 */
package com.chanapps.four.service;

import java.lang.ref.WeakReference;

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
	
	private ChanActivityId currentActivityId;
	private WeakReference<ChanIdentifiedActivity> currentActivity;
	private long networkWindowStart = 0;
	private long networkWindowEnd = 0;

	public void activityChange(ChanIdentifiedActivity newActivity) {
		if (currentActivityId != null && newActivity != null && !currentActivityId.equals(newActivity.getChanActivityId())) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = new WeakReference<ChanIdentifiedActivity>(newActivity);
	}
	
	public void manualRefresh(ChanIdentifiedActivity newActivity) {
		if (newActivity == null) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = new WeakReference<ChanIdentifiedActivity>(newActivity);
	}
}
