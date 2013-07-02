package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.FetchChanDataService;


public class WifiProfile extends MobileProfile {
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD,       new FetchParams(300L,  10L, 15, 10));
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(240L,  10L, 15, 10));
		REFRESH_TIME.put(Health.SLOW,      new FetchParams(180L,  10L, 15,  7));
		REFRESH_TIME.put(Health.GOOD,      new FetchParams( 60L,  10L, 10,  4));
		REFRESH_TIME.put(Health.PERFECT,   new FetchParams( 45L,  10L,  6,  3));
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

    }

}
