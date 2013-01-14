/**
 * 
 */
package com.chanapps.four.service;

import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.service.NetworkProfile.Failure;

/**
 * Class manages network profile switching.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class NetworkProfileManager {
	private static final String TAG = NetworkProfileManager.class.getSimpleName();
	
	private static NetworkProfileManager instance;
	
	public static NetworkProfileManager instance() {
		if (instance == null) {
			instance = new NetworkProfileManager();
		}
		return instance;
	}
	
	private NetworkProfileManager() {
	}
	
	private NetworkBroadcastReceiver receiver;
	private ChanActivityId currentActivityId;
	private WeakReference<ChanIdentifiedActivity> currentActivity;
	private NetworkProfile activeProfile = null;
	private UserPreferences userPrefs = null;
	
	private WifiProfile wifiProfile = new WifiProfile();
	private NoConnectionProfile noConnectionProfile = new NoConnectionProfile();
	private MobileProfile mobileProfile = new MobileProfile();
	
	public ChanActivityId getActivityId () {
		return currentActivityId;
	}
	
	public ChanIdentifiedActivity getActivity() {
		return currentActivity.get();
	}

	public void activityChange(ChanIdentifiedActivity newActivity) {
		Log.w(TAG, "activity change to " + newActivity.getChanActivityId());
		if (receiver == null) {
			receiver = new NetworkBroadcastReceiver();
			newActivity.getBaseContext().getApplicationContext()
				.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			Log.w(TAG, ConnectivityManager.CONNECTIVITY_ACTION + " receiver registered");
		}
		
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = new WeakReference<ChanIdentifiedActivity>(newActivity);

		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(newActivity.getBaseContext());
		}

		switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
			if (currentActivityId == null) {
				activeProfile.onApplicationStart(newActivity.getBaseContext());
			} else {
				activeProfile.onBoardSelectorSelected(newActivity.getBaseContext());
			}
			break;
		case BOARD_ACTIVITY:
			activeProfile.onBoardSelected(newActivity.getBaseContext(), currentActivityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			activeProfile.onThreadSelected(newActivity.getBaseContext(), currentActivityId.boardCode, currentActivityId.threadNo);
			break;
		case FULL_SCREEN_IMAGE_ACTIVITY:
			activeProfile.onFullImageLoading(newActivity.getBaseContext(), currentActivityId.boardCode, currentActivityId.threadNo, currentActivityId.postNo);
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
		
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(newActivity.getBaseContext());
		}

		switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
			if (currentActivityId == null) {
				activeProfile.onApplicationStart(newActivity.getBaseContext());
			} else {
				activeProfile.onBoardSelectorSelected(newActivity.getBaseContext());
			}
			break;
		case BOARD_ACTIVITY:
			activeProfile.onBoardRefreshed(newActivity.getBaseContext(), currentActivityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			activeProfile.onThreadRefreshed(newActivity.getBaseContext(), currentActivityId.boardCode, currentActivityId.threadNo);
			break;
		case FULL_SCREEN_IMAGE_ACTIVITY:
			activeProfile.onFullImageLoading(newActivity.getBaseContext(), currentActivityId.boardCode, currentActivityId.threadNo, currentActivityId.postNo);
			break;
		case POST_REPLY_ACTIVITY:
			break;
		case SETTINGS_ACTIVITY:
			break;
		default:
			Log.e(TAG, "Not handled activity type: " + currentActivityId.activity, new Exception("Check stack trace!"));
		}
	}
	
	public void finishedFetchingData(ChanIdentifiedService service, int time, int size) {
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchSuccess(service, time, size);
	}
	
	public void failedFetchingData(ChanIdentifiedService service, Failure failure) {
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchFailure(service, failure);
	}
	
	public void finishedParsingData(ChanIdentifiedService service) {
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataParseSuccess(service);
	}
	
	public void failedParsingData(ChanIdentifiedService service, Failure failure) {
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchFailure(service, failure);
	}
	
	public void changeNetworkProfile(NetworkProfile.Type type) {
		changeNetworkProfile(type, null);
	}
	
	public void changeNetworkProfile(NetworkProfile.Type type, String subType) {
		switch(type) {
		case NO_CONNECTION:
			if (activeProfile != noConnectionProfile) {
				if (activeProfile != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.get().getBaseContext());
				}
				activeProfile = noConnectionProfile;
				Log.w(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.get().getBaseContext());
				}
			}
			break;
		case WIFI:
			if (activeProfile != wifiProfile) {
				if (activeProfile != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.get().getBaseContext());
				}
				activeProfile = wifiProfile;
				Log.w(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.get().getBaseContext());
				}
			}
			break;
		case MOBILE:
			if (activeProfile != mobileProfile) {
				if (activeProfile != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.get().getBaseContext());
				}
				activeProfile = mobileProfile;
				Log.w(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.get().getBaseContext());
				}
			}
			break;
		}
	}
	
	public static class NetworkBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.w(TAG, "Connection change action: " + action);
            
            if (intent.getBooleanExtra("EXTRA_NO_CONNECTIVITY", false)) {
            	Log.w(TAG, "Disconnected from any network");
            	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.NO_CONNECTION);
            } else {
            	checkNetwork(context);
            }
        }
        
        public static void checkNetwork(Context context) {
            NetworkInfo activeNetwork = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
            	if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
        			Log.w(TAG, "Connected to Wifi");
        			NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.WIFI);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                	String networkType = activeNetwork.getSubtypeName();
            		Log.w(TAG, "Connected to mobile " + networkType);
            		NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.MOBILE, networkType);
                } else {
                	Log.w(TAG, "Connected to other type of network " + activeNetwork.getType());
                	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.MOBILE);
                }
            } else {
            	Log.w(TAG, "Not connected or connecting");
            	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.NO_CONNECTION);
            }
        }
	}
}
