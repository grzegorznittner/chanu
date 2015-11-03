package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import com.chanapps.four.data.FetchParams;


public class WifiProfile extends MobileProfile {
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD,       new FetchParams(600L,  3L, 15, 10, 0, 0));
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(600L,  3L, 15, 10, 100000, 5));
		REFRESH_TIME.put(Health.SLOW,      new FetchParams(600L,  3L, 15,  7, 250000, 10));
		REFRESH_TIME.put(Health.GOOD,      new FetchParams(600L,  3L, 10,  4, 500000, 15));
		REFRESH_TIME.put(Health.PERFECT,   new FetchParams(600L,  3L,  6,  3, 5000000, 20));
	}
	
	@Override
	public Type getConnectionType() {
		return Type.WIFI;
	}

	@Override
	public FetchParams getFetchParams() {
		return REFRESH_TIME.get(getConnectionHealth());
	}

    @Override
    public void onBoardSelected(Context context, String boardCode) {
        super.onBoardSelected(context, boardCode);
        // seems to overload phone on wifi
        /*
        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
        Health health = getConnectionHealth();
        if (health == Health.GOOD || health == Health.PERFECT) {
            ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
            int threadPrefechCounter = health == Health.GOOD ? 3 : 7;
            if (board != null) {
                for(ChanPost thread : board.threads) {
                    if (threadPrefechCounter <= 0) {
                        break;
                    }
                    if (thread.closed == 0 && thread.sticky == 0 && thread.replies > 5 && thread.images > 1) {
                        threadPrefechCounter--;
                        FetchChanDataService.scheduleThreadFetch(context, boardCode, thread.no, false, false);
                    }
                }
            }
        }
        */

    }

}
