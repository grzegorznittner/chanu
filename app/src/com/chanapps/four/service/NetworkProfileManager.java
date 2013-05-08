/**
 * 
 */
package com.chanapps.four.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.profile.MobileProfile;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;
import com.chanapps.four.service.profile.NoConnectionProfile;
import com.chanapps.four.service.profile.WifiProfile;

/**
 * Class manages network profile switching.
 * 
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class NetworkProfileManager {
	private static final String TAG = NetworkProfileManager.class.getSimpleName();
	private static final boolean DEBUG = false;
	
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
	private ChanIdentifiedActivity currentActivity;
	private NetworkProfile activeProfile = null;
	private UserStatistics userStats = null;
	
	private WifiProfile wifiProfile = new WifiProfile();
	private NoConnectionProfile noConnectionProfile = new NoConnectionProfile();
	private MobileProfile mobileProfile = new MobileProfile();
	
	public ChanActivityId getActivityId () {
		return currentActivityId;
	}
	
	/**
	 * Returns current network profile.
	 * CALL ONLY GET METHODS.
	 */
	public NetworkProfile getCurrentProfile() {
		return activeProfile == null ? noConnectionProfile : activeProfile;
	}
	
	public ChanIdentifiedActivity getActivity() {
		return currentActivity;
	}
	
	public FetchParams getFetchParams() {
		if (activeProfile != null) {
			return activeProfile.getFetchParams();
		} else {
			return noConnectionProfile.getFetchParams();
		}
	}
	
	public UserStatistics getUserStatistics() {
		return userStats;
	}

    public void activityChange(ChanIdentifiedActivity newActivity) {
		if (DEBUG) Log.i(TAG, "activity change to " + newActivity.getChanActivityId() + " receiver=" + receiver + " activity=" + currentActivity);
		if (receiver == null) {
			// we need to register network changes receiver
			receiver = new NetworkBroadcastReceiver();
			newActivity.getBaseContext().getApplicationContext()
				.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			if (DEBUG) Log.i(TAG, ConnectivityManager.CONNECTIVITY_ACTION + " receiver registered");
		}
		
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = newActivity;

		if (userStats == null) {
			userStats = ChanFileStorage.loadUserStats(newActivity.getBaseContext());
		}
		userStats.registerActivity(newActivity);

        // always do this because otherwise receiver is not always called
		//if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(newActivity.getBaseContext());
		//}


        switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
            if (currentActivityId == null) {
				activeProfile.onApplicationStart(newActivity.getBaseContext());
			} else {
				activeProfile.onBoardSelectorSelected(newActivity.getBaseContext(), currentActivityId.boardCode);
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
        NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.MANUAL_REFRESH);
        if (DEBUG) Log.i(TAG, "manualRefresh " + newActivity.getChanActivityId(), new Exception("manualRefresh"));
		if (newActivity == null) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = newActivity;
		
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(newActivity.getBaseContext());
		}
        if (DEBUG) Log.i(TAG, "EXCEPTION activeProfile=" + activeProfile);
		switch(currentActivityId.activity) {
		case BOARD_SELECTOR_ACTIVITY:
            activeProfile.onBoardSelectorRefreshed(newActivity.getBaseContext(), newActivity.getChanHandler(), currentActivityId.boardCode);
			break;
		case BOARD_ACTIVITY:
			activeProfile.onBoardRefreshed(newActivity.getBaseContext(), newActivity.getChanHandler(), currentActivityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			activeProfile.onThreadRefreshed(newActivity.getBaseContext(), newActivity.getChanHandler(), currentActivityId.boardCode, currentActivityId.threadNo);
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
	
	/**
	 * Replaces currently viewed data with the one fetched recently
	 */
	public void updateViewData(ChanIdentifiedActivity newActivity) {
		if (DEBUG) Log.i(TAG, "updateViewData " + newActivity.getChanActivityId(), new Exception("updateViewData"));
		if (newActivity == null) {
			return;
		}
		currentActivityId = newActivity.getChanActivityId();
		currentActivity = newActivity;
		
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(newActivity.getBaseContext());
		}

		switch(currentActivityId.activity) {
		case BOARD_ACTIVITY:
			activeProfile.onUpdateViewData(newActivity.getBaseContext(), newActivity.getChanHandler(), currentActivityId.boardCode);
			break;
		default:
			// we only support update view data for board view
		}
	}
	public void finishedImageDownload(ChanIdentifiedService service, int time, int size) {
		service = checkService(service);
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onImageDownloadSuccess(service.getApplicationContext(), time, size);
	}

	public void finishedFetchingData(ChanIdentifiedService service, int time, int size) {
		service = checkService(service);
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchSuccess(service, time, size);
	}
	
	public void failedFetchingData(ChanIdentifiedService service, Failure failure) {
		service = checkService(service);
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchFailure(service, failure);
	}
	
	public void finishedParsingData(ChanIdentifiedService service) {
		service = checkService(service);
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataParseSuccess(service);
	}
	
	public void failedParsingData(ChanIdentifiedService service, Failure failure) {
		service = checkService(service);
		if (activeProfile == null) {
			NetworkBroadcastReceiver.checkNetwork(service.getApplicationContext());
		}
		activeProfile.onDataFetchFailure(service, failure);
	}
	
	private ChanIdentifiedService checkService(ChanIdentifiedService service) {
		if (service == null) {
			return new ChanIdentifiedService() {
				@Override
				public ChanActivityId getChanActivityId() {
					return getActivity().getChanActivityId();
				}
				
				@Override
				public Context getApplicationContext() {
					return getActivity().getBaseContext();
				}
			};
		} else {
			return service;
		}
	}
	
	public void changeNetworkProfile(NetworkProfile.Type type) {
		changeNetworkProfile(type, null);
	}
	
	public void changeNetworkProfile(NetworkProfile.Type type, String subType) {
		switch(type) {
		case NO_CONNECTION:
			if (activeProfile != noConnectionProfile) {
				if (activeProfile != null && currentActivity != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.getBaseContext());
				}
				activeProfile = noConnectionProfile;
				if (DEBUG) Log.i(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.getBaseContext());
				}
			}
			break;
		case WIFI:
			if (activeProfile != wifiProfile) {
				if (activeProfile != null && currentActivity != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.getBaseContext());
				}
				activeProfile = wifiProfile;
				if (DEBUG) Log.i(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.getBaseContext());
				}
			}
			break;
		case MOBILE:
			if (activeProfile != mobileProfile) {
				if (activeProfile != null && currentActivity != null && currentActivity != null) {
					activeProfile.onProfileDeactivated(currentActivity.getBaseContext());
				}
				activeProfile = mobileProfile;
				if (DEBUG) Log.i(TAG, "Setting " + type + " profile");
				if (currentActivity != null) {
					activeProfile.onProfileActivated(currentActivity.getBaseContext());
				}
			}
			break;
		}
	}
	
	public static class NetworkBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) Log.i(TAG, "Connection change action: " + action);
// can't do this because extra is unreliable, returns true even when mobile data is up
//            if (intent.getBooleanExtra("EXTRA_NO_CONNECTIVITY", false)) {
//            	if (DEBUG) Log.i(TAG, "Disconnected from any network");
//            	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.NO_CONNECTION);
//            } else {
            	checkNetwork(context);
//            }
        }
        
        public static void checkNetwork(Context context) {
            NetworkInfo activeNetwork = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
            	if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
        			if (DEBUG) Log.i(TAG, "Connected to Wifi");
        			NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.WIFI);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                	String networkType = activeNetwork.getSubtypeName();
            		if (DEBUG) Log.i(TAG, "Connected to mobile " + networkType);
            		NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.MOBILE, networkType);
                } else {
                	if (DEBUG) Log.i(TAG, "Connected to other type of network " + activeNetwork.getType());
                	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.MOBILE);
                }
            } else {
            	if (DEBUG) Log.i(TAG, "Not connected or connecting");
            	NetworkProfileManager.instance().changeNetworkProfile(NetworkProfile.Type.NO_CONNECTION);
            }
        }
	}
	
	public void makeToast(final String text) {
		makeToast(text, Toast.LENGTH_SHORT);
	}

    public void makeToast(final String text, final int length) {
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity != null) {
            Handler handler = activity.getChanHandler();
            if (handler != null) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (DEBUG) Log.w(TAG, "Calling toast with '" + text + "'");
                        Toast.makeText(activity.getBaseContext(), text, length).show();
                    }
                }, 300);
            } else {
                if (DEBUG) Log.w(TAG, "Null handler for " + activity);
            }
        }
    }

    public void makeToast(final int id) {
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity != null) {
            Handler handler = activity.getChanHandler();
            if (handler != null) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (DEBUG) Log.w(TAG, "Calling toast with '" + id + "'");
                        Toast.makeText(activity.getBaseContext(), id, Toast.LENGTH_SHORT).show();
                    }
                }, 300);
            } else {
                if (DEBUG) Log.w(TAG, "Null handler for " + activity);
            }
        }
    }

    public static boolean isConnected() {
        NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
        return profile.getConnectionType() != NetworkProfile.Type.NO_CONNECTION;
    }

}
