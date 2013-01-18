package com.chanapps.four.service;

import java.util.HashMap;
import java.util.Map;

import com.chanapps.four.data.FetchParams;


public class WifiProfile extends MobileProfile {
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD, new FetchParams(600000L, 240000L, 15000));  // 10 min
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(300000L, 180000L, 15000));  // 5 min
		REFRESH_TIME.put(Health.SLOW, new FetchParams(180000L, 90000L, 15000));  // 3 min
		REFRESH_TIME.put(Health.GOOD, new FetchParams(120000L, 60000L, 10000));  // 2 min
		REFRESH_TIME.put(Health.PERFECT, new FetchParams(60000L, 30000L, 6000));  // 1 min
	}
	
	@Override
	public FetchParams getFetchParams() {
		return REFRESH_TIME.get(getConnectionHealth());
	}

}
