package com.chanapps.four.service.profile;

import java.util.HashMap;
import java.util.Map;

import com.chanapps.four.data.FetchParams;


public class WifiProfile extends MobileProfile {
	private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams> ();
	
	static {
		/* Mapping between connection health and fetch params
		 *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
		 */
		REFRESH_TIME.put(Health.BAD,       new FetchParams(300L,  10L, 15, 10));
		REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(600L,  10L, 15, 10));
		REFRESH_TIME.put(Health.SLOW,      new FetchParams(480L,  10L, 15,  7));
		REFRESH_TIME.put(Health.GOOD,      new FetchParams(300L,  10L, 10,  4));
		REFRESH_TIME.put(Health.PERFECT,   new FetchParams(180L,  10L,  6,  3));
	}
	
	@Override
	public Type getConnectionType() {
		return Type.WIFI;
	}

	@Override
	public FetchParams getFetchParams() {
		return REFRESH_TIME.get(getConnectionHealth());
	}

}
